package com.eeseka.nimbat.onboarding.presentation

sealed interface OnboardingEvent {
    data object MicrophoneGranted : OnboardingEvent
    data object ResumeAtMicrophone : OnboardingEvent
    data class SoundCheckFinished(val isHeard: Boolean) : OnboardingEvent
    data object Success : OnboardingEvent
}
