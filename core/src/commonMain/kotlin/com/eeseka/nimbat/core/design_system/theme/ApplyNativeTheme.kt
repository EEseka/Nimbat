package com.eeseka.nimbat.core.design_system.theme

import androidx.compose.runtime.Composable
import com.eeseka.nimbat.core.domain.settings.AppTheme

/**
 * Tells the platform's own views which theme the user picked.
 * NimbatTheme only reaches Compose; native views follow the device's setting unless told otherwise.
 */
@Composable
expect fun ApplyNativeTheme(theme: AppTheme)
