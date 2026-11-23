package com.eeseka.nimbat.settings.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eeseka.nimbat.AppConfig
import com.eeseka.nimbat.core.design_system.components.layouts.NimbatScaffold
import com.eeseka.nimbat.core.design_system.components.modals_and_overlays.NimbatDialog
import com.eeseka.nimbat.core.design_system.components.modals_and_overlays.NimbatFlashType
import com.eeseka.nimbat.core.design_system.components.modals_and_overlays.showFlashMessage
import com.eeseka.nimbat.core.design_system.components.navigation.NimbatTopAppBar
import com.eeseka.nimbat.core.design_system.components.util.AppHaptic
import com.eeseka.nimbat.core.design_system.components.util.rememberAppHaptic
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.core.presentation.device.mappers.getIcon
import com.eeseka.nimbat.core.presentation.permissions.MicrophonePermissionEffect
import com.eeseka.nimbat.core.presentation.permissions.MicrophonePermissionResumeEffect
import com.eeseka.nimbat.core.presentation.permissions.rememberPermissionController
import com.eeseka.nimbat.core.presentation.util.DeviceConfiguration
import com.eeseka.nimbat.core.presentation.util.ObserveAsEvents
import com.eeseka.nimbat.core.presentation.util.currentDeviceConfiguration
import com.eeseka.nimbat.settings.presentation.components.RenameDeviceSheet
import com.eeseka.nimbat.settings.presentation.components.SinglePaneSettings
import com.eeseka.nimbat.settings.presentation.components.TwoPaneSettings
import nimbat.feature.settings.generated.resources.Res
import nimbat.feature.settings.generated.resources.cancel
import nimbat.feature.settings.generated.resources.clear
import nimbat.feature.settings.generated.resources.clear_all_data_confirm_message
import nimbat.feature.settings.generated.resources.clear_all_data_confirm_title
import nimbat.feature.settings.generated.resources.settings
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val SOURCE_CODE_URL = "https://github.com/EEseka/nimbat"

@Composable
fun SettingsRoot(
    mainShellPadding: PaddingValues,
    navigateToOnboarding: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val permissionController = rememberPermissionController()
    val hapticFeedback = rememberAppHaptic()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is SettingsEvent.SoundCheckFinished -> {
                hapticFeedback(if (event.isHeard) AppHaptic.Success else AppHaptic.Error)
            }

            is SettingsEvent.Error -> {
                snackbarHostState.showFlashMessage(
                    message = event.message.asStringAsync(),
                    type = NimbatFlashType.Error
                )
            }

            SettingsEvent.OpenMicrophoneSettings -> permissionController.openAppSettings()
            SettingsEvent.DataCleared -> navigateToOnboarding()
        }
    }

    SettingsScreen(
        state = state,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
        mainShellPadding = mainShellPadding
    )
}

@Composable
fun SettingsScreen(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    mainShellPadding: PaddingValues
) {
    val uriHandler = LocalUriHandler.current
    val config = currentDeviceConfiguration()
    val canOpenAppSettings = rememberPermissionController().canOpenAppSettings

    MicrophonePermissionResumeEffect { permissionState ->
        onAction(SettingsAction.OnMicrophonePermissionChecked(permissionState))
    }

    MicrophonePermissionEffect(isEnabled = state.isRequestingMicrophone) { permissionState ->
        onAction(SettingsAction.OnMicrophonePermissionResolved(permissionState))
    }

    NimbatScaffold(
        snackbarHostState = snackbarHostState,
        topBar = {
            NimbatTopAppBar(title = stringResource(Res.string.settings))
        }
    ) { scaffoldPadding ->
        val topInset = scaffoldPadding.calculateTopPadding() + 24.dp
        val bottomInset = mainShellPadding.calculateBottomPadding() + 24.dp

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            when (config) {
                DeviceConfiguration.MOBILE_PORTRAIT,
                DeviceConfiguration.TABLET_PORTRAIT -> {
                    SinglePaneSettings(
                        state = state,
                        configuration = config,
                        canOpenAppSettings = canOpenAppSettings,
                        appVersion = AppConfig.APP_VERSION,
                        topInset = topInset,
                        bottomInset = bottomInset,
                        onAction = onAction,
                        onSourceCodeClick = { uriHandler.openUri(SOURCE_CODE_URL) }
                    )
                }

                DeviceConfiguration.MOBILE_LANDSCAPE,
                DeviceConfiguration.TABLET_LANDSCAPE,
                DeviceConfiguration.DESKTOP -> {
                    TwoPaneSettings(
                        state = state,
                        configuration = config,
                        canOpenAppSettings = canOpenAppSettings,
                        appVersion = AppConfig.APP_VERSION,
                        topInset = topInset,
                        bottomInset = bottomInset,
                        onAction = onAction,
                        onSourceCodeClick = { uriHandler.openUri(SOURCE_CODE_URL) }
                    )
                }
            }
        }
    }

    if (state.showRenameSheet) {
        RenameDeviceSheet(
            nameState = state.deviceNameTextState,
            deviceIcon = config.getIcon(),
            errorMessage = state.deviceNameError?.asString(),
            isSaving = state.isSavingDeviceName,
            onSaveClick = { onAction(SettingsAction.OnSaveDeviceNameClick) },
            onDismissRequest = { onAction(SettingsAction.OnDismissRenameSheet) }
        )
    }

    if (state.showClearDataDialog) {
        NimbatDialog(
            onDismissRequest = { onAction(SettingsAction.OnDismissClearDataDialog) },
            title = stringResource(Res.string.clear_all_data_confirm_title),
            message = stringResource(Res.string.clear_all_data_confirm_message),
            confirmText = stringResource(Res.string.clear),
            dismissText = stringResource(Res.string.cancel),
            onConfirm = { onAction(SettingsAction.OnClearDataConfirmed) },
            isDestructive = true
        )
    }
}

@PreviewLightDark
@Preview(name = "Mobile landscape", widthDp = 900, heightDp = 400)
@Preview(name = "Desktop", widthDp = 1280, heightDp = 800)
@Composable
private fun SettingsScreenPreview() {
    NimbatTheme {
        SettingsScreen(
            state = SettingsState(
                deviceName = "Emmanuel's iPhone",
                deviceNameTextState = TextFieldState("Emmanuel's iPhone"),
                roomTemperatureCelsius = 24.0,
                volume = 0.8f,
                distanceOffsetMeters = 0.07
            ),
            onAction = {},
            snackbarHostState = remember { SnackbarHostState() },
            mainShellPadding = PaddingValues(0.dp)
        )
    }
}
