package com.eeseka.nimbat.home.presentation

sealed interface HomeAction {
    data object OnRunSoundCheckClick : HomeAction
}
