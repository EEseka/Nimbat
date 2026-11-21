package com.eeseka.nimbat.core.data.util

import java.io.File

val appDataDirectory: File
    get() {
        val userHome = System.getProperty("user.home")
        return when (currentOs) {
            DesktopOs.WINDOWS -> File(System.getenv("APPDATA"), "Nimbat")
            DesktopOs.MACOS -> File(userHome, "Library/Application Support/Nimbat")
            DesktopOs.LINUX -> File(userHome, ".local/share/Nimbat")
        }
    }
