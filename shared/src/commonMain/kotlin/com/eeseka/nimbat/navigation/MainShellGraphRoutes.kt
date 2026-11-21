package com.eeseka.nimbat.navigation

import kotlinx.serialization.Serializable

sealed interface MainShellGraphRoutes {
    @Serializable
    data object Graph : MainShellGraphRoutes

    @Serializable
    data object SoundCheck : MainShellGraphRoutes
}
