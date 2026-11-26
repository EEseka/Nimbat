@file:OptIn(ExperimentalWasmJsInterop::class)

package com.eeseka.nimbat.core.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

@Composable
actual fun KeepScreenOnEffect() {
    DisposableEffect(Unit) {
        val wakeLock = holdScreenWakeLock()
        onDispose { letScreenSleep(wakeLock) }
    }
}

private fun holdScreenWakeLock(): JsAny = js(
    """(() => {
        const holder = { lock: null, isHeld: true };
        holder.request = () => {
            if (!holder.isHeld || document.visibilityState !== 'visible' || !navigator.wakeLock) return;
            navigator.wakeLock.request('screen')
                .then((lock) => { if (holder.isHeld) holder.lock = lock; else lock.release(); })
                .catch(() => {});
        };
        document.addEventListener('visibilitychange', holder.request);
        holder.request();
        return holder;
    })()"""
)

private fun letScreenSleep(holder: JsAny): Unit = js(
    """{
        holder.isHeld = false;
        document.removeEventListener('visibilitychange', holder.request);
        if (holder.lock) holder.lock.release();
    }"""
)
