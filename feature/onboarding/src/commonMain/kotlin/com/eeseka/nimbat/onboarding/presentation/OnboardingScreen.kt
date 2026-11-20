package com.eeseka.nimbat.onboarding.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.eeseka.nimbat.core.design_system.components.layouts.NimbatScaffold
import com.eeseka.nimbat.core.design_system.components.util.AppHaptic
import com.eeseka.nimbat.core.design_system.components.util.rememberAppHaptic
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.core.presentation.permissions.MicrophonePermissionEffect
import com.eeseka.nimbat.core.presentation.permissions.MicrophonePermissionResumeEffect
import com.eeseka.nimbat.core.presentation.permissions.PermissionState
import com.eeseka.nimbat.core.presentation.permissions.rememberPermissionController
import com.eeseka.nimbat.core.presentation.util.DeviceConfiguration
import com.eeseka.nimbat.core.presentation.util.ObserveAsEvents
import com.eeseka.nimbat.core.presentation.util.clearFocusOnTap
import com.eeseka.nimbat.core.presentation.util.currentDeviceConfiguration
import com.eeseka.nimbat.onboarding.presentation.components.DeviceNameField
import com.eeseka.nimbat.onboarding.presentation.components.OnboardingControls
import com.eeseka.nimbat.onboarding.presentation.components.OnboardingPageContent
import com.eeseka.nimbat.onboarding.presentation.components.illustrations.DistanceIllustration
import com.eeseka.nimbat.onboarding.presentation.components.illustrations.MicrophoneIllustration
import com.eeseka.nimbat.onboarding.presentation.components.illustrations.MotionIllustration
import com.eeseka.nimbat.onboarding.presentation.components.illustrations.NameIllustration
import com.eeseka.nimbat.onboarding.presentation.components.illustrations.SonarIllustration
import com.eeseka.nimbat.onboarding.presentation.components.illustrations.SoundCheckIllustration
import com.eeseka.nimbat.onboarding.presentation.mappers.getIcon
import com.eeseka.nimbat.onboarding.presentation.mappers.getTitle
import com.eeseka.nimbat.onboarding.presentation.model.OnboardingPage
import com.eeseka.nimbat.onboarding.presentation.util.getControlsUi
import com.eeseka.nimbat.onboarding.presentation.util.getDescription
import com.eeseka.nimbat.onboarding.presentation.util.unlockedPageCount
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingRoot(
    navigateToMain: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { state.unlockedPageCount() })
    val haptic = rememberAppHaptic()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            OnboardingEvent.MicrophoneGranted -> {
                haptic(AppHaptic.Success)
                snapshotFlow { pagerState.pageCount }.first { it > OnboardingPage.SOUND_CHECK.ordinal }
                pagerState.animateScrollToPage(OnboardingPage.SOUND_CHECK.ordinal)
            }
            OnboardingEvent.ResumeAtMicrophone -> pagerState.scrollToPage(OnboardingPage.MICROPHONE.ordinal)
            is OnboardingEvent.SoundCheckFinished -> {
                haptic(if (event.isHeard) AppHaptic.Success else AppHaptic.Warning)
            }
            OnboardingEvent.Success -> navigateToMain()
        }
    }

    OnboardingScreen(
        state = state,
        pagerState = pagerState,
        onAction = viewModel::onAction
    )
}

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    pagerState: PagerState,
    onAction: (OnboardingAction) -> Unit
) {
    val config = currentDeviceConfiguration()
    val scope = rememberCoroutineScope()
    val haptic = rememberAppHaptic()
    val permissionController = rememberPermissionController()

    MicrophonePermissionEffect(isEnabled = state.isRequestingMicrophone) { permission ->
        onAction(OnboardingAction.OnMicrophonePermissionResolved(permission))
    }

    MicrophonePermissionResumeEffect { permission ->
        onAction(OnboardingAction.OnMicrophonePermissionChecked(permission))
    }

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = pagerState.currentPage > 0,
        onBackCompleted = {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
        }
    )

    val onNext: () -> Unit = {
        haptic(AppHaptic.Selection)
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
    }

    val onGetStarted: () -> Unit = {
        haptic(AppHaptic.ImpactMedium)
        onAction(OnboardingAction.OnGetStartedClick)
    }

    NimbatScaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clearFocusOnTap(),
            contentAlignment = Alignment.Center
        ) {
            when (config) {
                DeviceConfiguration.MOBILE_LANDSCAPE -> {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OnboardingPager(
                            state = state,
                            pagerState = pagerState,
                            deviceConfiguration = config,
                            canOpenAppSettings = permissionController.canOpenAppSettings,
                            modifier = Modifier.weight(1f)
                        )

                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            OnboardingPageControls(
                                state = state,
                                pagerState = pagerState,
                                canOpenAppSettings = permissionController.canOpenAppSettings,
                                onAction = onAction,
                                onNext = onNext,
                                onOpenSettings = permissionController::openAppSettings,
                                onGetStarted = onGetStarted
                            )
                        }
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier.widthIn(max = 600.dp).fillMaxSize().padding(24.dp)
                    ) {
                        OnboardingPager(
                            state = state,
                            pagerState = pagerState,
                            deviceConfiguration = config,
                            canOpenAppSettings = permissionController.canOpenAppSettings,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        OnboardingPageControls(
                            state = state,
                            pagerState = pagerState,
                            canOpenAppSettings = permissionController.canOpenAppSettings,
                            onAction = onAction,
                            onNext = onNext,
                            onOpenSettings = permissionController::openAppSettings,
                            onGetStarted = onGetStarted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPager(
    state: OnboardingState,
    pagerState: PagerState,
    deviceConfiguration: DeviceConfiguration,
    canOpenAppSettings: Boolean,
    modifier: Modifier = Modifier
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier
    ) { index ->
        val page = OnboardingPage.entries[index]
        OnboardingPageContent(
            pageIndex = index,
            title = page.getTitle(),
            description = page.getDescription(state, canOpenAppSettings),
            pageOffset = { (pagerState.currentPage - index) + pagerState.currentPageOffsetFraction },
            illustration = { illustrationModifier ->
                when (page) {
                    OnboardingPage.SONAR -> SonarIllustration(illustrationModifier)
                    OnboardingPage.DISTANCE -> DistanceIllustration(illustrationModifier)
                    OnboardingPage.MOTION -> MotionIllustration(illustrationModifier)
                    OnboardingPage.MICROPHONE -> MicrophoneIllustration(
                        isGranted = state.microphonePermission == PermissionState.GRANTED,
                        modifier = illustrationModifier
                    )
                    OnboardingPage.SOUND_CHECK -> SoundCheckIllustration(
                        status = state.soundCheckStatus,
                        clarity = state.soundCheckClarity,
                        modifier = illustrationModifier
                    )
                    OnboardingPage.NAME -> NameIllustration(
                        name = state.deviceNameTextState.text.toString(),
                        deviceConfiguration = deviceConfiguration,
                        modifier = illustrationModifier
                    )
                }
            },
            extraContent = if (page == OnboardingPage.NAME) {
                {
                    DeviceNameField(
                        state = state.deviceNameTextState,
                        deviceIcon = deviceConfiguration.getIcon(),
                        errorMessage = state.deviceNameError?.asString()
                    )
                }
            } else {
                null
            }
        )
    }
}

@Composable
private fun OnboardingPageControls(
    state: OnboardingState,
    pagerState: PagerState,
    canOpenAppSettings: Boolean,
    onAction: (OnboardingAction) -> Unit,
    onNext: () -> Unit,
    onOpenSettings: () -> Unit,
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val controlsUi = OnboardingPage.entries[pagerState.currentPage].getControlsUi(
        state = state,
        canOpenAppSettings = canOpenAppSettings,
        onAction = onAction,
        onNext = onNext,
        onOpenSettings = onOpenSettings,
        onGetStarted = onGetStarted
    )

    OnboardingControls(
        pageCount = OnboardingPage.entries.size,
        currentPage = pagerState.currentPage,
        pagerPosition = { pagerState.currentPage + pagerState.currentPageOffsetFraction },
        primaryText = controlsUi.primaryText,
        onPrimaryClick = controlsUi.onPrimaryClick,
        isPrimaryLoading = controlsUi.isPrimaryLoading,
        primaryLoadingText = controlsUi.primaryLoadingText,
        secondaryText = controlsUi.secondaryText,
        onSecondaryClick = controlsUi.onSecondaryClick,
        modifier = modifier
    )
}

@PreviewLightDark
@Preview(name = "Mobile landscape", widthDp = 900, heightDp = 400)
@Composable
private fun OnboardingScreenPreview() {
    NimbatTheme {
        OnboardingScreen(
            state = OnboardingState(),
            pagerState = rememberPagerState(pageCount = { OnboardingState().unlockedPageCount() }),
            onAction = {}
        )
    }
}
