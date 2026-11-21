package com.eeseka.nimbat.onboarding.presentation.util

import androidx.compose.runtime.Composable
import com.eeseka.nimbat.core.presentation.permissions.PermissionState
import com.eeseka.nimbat.onboarding.presentation.OnboardingState
import com.eeseka.nimbat.onboarding.presentation.model.OnboardingPage
import com.eeseka.nimbat.onboarding.presentation.model.SoundCheckStatus
import nimbat.feature.onboarding.generated.resources.Res
import nimbat.feature.onboarding.generated.resources.distance_description
import nimbat.feature.onboarding.generated.resources.microphone_blocked_browser_description
import nimbat.feature.onboarding.generated.resources.microphone_blocked_description
import nimbat.feature.onboarding.generated.resources.microphone_description
import nimbat.feature.onboarding.generated.resources.microphone_granted_description
import nimbat.feature.onboarding.generated.resources.motion_description
import nimbat.feature.onboarding.generated.resources.name_description
import nimbat.feature.onboarding.generated.resources.sonar_description
import nimbat.feature.onboarding.generated.resources.sound_check_description
import nimbat.feature.onboarding.generated.resources.sound_check_failed_description
import nimbat.feature.onboarding.generated.resources.sound_check_listening_description
import nimbat.feature.onboarding.generated.resources.sound_check_passed_description
import nimbat.feature.onboarding.generated.resources.sound_check_passed_safari_description
import org.jetbrains.compose.resources.stringResource

@Composable
fun OnboardingPage.getDescription(state: OnboardingState, canOpenAppSettings: Boolean): String = when (this) {
    OnboardingPage.SONAR -> stringResource(Res.string.sonar_description)
    OnboardingPage.DISTANCE -> stringResource(Res.string.distance_description)
    OnboardingPage.MOTION -> stringResource(Res.string.motion_description)
    OnboardingPage.MICROPHONE -> stringResource(
        when (state.microphonePermission) {
            PermissionState.GRANTED -> Res.string.microphone_granted_description
            PermissionState.PERMANENTLY_DENIED -> if (canOpenAppSettings) {
                Res.string.microphone_blocked_description
            } else {
                Res.string.microphone_blocked_browser_description
            }
            else -> Res.string.microphone_description
        }
    )
    OnboardingPage.SOUND_CHECK -> state.soundCheckError?.asString() ?: stringResource(
        when (state.soundCheckStatus) {
            SoundCheckStatus.NOT_STARTED -> Res.string.sound_check_description
            SoundCheckStatus.LISTENING -> Res.string.sound_check_listening_description
            SoundCheckStatus.PASSED -> if (state.canHearOwnChirps) {
                Res.string.sound_check_passed_description
            } else {
                Res.string.sound_check_passed_safari_description
            }
            SoundCheckStatus.FAILED -> Res.string.sound_check_failed_description
        }
    )
    OnboardingPage.NAME -> stringResource(Res.string.name_description)
}
