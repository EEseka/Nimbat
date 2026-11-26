package com.eeseka.nimbat.core.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.UIKit.UIApplication

@Composable
actual fun KeepScreenOnEffect() {
    DisposableEffect(Unit) {
        UIApplication.sharedApplication.idleTimerDisabled = true
        onDispose { UIApplication.sharedApplication.idleTimerDisabled = false }
    }
}
