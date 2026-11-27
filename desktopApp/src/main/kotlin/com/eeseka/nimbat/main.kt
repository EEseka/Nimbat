package com.eeseka.nimbat

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.application
import com.eeseka.nimbat.core.domain.guard.model.GuardPhase
import com.eeseka.nimbat.di.desktopModule
import com.eeseka.nimbat.di.initKoin
import com.eeseka.nimbat.windows.CurtainWindows
import com.eeseka.nimbat.windows.NimbatWindow
import org.koin.compose.koinInject

fun main() {
    System.setProperty("apple.awt.enableTemplateImages", "true") // Lets the Mac menu bar tint the tray icon for light and dark

    initKoin {
        modules(desktopModule)
    }

    application {
        val applicationStateHolder = koinInject<ApplicationStateHolder>()
        val applicationState by applicationStateHolder.state.collectAsState()

        LaunchedEffect(applicationState.isOpen) {
            if (!applicationState.isOpen) {
                exitApplication()
            }
        }

        NimbatWindow(
            isVisible = applicationState.isWindowVisible,
            openRequests = applicationState.openRequests,
            onCloseRequest = applicationStateHolder::onWindowCloseRequest
        )

        if (applicationState.guardPhase.isRunning || !applicationState.isWindowVisible) {
            NimbatTray(
                guardPhase = applicationState.guardPhase,
                onOpenClick = applicationStateHolder::onOpenClick,
                onStopGuardClick = applicationStateHolder::onStopGuardClick,
                onQuitClick = ::exitApplication
            )
        }

        if (applicationState.guardPhase == GuardPhase.CURTAIN) {
            CurtainWindows(onLift = applicationStateHolder::onCurtainLift)
        }
    }
}
