package com.eeseka.nimbat

import com.eeseka.nimbat.core.domain.guard.model.GuardPhase

data class ApplicationState(
    val isWindowVisible: Boolean = true,
    val openRequests: Int = 0, // Each Open click brings the window to the front, even when it's already showing
    val isOpen: Boolean = true,
    val guardPhase: GuardPhase = GuardPhase.IDLE
)
