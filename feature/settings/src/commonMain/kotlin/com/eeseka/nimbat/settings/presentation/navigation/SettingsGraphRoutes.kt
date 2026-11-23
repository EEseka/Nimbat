package com.eeseka.nimbat.settings.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface SettingsGraphRoutes {
    @Serializable
    data object Graph : SettingsGraphRoutes

    @Serializable
    data object Settings : SettingsGraphRoutes
}
