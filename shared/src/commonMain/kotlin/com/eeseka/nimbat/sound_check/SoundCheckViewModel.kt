package com.eeseka.nimbat.sound_check

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eeseka.nimbat.core.domain.audio.AudioEngine
import com.eeseka.nimbat.core.domain.audio.AudioError
import com.eeseka.nimbat.core.domain.audio.AudioErrorException
import com.eeseka.nimbat.core.domain.util.Result
import com.eeseka.nimbat.core.domain.util.onFailure
import com.eeseka.nimbat.core.presentation.permissions.PermissionState
import com.eeseka.nimbat.core.presentation.util.UiText
import com.eeseka.nimbat.core.presentation.audio.mappers.toUiText
import com.eeseka.nimbat.sound.DOPPLER_BLOCK_SIZE
import com.eeseka.nimbat.sound.Finder
import com.eeseka.nimbat.sound.Motion
import com.eeseka.nimbat.sound.Reception
import com.eeseka.nimbat.sound.SAMPLE_RATE
import com.eeseka.nimbat.sound.UP_CHIRP
import com.eeseka.nimbat.sound.chirp
import com.eeseka.nimbat.sound.dopplerTone
import com.eeseka.nimbat.sound.median
import com.eeseka.nimbat.sound.motion
import com.eeseka.nimbat.sound.receive
import com.eeseka.nimbat.sound.transmit
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

class SoundCheckViewModel(
    private val audio: AudioEngine
) : ViewModel() {

    private val _state = MutableStateFlow(SoundCheckState())
    val state = _state.asStateFlow()

    private var motionJob: Job? = null

    fun onAction(action: SoundCheckAction) {
        when (action) {
            SoundCheckAction.OnChirpCheckClick -> askFor(SoundCheck.CHIRP)
            SoundCheckAction.OnMessageCheckClick -> askFor(SoundCheck.MESSAGE)
            SoundCheckAction.OnMotionCheckClick -> {
                if (motionJob?.isActive == true) motionJob?.cancel() else askFor(SoundCheck.MOTION)
            }
            is SoundCheckAction.OnMicrophonePermissionResolved -> onPermissionResolved(action.state)
        }
    }

    /** The screen asks for the microphone first; the check starts once the answer comes back. */
    private fun askFor(check: SoundCheck) {
        if (_state.value.runningCheck != null) return
        _state.update { it.copy(pendingCheck = check) }
    }

    private fun onPermissionResolved(permission: PermissionState) {
        val check = _state.value.pendingCheck ?: return
        _state.update { it.copy(pendingCheck = null) }
        if (permission != PermissionState.GRANTED) {
            _state.update { it.copy(status = UiText.DynamicString("The microphone permission was not granted.")) }
            return
        }
        when (check) {
            SoundCheck.CHIRP -> runCheck(check) { chirpCheck() }
            SoundCheck.MESSAGE -> runCheck(check) { messageCheck() }
            SoundCheck.MOTION -> startMotionCheck()
        }
    }

    private fun runCheck(check: SoundCheck, block: suspend () -> UiText) {
        viewModelScope.launch {
            _state.update { it.copy(runningCheck = check, status = UiText.DynamicString("Listening…")) }
            val status = try {
                block()
            } catch (e: AudioErrorException) {
                e.error.toUiText()
            }
            _state.update { it.copy(runningCheck = null, status = status) }
        }
    }

    private suspend fun chirpCheck(): UiText {
        val recording = when (val recorded = recordWhilePlaying(chirp(18_500.0, 20_000.0, 0.05))) {
            is Result.Failure -> return recorded.error.toUiText()
            is Result.Success -> recorded.data
        }
        val hit = Finder(recording, UP_CHIRP).find()
            ?: return UiText.DynamicString("Heard nothing in ${recording.seconds()} s of recording.")
        return UiText.DynamicString(
            "Heard the chirp at sample ${hit.sample}, ${hit.clarityDb.oneDecimal()} dB clear " +
                "(${recording.seconds()} s recorded)."
        )
    }

    private suspend fun messageCheck(): UiText {
        val sent = "Nimbat"
        val recording = when (val recorded = recordWhilePlaying(transmit(sent.encodeToByteArray()))) {
            is Result.Failure -> return recorded.error.toUiText()
            is Result.Success -> recorded.data
        }
        val result = when (val reception = receive(recording)) {
            null -> "No message heard in ${recording.seconds()} s of recording."
            is Reception.Garbled -> "Heard a message, but it was garbled."
            is Reception.Message -> "Received \"${reception.bytes.decodeToString()}\" (sent \"$sent\")."
        }
        return UiText.DynamicString(result)
    }

    private fun startMotionCheck() {
        _state.update {
            it.copy(
                runningCheck = SoundCheck.MOTION,
                status = UiText.DynamicString("Wave your hand toward and away from the device.")
            )
        }
        motionJob = viewModelScope.launch {
            val recent = ArrayDeque<Motion>()
            try {
                coroutineScope {
                    // loop() only returns if the speaker fails; turning that into an exception stops the microphone too
                    launch { audio.loop(dopplerTone()).onFailure { throw AudioErrorException(it) } }
                    audio.microphone().inBlocksOf(DOPPLER_BLOCK_SIZE).collect { block ->
                        recent.addLast(motion(block))
                        if (recent.size > BACKGROUND_READINGS) recent.removeFirst()
                        updateMotion(recent)
                    }
                }
            } catch (e: AudioErrorException) {
                _state.update { it.copy(status = e.error.toUiText()) }
            } finally {
                motionJob = null
                _state.update { it.copy(runningCheck = null, motionWord = null, motionReadout = null) }
            }
        }
    }

    /**
     * Holds the loudest echo of the last second, so a quick wave stays on screen, and compares it with the
     * background: the middle reading of the last 3 seconds. A jump of [MOVEMENT_DB] or more counts as movement.
     */
    private fun updateMotion(recent: ArrayDeque<Motion>) {
        val held = recent.takeLast(HELD_READINGS)
        val towardPeak = held.maxOf { it.towardDb }
        val awayPeak = held.maxOf { it.awayDb }
        val towardBackground = median(recent.map { it.towardDb })
        val awayBackground = median(recent.map { it.awayDb })
        val towardJump = towardPeak - towardBackground
        val awayJump = awayPeak - awayBackground

        val word = when {
            towardJump >= MOVEMENT_DB && towardJump >= awayJump -> "Toward"
            awayJump >= MOVEMENT_DB -> "Away"
            else -> "Still"
        }
        _state.update {
            it.copy(
                motionWord = word,
                motionReadout = "Toward ${towardPeak.wholeDb()} dB (still ${towardBackground.wholeDb()}) · " +
                    "Away ${awayPeak.wholeDb()} dB (still ${awayBackground.wholeDb()})"
            )
        }
    }

    /**
     * Records from half a second before [sound] plays until 0.3 s after, and returns the whole recording.
     * A microphone failure ends the recording with an [AudioErrorException].
     */
    private suspend fun recordWhilePlaying(sound: FloatArray): Result<FloatArray, AudioError> = coroutineScope {
        val blocks = mutableListOf<FloatArray>()
        val recording = launch { audio.microphone().collect { blocks += it } }
        delay(500.milliseconds) // give the microphone time to start
        val played = audio.play(sound)
        delay(300.milliseconds) // keep listening for the echoes
        recording.cancelAndJoin()
        if (played is Result.Failure) return@coroutineScope played

        val samples = FloatArray(blocks.sumOf { it.size })
        var at = 0
        blocks.forEach { block ->
            block.copyInto(samples, destinationOffset = at)
            at += block.size
        }
        Result.Success(samples)
    }
}

/** Regroups blocks of any size into blocks of exactly [size] samples. */
private fun Flow<FloatArray>.inBlocksOf(size: Int): Flow<FloatArray> = flow {
    val pending = FloatArray(size)
    var filled = 0
    collect { samples ->
        for (sample in samples) {
            pending[filled++] = sample
            if (filled == size) {
                emit(pending.copyOf())
                filled = 0
            }
        }
    }
}

/** About 12 Doppler readings a second (48,000 ÷ 4,096). */
private const val HELD_READINGS = 12
private const val BACKGROUND_READINGS = 36

/** How far above the background an echo must jump to count as movement. A first guess, to tune on real devices. */
private const val MOVEMENT_DB = 10.0

private fun Double.wholeDb(): String = when {
    isInfinite() -> if (this > 0) "∞" else "−∞"
    isNaN() -> "?"
    else -> roundToInt().toString()
}

private fun FloatArray.seconds(): String = (size.toDouble() / SAMPLE_RATE).oneDecimal()

private fun Double.oneDecimal(): String = when {
    isInfinite() -> if (this > 0) "∞" else "−∞"
    isNaN() -> "?"
    else -> ((this * 10).roundToInt() / 10.0).toString()
}
