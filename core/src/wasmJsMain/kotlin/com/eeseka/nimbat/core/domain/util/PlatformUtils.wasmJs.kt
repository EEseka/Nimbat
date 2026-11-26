@file:OptIn(ExperimentalWasmJsInterop::class)

package com.eeseka.nimbat.core.domain.util

actual object PlatformUtils {
    actual fun isIOS(): Boolean = false
    actual fun isMacSafari(): Boolean {
        val agent = userAgent()
        val isSafari = "AppleWebKit" in agent && "Chrome/" !in agent
        return isSafari && "Macintosh" in agent && maxTouchPoints() == 0
    }
    actual fun hasHaptics(): Boolean = canVibrate() && maxTouchPoints() > 0
    actual fun isComputer(): Boolean = hasFinePointer()
}

private fun userAgent(): String = js("navigator.userAgent")

private fun canVibrate(): Boolean = js("typeof navigator.vibrate === 'function'")

private fun maxTouchPoints(): Int = js("navigator.maxTouchPoints || 0")

private fun hasFinePointer(): Boolean = js("window.matchMedia('(pointer: fine)').matches")
