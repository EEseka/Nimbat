package com.eeseka.nimbat.windows

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.Window
import com.eeseka.nimbat.App
import nimbat.desktopapp.generated.resources.Res
import nimbat.desktopapp.generated.resources.nimbat_logo
import org.jetbrains.compose.resources.painterResource
import java.awt.Desktop
import java.awt.Frame

@Composable
fun NimbatWindow(
    isVisible: Boolean,
    openRequests: Int,
    onCloseRequest: () -> Unit
) {
    Window(
        onCloseRequest = onCloseRequest,
        visible = isVisible,
        title = "Nimbat",
        icon = painterResource(Res.drawable.nimbat_logo)
    ) {
        LaunchedEffect(openRequests) {
            if (openRequests > 0) window.bringToFront()
        }

        App()
    }
}

private fun ComposeWindow.bringToFront() {
    extendedState = Frame.NORMAL
    if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop()
            .takeIf { it.isSupported(Desktop.Action.APP_REQUEST_FOREGROUND) }
            ?.requestForeground(true)
    }
    toFront()
    requestFocus()
}
