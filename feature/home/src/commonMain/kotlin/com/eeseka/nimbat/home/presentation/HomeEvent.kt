package com.eeseka.nimbat.home.presentation

import com.eeseka.nimbat.core.presentation.util.UiText

sealed interface HomeEvent {
    data class Error(val message: UiText) : HomeEvent
    data class SoundCheckFinished(val isHeard: Boolean) : HomeEvent
}
