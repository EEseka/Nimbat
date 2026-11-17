package com.eeseka.nimbat.core.design_system.theme

import androidx.compose.runtime.Composable
import com.eeseka.nimbat.core.domain.settings.AppTheme

// Desktop draws every view through Compose, so there are no native views to theme.
@Composable
actual fun ApplyNativeTheme(theme: AppTheme) = Unit
