package com.eeseka.nimbat

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.sound_check.SoundCheckRoot

@Composable
fun App() {
    NimbatTheme(darkTheme = isSystemInDarkTheme()) {
        SoundCheckRoot()
    }
}
