package com.eeseka.nimbat.core.domain.util

expect object PlatformUtils {
    fun isIOS(): Boolean
    fun isMacSafari(): Boolean
    fun hasHaptics(): Boolean
}
