package com.eeseka.nimbat.core.design_system.components.util

import androidx.compose.runtime.Composable

// Computers have no vibration motor, so desktop haptics do nothing
@Composable
actual fun rememberAppHaptic(): (AppHaptic) -> Unit = {}
