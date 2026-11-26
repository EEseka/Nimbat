package com.eeseka.nimbat.core.domain.util

actual object PlatformUtils {
    actual fun isIOS(): Boolean = false
    actual fun isMacSafari(): Boolean = false
    actual fun hasHaptics(): Boolean = true
    actual fun isComputer(): Boolean = false
}
