package com.eeseka.nimbat.guard.presentation

import com.eeseka.nimbat.core.presentation.util.UiText

sealed interface GuardEvent {
    data object AlarmStarted : GuardEvent
    data class Error(val message: UiText) : GuardEvent
    data object OpenMicrophoneSettings : GuardEvent
}
