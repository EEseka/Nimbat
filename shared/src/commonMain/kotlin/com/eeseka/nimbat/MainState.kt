package com.eeseka.nimbat

import com.eeseka.nimbat.core.domain.settings.AppTheme

data class MainState(
    val isCheckingOnboarding: Boolean = true,
    val hasSeenOnboarding: Boolean = false,
    val theme: AppTheme = AppTheme.SYSTEM,
    val isPureBlackEnabled: Boolean = false,
    val areHapticsEnabled: Boolean = true
)
