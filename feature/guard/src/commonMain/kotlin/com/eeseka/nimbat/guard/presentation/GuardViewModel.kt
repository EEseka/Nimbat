package com.eeseka.nimbat.guard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eeseka.nimbat.core.domain.guard.ComputerLock
import com.eeseka.nimbat.core.domain.guard.GuardMonitor
import com.eeseka.nimbat.core.domain.guard.GuardStorage
import com.eeseka.nimbat.core.domain.guard.model.GuardPhase
import com.eeseka.nimbat.core.domain.guard.model.GuardStatus
import com.eeseka.nimbat.core.presentation.audio.mappers.toUiText
import com.eeseka.nimbat.core.presentation.permissions.PermissionState
import com.eeseka.nimbat.guard.presentation.mappers.toGuardSessionUi
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GuardViewModel(
    private val guardMonitor: GuardMonitor,
    private val guardStorage: GuardStorage,
    computerLock: ComputerLock
) : ViewModel() {

    private companion object {
        /** An echo this far above the still room lights the drawing's waves fully. */
        const val FULL_MOVEMENT_LEVEL_DB = 20.0
    }

    private val eventChannel = Channel<GuardEvent>()
    val events = eventChannel.receiveAsFlow()

    private var hasLoadedInitialData = false
    private var hasAskedForMicrophone = false

    // Starts from the monitor's phase, so coming back to a failed run doesn't announce the old failure again
    private val _state = MutableStateFlow(
        GuardState(phase = guardMonitor.status.value.phase, canLockComputer = computerLock.canLock)
    )

    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                observeStatus()
                observeLog()
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = GuardState()
        )

    fun onAction(action: GuardAction) {
        when (action) {
            GuardAction.OnScreenStopped -> guardMonitor.stop()

            is GuardAction.OnMicrophonePermissionChecked -> onMicrophonePermissionChecked(action.state)
            GuardAction.OnAllowMicrophoneClick -> allowMicrophone()
            is GuardAction.OnMicrophonePermissionResolved -> _state.update {
                it.copy(microphonePermission = action.state, isRequestingMicrophone = false)
            }

            is GuardAction.OnModeSelected -> guardMonitor.setMode(action.mode)
            is GuardAction.OnLockComputerToggled -> guardMonitor.setLocksComputer(action.isOn)

            GuardAction.OnStartClick -> guardMonitor.start()
            GuardAction.OnStopClick -> guardMonitor.stop()
            GuardAction.OnCurtainLifted -> guardMonitor.liftCurtain()

            GuardAction.OnClearLogClick -> _state.update { it.copy(showClearLogDialog = true) }
            GuardAction.OnClearLogDismissed -> _state.update { it.copy(showClearLogDialog = false) }
            GuardAction.OnClearLogConfirmed -> clearLog()
        }
    }

    override fun onCleared() {
        guardMonitor.stop()
    }

    private fun observeStatus() {
        guardMonitor.status
            .onEach { status -> onStatus(status) }
            .launchIn(viewModelScope)
    }

    private fun observeLog() {
        guardStorage.sessions
            .onEach { sessions ->
                val sessionsUi = sessions.map { it.toGuardSessionUi() }.toImmutableList()
                _state.update { it.copy(sessions = sessionsUi) }
            }
            .launchIn(viewModelScope)
    }

    private fun onStatus(status: GuardStatus) {
        val previousPhase = _state.value.phase
        _state.update {
            it.copy(
                phase = status.phase,
                mode = status.mode,
                locksComputer = status.locksComputer,
                countdownSeconds = status.countdownSeconds,
                calibrationProgress = status.calibrationProgress,
                isToneClear = status.isToneClear,
                movementLevel = (status.riseDb / FULL_MOVEMENT_LEVEL_DB).toFloat().coerceIn(0f, 1f),
                movementCount = status.movementCount
            )
        }

        if (status.phase == previousPhase) return
        val event = when (status.phase) {
            GuardPhase.RINGING -> GuardEvent.AlarmStarted
            GuardPhase.FAILED -> status.error?.let { GuardEvent.Error(it.toUiText()) }
            else -> null
        } ?: return
        viewModelScope.launch {
            eventChannel.send(event)
        }
    }

    private fun clearLog() {
        _state.update { it.copy(showClearLogDialog = false) }
        viewModelScope.launch {
            guardStorage.clear()
        }
    }

    private fun onMicrophonePermissionChecked(permission: PermissionState) {
        // Android's quiet check reports a blocked mic as NOT_DETERMINED, so blocked stays blocked until GRANTED
        val wasBlocked = _state.value.microphonePermission == PermissionState.PERMANENTLY_DENIED
        if (wasBlocked && permission != PermissionState.GRANTED) return

        _state.update { it.copy(microphonePermission = permission) }
        if (permission == PermissionState.NOT_DETERMINED && !hasAskedForMicrophone) {
            hasAskedForMicrophone = true
            _state.update { it.copy(isRequestingMicrophone = true) }
        }
    }

    private fun allowMicrophone() {
        if (_state.value.microphonePermission == PermissionState.PERMANENTLY_DENIED) {
            viewModelScope.launch {
                eventChannel.send(GuardEvent.OpenMicrophoneSettings)
            }
        } else {
            _state.update { it.copy(isRequestingMicrophone = true) }
        }
    }
}
