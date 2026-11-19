package com.eeseka.nimbat

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.eeseka.nimbat.di.initKoin
import nimbat.desktopapp.generated.resources.Res
import nimbat.desktopapp.generated.resources.nimbat_logo
import org.jetbrains.compose.resources.painterResource

fun main() {
    initKoin()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Nimbat",
            icon = painterResource(Res.drawable.nimbat_logo)
        ) {
            App()
        }
    }
}
