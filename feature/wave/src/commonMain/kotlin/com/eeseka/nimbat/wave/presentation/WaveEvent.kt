package com.eeseka.nimbat.wave.presentation

import com.eeseka.nimbat.core.presentation.util.UiText

sealed interface WaveEvent {
    data object GestureDetected : WaveEvent
    data object BatCrashed : WaveEvent
    data class Error(val message: UiText) : WaveEvent
    data object OpenMicrophoneSettings : WaveEvent
}
