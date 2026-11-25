package com.eeseka.nimbat.home.presentation

data class HomeState(
    val isSoundCheckPassed: Boolean? = null,
    val isRunningSoundCheck: Boolean = false,
    val canHearOwnChirps: Boolean = true
)
