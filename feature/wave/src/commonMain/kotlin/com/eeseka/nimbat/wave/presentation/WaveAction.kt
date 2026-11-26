package com.eeseka.nimbat.wave.presentation

import com.eeseka.nimbat.core.presentation.permissions.PermissionState
import com.eeseka.nimbat.wave.presentation.model.CaveLane
import com.eeseka.nimbat.wave.presentation.model.WaveDemo

sealed interface WaveAction {
    data object OnScreenStarted : WaveAction
    data object OnScreenStopped : WaveAction

    data class OnMicrophonePermissionChecked(val state: PermissionState) : WaveAction
    data object OnAllowMicrophoneClick : WaveAction
    data class OnMicrophonePermissionResolved(val state: PermissionState) : WaveAction
    data object OnTryAgainClick : WaveAction

    data class OnDemoSelected(val demo: WaveDemo) : WaveAction
    data class OnSlideSettled(val index: Int) : WaveAction
    data class OnAnimalSettled(val index: Int) : WaveAction
    data class OnCaveTap(val lane: CaveLane) : WaveAction
    data class OnBatDashFrame(val frameTimeNanos: Long) : WaveAction
}
