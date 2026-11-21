@file:OptIn(ExperimentalWasmJsInterop::class)

package com.eeseka.nimbat.core.data.device

import com.eeseka.nimbat.core.domain.device.DeviceNameProvider

actual class NativeDeviceNameProvider : DeviceNameProvider {

    actual override suspend fun deviceName(): String {
        val agent = userAgent()
        val browser = when {
            "Edg" in agent -> "Edge"
            "OPR/" in agent -> "Opera"
            "Firefox/" in agent || "FxiOS/" in agent -> "Firefox"
            "Chrome/" in agent || "CriOS/" in agent -> "Chrome"
            "Safari/" in agent -> "Safari"
            else -> "Browser"
        }
        val system = when {
            "iPhone" in agent -> "iPhone"
            "iPad" in agent -> "iPad"
            "Android" in agent -> "Android"
            "CrOS" in agent -> "Chromebook"
            "Macintosh" in agent -> "Mac"
            "Windows" in agent -> "Windows"
            "Linux" in agent -> "Linux"
            else -> null
        }
        return if (system != null) "$browser on $system" else browser
    }
}

private fun userAgent(): String = js("navigator.userAgent")
