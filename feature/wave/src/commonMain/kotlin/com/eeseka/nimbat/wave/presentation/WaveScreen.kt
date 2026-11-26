package com.eeseka.nimbat.wave.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import com.eeseka.nimbat.core.design_system.components.buttons.NimbatIconButton
import com.eeseka.nimbat.core.design_system.components.layouts.NimbatScaffold
import com.eeseka.nimbat.core.design_system.components.modals_and_overlays.NimbatFlashType
import com.eeseka.nimbat.core.design_system.components.modals_and_overlays.showFlashMessage
import com.eeseka.nimbat.core.design_system.components.navigation.NimbatIosBarButtonItem
import com.eeseka.nimbat.core.design_system.components.navigation.NimbatTopAppBar
import com.eeseka.nimbat.core.design_system.components.util.AppHaptic
import com.eeseka.nimbat.core.design_system.components.util.rememberAppHaptic
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.core.domain.util.PlatformUtils
import com.eeseka.nimbat.core.presentation.permissions.MicrophonePermissionEffect
import com.eeseka.nimbat.core.presentation.permissions.MicrophonePermissionResumeEffect
import com.eeseka.nimbat.core.presentation.permissions.PermissionState
import com.eeseka.nimbat.core.presentation.permissions.rememberPermissionController
import com.eeseka.nimbat.core.presentation.util.DeviceConfiguration
import com.eeseka.nimbat.core.presentation.util.KeepScreenOnEffect
import com.eeseka.nimbat.core.presentation.util.ObserveAsEvents
import com.eeseka.nimbat.core.presentation.util.currentDeviceConfiguration
import com.eeseka.nimbat.sound.GestureDirection
import com.eeseka.nimbat.wave.presentation.components.SinglePaneWave
import com.eeseka.nimbat.wave.presentation.components.TwoPaneWave
import com.eeseka.nimbat.wave.presentation.model.WaveSensorStatus
import kotlinx.collections.immutable.persistentListOf
import nimbat.feature.wave.generated.resources.Res
import nimbat.feature.wave.generated.resources.back
import nimbat.feature.wave.generated.resources.wave
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WaveRoot(
    navigateBack: () -> Unit,
    viewModel: WaveViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val permissionController = rememberPermissionController()
    val hapticFeedback = rememberAppHaptic()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            WaveEvent.GestureDetected -> hapticFeedback(AppHaptic.ImpactMedium)
            WaveEvent.BatCrashed -> hapticFeedback(AppHaptic.ImpactHeavy)

            is WaveEvent.Error -> {
                snackbarHostState.showFlashMessage(
                    message = event.message.asStringAsync(),
                    type = NimbatFlashType.Error
                )
            }

            WaveEvent.OpenMicrophoneSettings -> permissionController.openAppSettings()
        }
    }

    WaveScreen(
        state = state,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
        navigateBack = navigateBack
    )
}

@Composable
fun WaveScreen(
    state: WaveState,
    onAction: (WaveAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    navigateBack: () -> Unit
) {
    val config = currentDeviceConfiguration()
    val canOpenAppSettings = rememberPermissionController().canOpenAppSettings
    val isScreenUpright = remember { PlatformUtils.isComputer() }
    val hapticFeedback = rememberAppHaptic()

    KeepScreenOnEffect()

    LifecycleEventEffect(Lifecycle.Event.ON_START) { onAction(WaveAction.OnScreenStarted) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { onAction(WaveAction.OnScreenStopped) }

    MicrophonePermissionResumeEffect { permissionState ->
        onAction(WaveAction.OnMicrophonePermissionChecked(permissionState))
    }

    MicrophonePermissionEffect(isEnabled = state.isRequestingMicrophone) { permissionState ->
        onAction(WaveAction.OnMicrophonePermissionResolved(permissionState))
    }

    NimbatScaffold(
        snackbarHostState = snackbarHostState,
        topBar = {
            val backLabel = stringResource(Res.string.back)

            NimbatTopAppBar(
                title = stringResource(Res.string.wave),
                navigationIcon = {
                    NimbatIconButton(
                        onClick = {
                            hapticFeedback(AppHaptic.ImpactLight)
                            navigateBack()
                        }
                    ) {
                        Icon(
                            imageVector = Lucide.ChevronLeft,
                            contentDescription = backLabel
                        )
                    }
                },
                iosLeadingItems = persistentListOf(
                    NimbatIosBarButtonItem(
                        sfSymbol = "chevron.left",
                        onClick = {
                            hapticFeedback(AppHaptic.ImpactLight)
                            navigateBack()
                        }
                    )
                )
            )
        }
    ) { scaffoldPadding ->
        val topInset = scaffoldPadding.calculateTopPadding() + 16.dp
        val bottomInset = scaffoldPadding.calculateBottomPadding() + 16.dp

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            when (config) {
                DeviceConfiguration.MOBILE_PORTRAIT,
                DeviceConfiguration.TABLET_PORTRAIT -> {
                    SinglePaneWave(
                        state = state,
                        configuration = config,
                        canOpenAppSettings = canOpenAppSettings,
                        isScreenUpright = isScreenUpright,
                        topInset = topInset,
                        bottomInset = bottomInset,
                        onAction = onAction
                    )
                }

                DeviceConfiguration.MOBILE_LANDSCAPE,
                DeviceConfiguration.TABLET_LANDSCAPE,
                DeviceConfiguration.DESKTOP -> {
                    TwoPaneWave(
                        state = state,
                        configuration = config,
                        canOpenAppSettings = canOpenAppSettings,
                        isScreenUpright = isScreenUpright,
                        topInset = topInset,
                        bottomInset = bottomInset,
                        onAction = onAction
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Preview(name = "Mobile landscape", widthDp = 900, heightDp = 400)
@Preview(name = "Desktop", widthDp = 1280, heightDp = 800)
@Composable
private fun WaveScreenPreview() {
    NimbatTheme {
        WaveScreen(
            state = WaveState(
                microphonePermission = PermissionState.GRANTED,
                sensorStatus = WaveSensorStatus.SENSING,
                towardLevel = 0.7f,
                lastGesture = GestureDirection.TOWARD,
                slideIndex = 1
            ),
            onAction = {},
            snackbarHostState = remember { SnackbarHostState() },
            navigateBack = {}
        )
    }
}
