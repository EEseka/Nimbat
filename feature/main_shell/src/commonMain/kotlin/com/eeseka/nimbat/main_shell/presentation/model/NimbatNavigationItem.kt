package com.eeseka.nimbat.main_shell.presentation.model

import com.eeseka.nimbat.home.presentation.navigation.HomeGraphRoutes
import com.eeseka.nimbat.main_shell.presentation.navigation.PlaceholderRoutes
import com.eeseka.nimbat.settings.presentation.navigation.SettingsGraphRoutes

enum class NimbatNavigationItem(val route: Any) {
    HOME(route = HomeGraphRoutes.Graph),
    LAB(route = PlaceholderRoutes.Lab),
    SETTINGS(route = SettingsGraphRoutes.Graph)
}
