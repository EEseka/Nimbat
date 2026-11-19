package com.eeseka.nimbat.sound_check

import com.eeseka.nimbat.core.presentation.util.UiText

// Temporary screen for trying platform audio on real devices; deleted once Onboarding's sound check exists
data class SoundCheckState(
    val status: UiText = UiText.DynamicString("Tap a check to start."),
    val runningCheck: SoundCheck? = null,
    val pendingCheck: SoundCheck? = null,
    val motionWord: String? = null,
    val motionReadout: String? = null
)

enum class SoundCheck {
    CHIRP,
    MESSAGE,
    MOTION
}
