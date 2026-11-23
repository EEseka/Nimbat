package com.eeseka.nimbat.settings.presentation

import com.eeseka.nimbat.core.presentation.util.UiText

sealed interface SettingsEvent {
    data class Error(val message: UiText) : SettingsEvent
    data object OpenMicrophoneSettings : SettingsEvent
    data class SoundCheckFinished(val isHeard: Boolean) : SettingsEvent
    data object DataCleared : SettingsEvent
}
