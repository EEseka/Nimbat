package com.eeseka.nimbat

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Tray
import com.eeseka.nimbat.core.data.util.DesktopOs
import com.eeseka.nimbat.core.data.util.currentOs
import com.eeseka.nimbat.core.domain.guard.model.GuardPhase
import nimbat.desktopapp.generated.resources.Res
import nimbat.desktopapp.generated.resources.guard_alarm_ringing
import nimbat.desktopapp.generated.resources.guard_counting_down
import nimbat.desktopapp.generated.resources.guard_curtain_down
import nimbat.desktopapp.generated.resources.guard_stopped
import nimbat.desktopapp.generated.resources.guard_watching
import nimbat.desktopapp.generated.resources.nimbat
import nimbat.desktopapp.generated.resources.nimbat_logo
import nimbat.desktopapp.generated.resources.nimbat_tray
import nimbat.desktopapp.generated.resources.open_nimbat
import nimbat.desktopapp.generated.resources.quit_nimbat
import nimbat.desktopapp.generated.resources.stop_guard
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ApplicationScope.NimbatTray(
    guardPhase: GuardPhase,
    onOpenClick: () -> Unit,
    onStopGuardClick: () -> Unit,
    onQuitClick: () -> Unit
) {
    val status = stringResource(
        when (guardPhase) {
            GuardPhase.COUNTDOWN, GuardPhase.CALIBRATING -> Res.string.guard_counting_down
            GuardPhase.WATCHING -> Res.string.guard_watching
            GuardPhase.RINGING -> Res.string.guard_alarm_ringing
            GuardPhase.CURTAIN -> Res.string.guard_curtain_down
            GuardPhase.IDLE, GuardPhase.FAILED -> Res.string.guard_stopped
        }
    )
    val openText = stringResource(Res.string.open_nimbat)
    val stopText = stringResource(Res.string.stop_guard)
    val quitText = stringResource(Res.string.quit_nimbat)

    val icon = if (currentOs == DesktopOs.MACOS) Res.drawable.nimbat_tray else Res.drawable.nimbat_logo

    Tray(
        icon = painterResource(icon),
        tooltip = stringResource(Res.string.nimbat),
        onAction = onOpenClick
    ) {
        Item(text = status, enabled = false, onClick = {})
        Separator()
        Item(text = openText, onClick = onOpenClick)
        Item(text = stopText, enabled = guardPhase.isRunning, onClick = onStopGuardClick)
        Separator()
        Item(text = quitText, onClick = onQuitClick)
    }
}
