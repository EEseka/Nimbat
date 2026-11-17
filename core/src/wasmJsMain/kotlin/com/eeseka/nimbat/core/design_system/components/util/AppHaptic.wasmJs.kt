package com.eeseka.nimbat.core.design_system.components.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberAppHaptic(): (AppHaptic) -> Unit {
    val areHapticsEnabled = LocalHapticsEnabled.current

    return remember(areHapticsEnabled) {
        { hapticType ->
            if (areHapticsEnabled) {
                val milliseconds = when (hapticType) {
                    AppHaptic.Selection -> 5
                    AppHaptic.ImpactLight -> 10
                    AppHaptic.ImpactMedium, AppHaptic.Success -> 20
                    AppHaptic.ImpactHeavy, AppHaptic.Warning -> 30
                    AppHaptic.Error -> 40
                }
                vibrate(milliseconds)
            }
        }
    }
}

// Android browsers vibrate; iPhone browsers and computers ignore the call
@OptIn(ExperimentalWasmJsInterop::class)
private fun vibrate(milliseconds: Int): Unit =
    js("{ if (navigator.vibrate) { navigator.vibrate(milliseconds); } }")
