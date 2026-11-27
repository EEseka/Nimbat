package com.eeseka.nimbat.guard.presentation

import com.eeseka.nimbat.core.domain.guard.model.GuardMode
import com.eeseka.nimbat.core.presentation.permissions.PermissionState

sealed interface GuardAction {
    data object OnScreenStopped : GuardAction

    data class OnMicrophonePermissionChecked(val state: PermissionState) : GuardAction
    data object OnAllowMicrophoneClick : GuardAction
    data class OnMicrophonePermissionResolved(val state: PermissionState) : GuardAction

    data class OnModeSelected(val mode: GuardMode) : GuardAction
    data class OnLockComputerToggled(val isOn: Boolean) : GuardAction

    data object OnStartClick : GuardAction
    data object OnStopClick : GuardAction
    data object OnCurtainLifted : GuardAction

    data object OnClearLogClick : GuardAction
    data object OnClearLogConfirmed : GuardAction
    data object OnClearLogDismissed : GuardAction
}
