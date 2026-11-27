package com.eeseka.nimbat

import com.eeseka.nimbat.core.domain.guard.GuardMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class ApplicationStateHolder(
    private val applicationScope: CoroutineScope,
    private val guardMonitor: GuardMonitor
) {

    private val _state = MutableStateFlow(ApplicationState())
    val state = _state
        .onStart { observeGuard() }
        .stateIn(
            scope = applicationScope,
            started = SharingStarted.Lazily,
            initialValue = ApplicationState()
        )

    fun onWindowCloseRequest() {
        val isGuardRunning = _state.value.guardPhase.isRunning
        _state.update { if (isGuardRunning) it.copy(isWindowVisible = false) else it.copy(isOpen = false) }
    }

    fun onOpenClick() {
        _state.update { it.copy(isWindowVisible = true, openRequests = it.openRequests + 1) }
    }

    fun onStopGuardClick() {
        guardMonitor.stop()
    }

    fun onCurtainLift() {
        guardMonitor.liftCurtain()
    }

    private fun observeGuard() {
        guardMonitor.status
            .onEach { status -> _state.update { it.copy(guardPhase = status.phase) } }
            .launchIn(applicationScope)
    }
}
