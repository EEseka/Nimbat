package com.eeseka.nimbat.core.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinBase
import com.sun.jna.ptr.IntByReference

private const val ASSERTION_LEVEL_ON = 255
private const val AWAKE_REASON = "Wave is listening for your hand"

@Composable
actual fun KeepScreenOnEffect() {
    DisposableEffect(Unit) {
        val letSleep = keepDisplayAwake()
        onDispose { letSleep() }
    }
}

private fun keepDisplayAwake(): () -> Unit {
    val os = System.getProperty("os.name").lowercase()
    return runCatching {
        when {
            "mac" in os -> keepMacAwake()
            "win" in os -> keepWindowsAwake()
            else -> keepLinuxAwake()
        }
    }.getOrDefault {}
}

private fun keepMacAwake(): () -> Unit {
    val type = CFStringRef.createCFString("PreventUserIdleDisplaySleep")
    val reason = CFStringRef.createCFString(AWAKE_REASON)
    val assertionId = IntByReference()
    val result = MacPower.INSTANCE.IOPMAssertionCreateWithName(type, ASSERTION_LEVEL_ON, reason, assertionId)
    type.release()
    reason.release()
    if (result != 0) return {}
    return { MacPower.INSTANCE.IOPMAssertionRelease(assertionId.value) }
}

private fun keepWindowsAwake(): () -> Unit {
    Kernel32.INSTANCE.SetThreadExecutionState(WinBase.ES_CONTINUOUS or WinBase.ES_DISPLAY_REQUIRED)
    return { Kernel32.INSTANCE.SetThreadExecutionState(WinBase.ES_CONTINUOUS) }
}

/** Desktops differ on Linux; systemd's idle lock is the closest shared one. It also ends if Nimbat does. */
private fun keepLinuxAwake(): () -> Unit {
    val pid = ProcessHandle.current().pid()
    val inhibitor = ProcessBuilder(
        "systemd-inhibit", "--what=idle", "--who=Nimbat", "--why=$AWAKE_REASON",
        "tail", "--pid=$pid", "-f", "/dev/null"
    )
        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .start()
    return { inhibitor.destroy() }
}

@Suppress("FunctionName")
private interface MacPower : Library {
    companion object {
        val INSTANCE: MacPower = Native.load("IOKit", MacPower::class.java)
    }

    fun IOPMAssertionCreateWithName(type: CFStringRef, level: Int, name: CFStringRef, id: IntByReference): Int
    fun IOPMAssertionRelease(id: Int): Int
}
