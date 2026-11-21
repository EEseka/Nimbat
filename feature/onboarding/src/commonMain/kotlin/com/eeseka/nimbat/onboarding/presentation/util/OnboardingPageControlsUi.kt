package com.eeseka.nimbat.onboarding.presentation.util

import androidx.compose.runtime.Composable
import com.eeseka.nimbat.core.presentation.permissions.PermissionState
import com.eeseka.nimbat.onboarding.presentation.OnboardingAction
import com.eeseka.nimbat.onboarding.presentation.OnboardingState
import com.eeseka.nimbat.onboarding.presentation.model.OnboardingControlsUi
import com.eeseka.nimbat.onboarding.presentation.model.OnboardingPage
import com.eeseka.nimbat.onboarding.presentation.model.SoundCheckStatus
import nimbat.feature.onboarding.generated.resources.Res
import nimbat.feature.onboarding.generated.resources.allow_microphone
import nimbat.feature.onboarding.generated.resources.asking
import nimbat.feature.onboarding.generated.resources.get_started
import nimbat.feature.onboarding.generated.resources.listening
import nimbat.feature.onboarding.generated.resources.next
import nimbat.feature.onboarding.generated.resources.open_settings
import nimbat.feature.onboarding.generated.resources.start_sound_check
import nimbat.feature.onboarding.generated.resources.try_again
import org.jetbrains.compose.resources.stringResource

@Composable
fun OnboardingPage.getControlsUi(
    state: OnboardingState,
    canOpenAppSettings: Boolean,
    onAction: (OnboardingAction) -> Unit,
    onNext: () -> Unit,
    onOpenSettings: () -> Unit,
    onGetStarted: () -> Unit
): OnboardingControlsUi {
    val next = stringResource(Res.string.next)
    return when (this) {
        OnboardingPage.SONAR, OnboardingPage.DISTANCE, OnboardingPage.MOTION -> OnboardingControlsUi(
            primaryText = next,
            onPrimaryClick = onNext
        )
        OnboardingPage.MICROPHONE -> when (state.microphonePermission) {
            PermissionState.GRANTED -> OnboardingControlsUi(
                primaryText = next,
                onPrimaryClick = onNext
            )
            PermissionState.PERMANENTLY_DENIED -> if (canOpenAppSettings) {
                OnboardingControlsUi(
                    primaryText = stringResource(Res.string.open_settings),
                    onPrimaryClick = onOpenSettings
                )
            } else {
                OnboardingControlsUi(
                    primaryText = stringResource(Res.string.try_again),
                    onPrimaryClick = { onAction(OnboardingAction.OnAllowMicrophoneClick) },
                    isPrimaryLoading = state.isRequestingMicrophone,
                    primaryLoadingText = stringResource(Res.string.asking)
                )
            }
            else -> OnboardingControlsUi(
                primaryText = stringResource(Res.string.allow_microphone),
                onPrimaryClick = { onAction(OnboardingAction.OnAllowMicrophoneClick) },
                isPrimaryLoading = state.isRequestingMicrophone,
                primaryLoadingText = stringResource(Res.string.asking)
            )
        }
        OnboardingPage.SOUND_CHECK -> when (state.soundCheckStatus) {
            SoundCheckStatus.NOT_STARTED, SoundCheckStatus.LISTENING -> OnboardingControlsUi(
                primaryText = stringResource(Res.string.start_sound_check),
                onPrimaryClick = { onAction(OnboardingAction.OnStartSoundCheckClick) },
                isPrimaryLoading = state.soundCheckStatus == SoundCheckStatus.LISTENING,
                primaryLoadingText = stringResource(Res.string.listening)
            )
            SoundCheckStatus.PASSED -> OnboardingControlsUi(
                primaryText = next,
                onPrimaryClick = onNext
            )
            SoundCheckStatus.FAILED -> OnboardingControlsUi(
                primaryText = next,
                onPrimaryClick = onNext,
                secondaryText = stringResource(Res.string.try_again),
                onSecondaryClick = { onAction(OnboardingAction.OnStartSoundCheckClick) }
            )
        }
        OnboardingPage.NAME -> OnboardingControlsUi(
            primaryText = stringResource(Res.string.get_started),
            onPrimaryClick = onGetStarted,
            isPrimaryLoading = state.isSaving
        )
    }
}
