package com.eeseka.nimbat.wave.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eeseka.nimbat.core.domain.audio.AudioError
import com.eeseka.nimbat.core.domain.audio.AudioErrorException
import com.eeseka.nimbat.core.domain.audio.GestureSensor
import com.eeseka.nimbat.core.domain.wave.WaveStorage
import com.eeseka.nimbat.core.presentation.audio.mappers.toUiText
import com.eeseka.nimbat.core.presentation.permissions.PermissionState
import com.eeseka.nimbat.sound.Gesture
import com.eeseka.nimbat.sound.GestureDirection
import com.eeseka.nimbat.sound.GestureReading
import com.eeseka.nimbat.wave.presentation.model.BatDash
import com.eeseka.nimbat.wave.presentation.model.BatDashPhase
import com.eeseka.nimbat.wave.presentation.model.CaveLane
import com.eeseka.nimbat.wave.presentation.model.GalleryAnimal
import com.eeseka.nimbat.wave.presentation.model.WaveDemo
import com.eeseka.nimbat.wave.presentation.model.WaveSensorStatus
import com.eeseka.nimbat.wave.presentation.model.WaveSlide
import com.eeseka.nimbat.wave.presentation.model.advance
import com.eeseka.nimbat.wave.presentation.model.fly
import com.eeseka.nimbat.wave.presentation.model.land
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class WaveViewModel(
    private val gestureSensor: GestureSensor,
    private val waveStorage: WaveStorage
) : ViewModel() {

    private companion object {
        /** An echo this far above the still room fills its meter. */
        const val FULL_LEVEL_DB = 20.0

        const val LONGEST_FRAME_MILLIS = 50L
        const val NANOS_PER_MILLI = 1_000_000L
    }

    private val eventChannel = Channel<WaveEvent>()
    val events = eventChannel.receiveAsFlow()

    private val isScreenStarted = MutableStateFlow(false)
    private val sensingAttempt = MutableStateFlow(0)

    private var hasLoadedInitialData = false
    private var hasAskedForMicrophone = false
    private var lastFrameNanos: Long? = null

    private val _state = MutableStateFlow(WaveState())

    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                observeGestures()
                observeBestScore()
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = WaveState()
        )

    fun onAction(action: WaveAction) {
        when (action) {
            WaveAction.OnScreenStarted -> isScreenStarted.value = true
            WaveAction.OnScreenStopped -> {
                isScreenStarted.value = false
                landBat()
            }

            is WaveAction.OnMicrophonePermissionChecked -> onMicrophonePermissionChecked(action.state)
            WaveAction.OnAllowMicrophoneClick -> allowMicrophone()
            is WaveAction.OnMicrophonePermissionResolved -> _state.update {
                it.copy(microphonePermission = action.state, isRequestingMicrophone = false)
            }

            WaveAction.OnTryAgainClick -> sensingAttempt.update { it + 1 }

            is WaveAction.OnDemoSelected -> {
                landBat()
                _state.update { it.copy(demo = action.demo) }
            }

            is WaveAction.OnSlideSettled -> _state.update { it.copy(slideIndex = action.index) }
            is WaveAction.OnAnimalSettled -> _state.update { it.copy(animalIndex = action.index) }
            is WaveAction.OnCaveTap -> flyBat(action.lane)
            is WaveAction.OnBatDashFrame -> onBatDashFrame(action.frameTimeNanos)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeGestures() {
        combine(
            isScreenStarted,
            _state.map { it.microphonePermission == PermissionState.GRANTED }.distinctUntilChanged(),
            sensingAttempt
        ) { isStarted, isMicrophoneGranted, _ ->
            isStarted && isMicrophoneGranted
        }
            .flatMapLatest { isListening -> if (isListening) gestureReadings() else emptyFlow() }
            .launchIn(viewModelScope)
    }

    private fun observeBestScore() {
        waveStorage.bestBatDashScore
            .onEach { best -> _state.update { it.copy(batDash = it.batDash.copy(bestScore = best)) } }
            .launchIn(viewModelScope)
    }

    private fun gestureReadings(): Flow<GestureReading> = gestureSensor.readings()
        .onStart {
            _state.update {
                it.copy(
                    sensorStatus = WaveSensorStatus.CALIBRATING,
                    calibrationProgress = 0f,
                    isToneClear = true,
                    towardLevel = 0f,
                    awayLevel = 0f
                )
            }
        }
        .onEach { reading -> onReading(reading) }
        .catch { error ->
            if (error !is AudioErrorException) throw error
            stopSensing(error.error)
        }

    private suspend fun onReading(reading: GestureReading) {
        when (reading) {
            is GestureReading.Calibrating -> _state.update {
                it.copy(
                    sensorStatus = WaveSensorStatus.CALIBRATING,
                    calibrationProgress = reading.progress,
                    isToneClear = reading.isToneClear
                )
            }

            is GestureReading.Sensing -> {
                _state.update {
                    it.copy(
                        sensorStatus = WaveSensorStatus.SENSING,
                        isToneClear = reading.isToneClear,
                        towardLevel = levelFor(reading.towardRiseDb),
                        awayLevel = levelFor(reading.awayRiseDb)
                    )
                }
                reading.gesture?.let { onGesture(it) }
            }
        }
    }

    private suspend fun onGesture(gesture: Gesture) {
        val step = if (gesture.direction == GestureDirection.TOWARD) 1 else -1
        _state.update {
            it.copy(
                lastGesture = gesture.direction,
                gestureCount = it.gestureCount + 1
            )
        }

        when (_state.value.demo) {
            WaveDemo.SLIDES -> _state.update {
                it.copy(slideIndex = (it.slideIndex + step).coerceIn(0, WaveSlide.entries.lastIndex))
            }
            WaveDemo.GALLERY -> _state.update {
                it.copy(animalIndex = (it.animalIndex + step).coerceIn(0, GalleryAnimal.entries.lastIndex))
            }
            WaveDemo.BAT_DASH -> flyBat(if (gesture.direction == GestureDirection.AWAY) CaveLane.HIGH else CaveLane.LOW)
        }
        eventChannel.send(WaveEvent.GestureDetected)
    }

    private suspend fun stopSensing(error: AudioError) {
        _state.update {
            it.copy(
                sensorStatus = WaveSensorStatus.FAILED,
                towardLevel = 0f,
                awayLevel = 0f
            )
        }
        eventChannel.send(WaveEvent.Error(error.toUiText()))
    }

    private fun flyBat(lane: CaveLane) {
        _state.update { it.copy(batDash = it.batDash.fly(lane, seed = Random.nextInt())) }
    }

    private fun onBatDashFrame(frameTimeNanos: Long) {
        val elapsedMillis = lastFrameNanos?.let { (frameTimeNanos - it) / NANOS_PER_MILLI } ?: 0L
        lastFrameNanos = frameTimeNanos

        val before = _state.value.batDash
        val after = before.advance(elapsedMillis.coerceIn(0L, LONGEST_FRAME_MILLIS))
        _state.update { it.copy(batDash = after) }

        if (before.phase == BatDashPhase.FLYING && after.phase == BatDashPhase.CRASHED) {
            viewModelScope.launch { eventChannel.send(WaveEvent.BatCrashed) }
            saveIfBest(before, after)
        }
    }

    private fun landBat() {
        lastFrameNanos = null
        val before = _state.value.batDash
        val after = before.land()
        _state.update { it.copy(batDash = after) }
        saveIfBest(before, after)
    }

    private fun saveIfBest(before: BatDash, after: BatDash) {
        if (after.bestScore <= before.bestScore) return
        viewModelScope.launch {
            waveStorage.setBestBatDashScore(after.bestScore)
        }
    }

    private fun onMicrophonePermissionChecked(permission: PermissionState) {
        // Android's quiet check reports a blocked mic as NOT_DETERMINED, so blocked stays blocked until GRANTED
        val wasBlocked = _state.value.microphonePermission == PermissionState.PERMANENTLY_DENIED
        if (wasBlocked && permission != PermissionState.GRANTED) return

        _state.update { it.copy(microphonePermission = permission) }
        if (permission == PermissionState.NOT_DETERMINED && !hasAskedForMicrophone) {
            hasAskedForMicrophone = true
            _state.update { it.copy(isRequestingMicrophone = true) }
        }
    }

    private fun allowMicrophone() {
        if (_state.value.microphonePermission == PermissionState.PERMANENTLY_DENIED) {
            viewModelScope.launch {
                eventChannel.send(WaveEvent.OpenMicrophoneSettings)
            }
        } else {
            _state.update { it.copy(isRequestingMicrophone = true) }
        }
    }

    private fun levelFor(riseDb: Double): Float = (riseDb / FULL_LEVEL_DB).toFloat().coerceIn(0f, 1f)
}
