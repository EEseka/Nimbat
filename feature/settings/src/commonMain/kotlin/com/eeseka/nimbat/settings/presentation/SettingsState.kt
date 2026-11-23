package com.eeseka.nimbat.settings.presentation

import androidx.compose.foundation.text.input.TextFieldState
import com.eeseka.nimbat.core.domain.settings.AppTheme
import com.eeseka.nimbat.core.domain.settings.MeasurementUnits
import com.eeseka.nimbat.core.presentation.permissions.PermissionState
import com.eeseka.nimbat.core.presentation.util.UiText
import com.eeseka.nimbat.sound.DEFAULT_TEMPERATURE_C

data class SettingsState(
    val deviceName: String = "",
    val microphonePermission: PermissionState = PermissionState.GRANTED,
    val isRequestingMicrophone: Boolean = false,

    val deviceNameTextState: TextFieldState = TextFieldState(),
    val deviceNameError: UiText? = null,
    val showRenameSheet: Boolean = false,
    val isSavingDeviceName: Boolean = false,

    val isSoundCheckPassed: Boolean? = null,
    val isRunningSoundCheck: Boolean = false,
    val canHearOwnChirps: Boolean = true,

    val roomTemperatureCelsius: Double = DEFAULT_TEMPERATURE_C,
    val volume: Float = 1f,
    val distanceOffsetMeters: Double = 0.0,
    val measurementUnits: MeasurementUnits = MeasurementUnits.METRIC,

    val appTheme: AppTheme = AppTheme.SYSTEM,
    val isPureBlackEnabled: Boolean = false,
    val areHapticsEnabled: Boolean = true,
    val canUseHaptics: Boolean = true,

    val showClearDataDialog: Boolean = false,
    val isClearingData: Boolean = false,

    val isPlayingChirp: Boolean = false
)
