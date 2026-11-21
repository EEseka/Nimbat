package com.eeseka.nimbat.sound_check

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eeseka.nimbat.core.domain.audio.AudioEngine
import com.eeseka.nimbat.core.domain.audio.AudioErrorException
import com.eeseka.nimbat.core.domain.audio.util.inBlocksOf
import com.eeseka.nimbat.core.domain.audio.util.recordWhileLooping
import com.eeseka.nimbat.core.domain.audio.util.recordWhilePlaying
import com.eeseka.nimbat.core.domain.util.Result
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class SoundCheckViewModel(
    private val audio: AudioEngine
) : ViewModel() {

    private val _state = MutableStateFlow(SoundCheckState())
    val state = _state.asStateFlow()

    fun onAction(action: SoundCheckAction) {
        when (action) {
            SoundCheckAction.OnChirpCheckClick -> askFor(SoundCheck.CHIRP)
            SoundCheckAction.OnMessageCheckClick -> askFor(SoundCheck.MESSAGE)
            SoundCheckAction.OnMotionCheckClick -> {
                if (_state.value.runningCheck == SoundCheck.MOTION) {
                    _state.update { it.copy(runningCheck = null) }
                } else {
                    askFor(SoundCheck.MOTION)
                }
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
        val recording = when (val recorded = audio.recordWhilePlaying(chirp(18_500.0, 20_000.0, 0.05))) {
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
        val recording = when (val recorded = audio.recordWhilePlaying(transmit(sent.encodeToByteArray()))) {
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
        viewModelScope.launch {
            val recent = ArrayDeque<Motion>()
            try {
                audio.recordWhileLooping(dopplerTone())
                    .takeWhile { _state.value.runningCheck == SoundCheck.MOTION }
                    .inBlocksOf(DOPPLER_BLOCK_SIZE)
                    .collect { block ->
                        recent.addLast(motion(block))
                        if (recent.size > BACKGROUND_READINGS) recent.removeFirst()
                        updateMotion(recent)
                    }
            } catch (e: AudioErrorException) {
                _state.update { it.copy(status = e.error.toUiText()) }
            } finally {
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
