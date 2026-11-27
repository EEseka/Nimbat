package com.eeseka.nimbat.windows

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.eeseka.nimbat.guard.presentation.components.GuardCurtain
import java.awt.GraphicsEnvironment

@Composable
fun CurtainWindows(onLift: () -> Unit) {
    val screens = remember {
        GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.map { it.defaultConfiguration.bounds }
    }

    screens.forEachIndexed { index, bounds ->
        key(index) {
            Window(
                onCloseRequest = onLift,
                state = rememberWindowState(
                    position = WindowPosition(bounds.x.dp, bounds.y.dp),
                    size = DpSize(bounds.width.dp, bounds.height.dp)
                ),
                undecorated = true,
                resizable = false,
                alwaysOnTop = true,
                onKeyEvent = { event ->
                    if (event.type == KeyEventType.KeyDown) onLift()
                    true
                }
            ) {
                GuardCurtain(onLift = onLift)
            }
        }
    }
}
