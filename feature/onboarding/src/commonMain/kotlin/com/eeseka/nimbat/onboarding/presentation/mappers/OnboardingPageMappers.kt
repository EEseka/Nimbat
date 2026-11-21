package com.eeseka.nimbat.onboarding.presentation.mappers

import androidx.compose.runtime.Composable
import com.eeseka.nimbat.onboarding.presentation.model.OnboardingPage
import nimbat.feature.onboarding.generated.resources.Res
import nimbat.feature.onboarding.generated.resources.distance_title
import nimbat.feature.onboarding.generated.resources.microphone_title
import nimbat.feature.onboarding.generated.resources.motion_title
import nimbat.feature.onboarding.generated.resources.name_title
import nimbat.feature.onboarding.generated.resources.sonar_title
import nimbat.feature.onboarding.generated.resources.sound_check_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun OnboardingPage.getTitle(): String = stringResource(
    when (this) {
        OnboardingPage.SONAR -> Res.string.sonar_title
        OnboardingPage.DISTANCE -> Res.string.distance_title
        OnboardingPage.MOTION -> Res.string.motion_title
        OnboardingPage.MICROPHONE -> Res.string.microphone_title
        OnboardingPage.SOUND_CHECK -> Res.string.sound_check_title
        OnboardingPage.NAME -> Res.string.name_title
    }
)
