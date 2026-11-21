package com.eeseka.nimbat.onboarding.presentation

import androidx.compose.foundation.text.input.TextFieldState
import com.eeseka.nimbat.core.presentation.permissions.PermissionState
import com.eeseka.nimbat.core.presentation.util.UiText
import com.eeseka.nimbat.onboarding.presentation.model.SoundCheckStatus

data class OnboardingState(
    val microphonePermission: PermissionState = PermissionState.NOT_DETERMINED,
    val isRequestingMicrophone: Boolean = false,

    val soundCheckStatus: SoundCheckStatus = SoundCheckStatus.NOT_STARTED,
    val soundCheckClarity: Float = 0f,
    val soundCheckError: UiText? = null,
    val canHearOwnChirps: Boolean = true,

    val deviceNameTextState: TextFieldState = TextFieldState(),
    val deviceNameError: UiText? = null,
    val isSaving: Boolean = false
)
