package com.eeseka.nimbat.settings.presentation

import com.eeseka.nimbat.core.domain.settings.AppTheme
import com.eeseka.nimbat.core.domain.settings.MeasurementUnits
import com.eeseka.nimbat.core.presentation.permissions.PermissionState

sealed interface SettingsAction {
    data class OnMicrophonePermissionChecked(val state: PermissionState) : SettingsAction
    data object OnAllowMicrophoneClick : SettingsAction
    data class OnMicrophonePermissionResolved(val state: PermissionState) : SettingsAction

    data object OnRenameClick : SettingsAction
    data object OnSaveDeviceNameClick : SettingsAction
    data object OnDismissRenameSheet : SettingsAction

    data object OnRunSoundCheckClick : SettingsAction

    data class OnRoomTemperatureChange(val celsius: Double) : SettingsAction
    data object OnRoomTemperatureChangeFinished : SettingsAction
    data class OnVolumeChange(val volume: Float) : SettingsAction
    data object OnVolumeChangeFinished : SettingsAction
    data class OnMeasurementUnitsSelected(val units: MeasurementUnits) : SettingsAction

    data class OnThemeSelected(val theme: AppTheme) : SettingsAction
    data class OnPureBlackToggled(val isEnabled: Boolean) : SettingsAction
    data class OnHapticsToggled(val isEnabled: Boolean) : SettingsAction

    data object OnClearDataClick : SettingsAction
    data object OnClearDataConfirmed : SettingsAction
    data object OnDismissClearDataDialog : SettingsAction

    data object OnLogoClick : SettingsAction
}
