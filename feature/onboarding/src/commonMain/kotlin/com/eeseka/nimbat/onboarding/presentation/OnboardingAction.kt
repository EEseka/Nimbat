package com.eeseka.nimbat.onboarding.presentation

import com.eeseka.nimbat.core.presentation.permissions.PermissionState

sealed interface OnboardingAction {
    data object OnAllowMicrophoneClick : OnboardingAction
    data class OnMicrophonePermissionChecked(val state: PermissionState) : OnboardingAction
    data class OnMicrophonePermissionResolved(val state: PermissionState) : OnboardingAction
    data object OnStartSoundCheckClick : OnboardingAction
    data object OnGetStartedClick : OnboardingAction
}
