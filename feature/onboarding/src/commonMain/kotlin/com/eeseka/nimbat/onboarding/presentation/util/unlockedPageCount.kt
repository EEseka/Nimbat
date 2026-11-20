package com.eeseka.nimbat.onboarding.presentation.util

import com.eeseka.nimbat.core.presentation.permissions.PermissionState
import com.eeseka.nimbat.onboarding.presentation.OnboardingState
import com.eeseka.nimbat.onboarding.presentation.model.OnboardingPage
import com.eeseka.nimbat.onboarding.presentation.model.SoundCheckStatus

fun OnboardingState.unlockedPageCount(): Int = when {
    soundCheckStatus == SoundCheckStatus.PASSED || soundCheckStatus == SoundCheckStatus.FAILED -> OnboardingPage.entries.size
    microphonePermission == PermissionState.GRANTED -> OnboardingPage.SOUND_CHECK.ordinal + 1
    else -> OnboardingPage.MICROPHONE.ordinal + 1
}
