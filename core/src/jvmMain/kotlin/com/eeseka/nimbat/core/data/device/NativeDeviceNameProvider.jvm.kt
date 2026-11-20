package com.eeseka.nimbat.core.data.device

import com.eeseka.nimbat.core.data.util.DesktopOs
import com.eeseka.nimbat.core.data.util.currentOs
import com.eeseka.nimbat.core.domain.device.DeviceNameProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

actual class NativeDeviceNameProvider : DeviceNameProvider {
    private companion object {
        val GENERATED_WINDOWS_NAME = Regex("^(DESKTOP|LAPTOP)-[A-Z0-9]+$")
    }

    actual override suspend fun deviceName(): String = withContext(Dispatchers.IO) {
        when (currentOs) {
            DesktopOs.MACOS -> macComputerName() ?: "Mac"
            DesktopOs.WINDOWS -> windowsComputerName() ?: "Windows PC"
            DesktopOs.LINUX -> linuxHostname() ?: "Linux PC"
        }
    }

    private fun macComputerName(): String? = try {
        val process = ProcessBuilder("scutil", "--get", "ComputerName").start()
        try {
            val hasFinished = process.waitFor(2, TimeUnit.SECONDS)
            if (hasFinished && process.exitValue() == 0) {
                process.inputStream.bufferedReader().readLine()?.trim()?.takeIf { it.isNotEmpty() }
            } else {
                null
            }
        } finally {
            process.destroy()
        }
    } catch (_: IOException) {
        null
    }

    private fun windowsComputerName(): String? =
        System.getenv("COMPUTERNAME")?.takeUnless { it.isBlank() || GENERATED_WINDOWS_NAME.matches(it) }

    private fun linuxHostname(): String? = try {
        File("/etc/hostname").readText().trim().takeIf { it.isNotEmpty() }
    } catch (_: IOException) {
        System.getenv("HOSTNAME")?.takeIf { it.isNotBlank() }
    }
}
