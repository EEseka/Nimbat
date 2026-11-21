package com.eeseka.nimbat.onboarding.presentation.model

class OnboardingControlsUi(
    val primaryText: String,
    val onPrimaryClick: () -> Unit,
    val isPrimaryLoading: Boolean = false,
    val primaryLoadingText: String? = null,
    val secondaryText: String? = null,
    val onSecondaryClick: () -> Unit = {}
)
