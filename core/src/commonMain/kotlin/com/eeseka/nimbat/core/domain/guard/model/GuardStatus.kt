package com.eeseka.nimbat.core.domain.guard.model

import com.eeseka.nimbat.core.domain.audio.AudioError

data class GuardStatus(
    val phase: GuardPhase = GuardPhase.IDLE,
    val mode: GuardMode = GuardMode.ALARM,
    val locksComputer: Boolean = false,
    val countdownSeconds: Int = 0,
    val calibrationProgress: Float = 0f,
    val isToneClear: Boolean = true,
    val riseDb: Double = 0.0, // How far the strongest echo stands above the still room right now
    val movementCount: Int = 0,
    val error: AudioError? = null
)

enum class GuardPhase {
    IDLE,
    COUNTDOWN,
    CALIBRATING,
    WATCHING,
    RINGING,
    CURTAIN,
    FAILED;

    val isRunning: Boolean
        get() = this != IDLE && this != FAILED
}
