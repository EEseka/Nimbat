package com.eeseka.nimbat.core.domain.guard

import com.eeseka.nimbat.core.domain.guard.model.GuardMode
import com.eeseka.nimbat.core.domain.guard.model.GuardStatus
import kotlinx.coroutines.flow.StateFlow

interface GuardMonitor {
    val status: StateFlow<GuardStatus>

    fun setMode(mode: GuardMode)
    fun setLocksComputer(isOn: Boolean)

    /** Counts down (except for the curtain), learns the still room, then watches and logs until [stop]. */
    fun start()
    fun stop()

    /** Lifts the curtain once you're back, then watches for you leaving again. */
    fun liftCurtain()
}
