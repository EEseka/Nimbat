package com.eeseka.nimbat.core.data.guard

import com.eeseka.nimbat.core.data.util.DesktopOs
import com.eeseka.nimbat.core.data.util.currentOs
import com.eeseka.nimbat.core.domain.guard.ComputerLock
import com.sun.jna.platform.win32.User32
import java.io.IOException

actual class NativeComputerLock : ComputerLock {
    actual override val canLock: Boolean = true

    actual override fun lock() {
        when (currentOs) {
            DesktopOs.WINDOWS -> User32.INSTANCE.LockWorkStation()
            DesktopOs.MACOS -> run("pmset", "displaysleepnow") // macOS has no public lock call; display sleep locks when the Mac asks for a password after sleep
            DesktopOs.LINUX -> run("loginctl", "lock-session")
        }
    }

    private fun run(vararg command: String) {
        try {
            ProcessBuilder(*command)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
        } catch (_: IOException) {
            // The curtain still covers the screen
        }
    }
}
