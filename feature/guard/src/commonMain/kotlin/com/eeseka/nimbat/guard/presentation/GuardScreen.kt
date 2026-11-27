package com.eeseka.nimbat.guard.presentation

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
import com.eeseka.nimbat.core.design_system.components.modals_and_overlays.NimbatDialog
import com.eeseka.nimbat.core.design_system.components.modals_and_overlays.NimbatFlashType
import com.eeseka.nimbat.core.design_system.components.modals_and_overlays.showFlashMessage
import com.eeseka.nimbat.core.design_system.components.navigation.NimbatIosBarButtonItem
import com.eeseka.nimbat.core.design_system.components.navigation.NimbatTopAppBar
import com.eeseka.nimbat.core.design_system.components.util.AppHaptic
import com.eeseka.nimbat.core.design_system.components.util.rememberAppHaptic
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.core.domain.guard.model.GuardLogEntryKind
import com.eeseka.nimbat.core.domain.guard.model.GuardMode
import com.eeseka.nimbat.core.domain.guard.model.GuardPhase
import com.eeseka.nimbat.core.domain.util.PlatformUtils
import com.eeseka.nimbat.core.presentation.permissions.MicrophonePermissionEffect
import com.eeseka.nimbat.core.presentation.permissions.MicrophonePermissionResumeEffect
import com.eeseka.nimbat.core.presentation.permissions.PermissionState
import com.eeseka.nimbat.core.presentation.permissions.rememberPermissionController
import com.eeseka.nimbat.core.presentation.util.DeviceConfiguration
import com.eeseka.nimbat.core.presentation.util.KeepScreenOnEffect
import com.eeseka.nimbat.core.presentation.util.ObserveAsEvents
import com.eeseka.nimbat.core.presentation.util.currentDeviceConfiguration
import com.eeseka.nimbat.guard.presentation.components.AlarmOverlay
import com.eeseka.nimbat.guard.presentation.components.GuardCurtain
import com.eeseka.nimbat.guard.presentation.components.SinglePaneGuard
import com.eeseka.nimbat.guard.presentation.components.TwoPaneGuard
import com.eeseka.nimbat.guard.presentation.components.WatchingOverlay
import com.eeseka.nimbat.guard.presentation.model.GuardLogEntryUi
import com.eeseka.nimbat.guard.presentation.model.GuardSessionUi
import com.eeseka.nimbat.sound.GestureDirection
import kotlinx.collections.immutable.persistentListOf
import nimbat.feature.guard.generated.resources.Res
import nimbat.feature.guard.generated.resources.back
import nimbat.feature.guard.generated.resources.cancel
import nimbat.feature.guard.generated.resources.clear
import nimbat.feature.guard.generated.resources.clear_log_message
import nimbat.feature.guard.generated.resources.clear_log_title
import nimbat.feature.guard.generated.resources.guard
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GuardRoot(
    navigateBack: () -> Unit,
    viewModel: GuardViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val permissionController = rememberPermissionController()
    val hapticFeedback = rememberAppHaptic()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            GuardEvent.AlarmStarted -> hapticFeedback(AppHaptic.Warning)

            is GuardEvent.Error -> {
                snackbarHostState.showFlashMessage(
                    message = event.message.asStringAsync(),
                    type = NimbatFlashType.Error
                )
            }

            GuardEvent.OpenMicrophoneSettings -> permissionController.openAppSettings()
        }
    }

    GuardScreen(
        state = state,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
        navigateBack = navigateBack
    )
}

@Composable
fun GuardScreen(
    state: GuardState,
    onAction: (GuardAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    navigateBack: () -> Unit
) {
    val config = currentDeviceConfiguration()
    val canOpenAppSettings = rememberPermissionController().canOpenAppSettings
    val isScreenUpright = remember { PlatformUtils.isComputer() }
    val isWeb = remember { PlatformUtils.isWeb() }
    val hapticFeedback = rememberAppHaptic()
    val isRunning = state.phase.isRunning

    if (isRunning) {
        KeepScreenOnEffect()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (!isScreenUpright) onAction(GuardAction.OnScreenStopped)
    }

    MicrophonePermissionResumeEffect { permissionState ->
        onAction(GuardAction.OnMicrophonePermissionChecked(permissionState))
    }

    MicrophonePermissionEffect(isEnabled = state.isRequestingMicrophone) { permissionState ->
        onAction(GuardAction.OnMicrophonePermissionResolved(permissionState))
    }

    if (state.showClearLogDialog) {
        NimbatDialog(
            onDismissRequest = { onAction(GuardAction.OnClearLogDismissed) },
            title = stringResource(Res.string.clear_log_title),
            message = stringResource(Res.string.clear_log_message),
            confirmText = stringResource(Res.string.clear),
            onConfirm = { onAction(GuardAction.OnClearLogConfirmed) },
            dismissText = stringResource(Res.string.cancel),
            isDestructive = true
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NimbatScaffold(
            snackbarHostState = snackbarHostState,
            topBar = {
                if (isRunning) return@NimbatScaffold
                val backLabel = stringResource(Res.string.back)

                NimbatTopAppBar(
                    title = stringResource(Res.string.guard),
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
                        SinglePaneGuard(
                            state = state,
                            configuration = config,
                            canOpenAppSettings = canOpenAppSettings,
                            isScreenUpright = isScreenUpright,
                            isWeb = isWeb,
                            topInset = topInset,
                            bottomInset = bottomInset,
                            onAction = onAction
                        )
                    }

                    DeviceConfiguration.MOBILE_LANDSCAPE,
                    DeviceConfiguration.TABLET_LANDSCAPE,
                    DeviceConfiguration.DESKTOP -> {
                        TwoPaneGuard(
                            state = state,
                            configuration = config,
                            canOpenAppSettings = canOpenAppSettings,
                            isScreenUpright = isScreenUpright,
                            isWeb = isWeb,
                            topInset = topInset,
                            bottomInset = bottomInset,
                            onAction = onAction
                        )
                    }
                }
            }
        }

        when (state.phase) {
            GuardPhase.COUNTDOWN,
            GuardPhase.CALIBRATING,
            GuardPhase.WATCHING -> WatchingOverlay(
                state = state,
                configuration = config,
                onStopClick = {
                    hapticFeedback(AppHaptic.ImpactLight)
                    onAction(GuardAction.OnStopClick)
                }
            )

            GuardPhase.RINGING -> AlarmOverlay(
                configuration = config,
                onTurnOffClick = {
                    hapticFeedback(AppHaptic.ImpactMedium)
                    onAction(GuardAction.OnStopClick)
                }
            )

            GuardPhase.CURTAIN -> GuardCurtain(onLift = { onAction(GuardAction.OnCurtainLifted) })

            GuardPhase.IDLE,
            GuardPhase.FAILED -> Unit
        }
    }
}

@PreviewLightDark
@Preview(name = "Desktop", widthDp = 1280, heightDp = 800)
@Composable
private fun GuardScreenPreview() {
    NimbatTheme {
        GuardScreen(
            state = GuardState(
                microphonePermission = PermissionState.GRANTED,
                sessions = persistentListOf(
                    GuardSessionUi(
                        id = 1,
                        mode = GuardMode.ALARM,
                        startedAtMillis = 0L,
                        endedAtMillis = 1_800_000L,
                        entries = persistentListOf(
                            GuardLogEntryUi(
                                kind = GuardLogEntryKind.MOVEMENT,
                                atMillis = 300_000L,
                                durationMillis = 2_400L,
                                strength = 0.8f,
                                direction = GestureDirection.TOWARD
                            )
                        )
                    )
                )
            ),
            onAction = {},
            snackbarHostState = remember { SnackbarHostState() },
            navigateBack = {}
        )
    }
}
