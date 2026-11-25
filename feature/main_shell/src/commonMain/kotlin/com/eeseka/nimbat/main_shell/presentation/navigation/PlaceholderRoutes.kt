package com.eeseka.nimbat.main_shell.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface PlaceholderRoutes {
    @Serializable
    data object Lab : PlaceholderRoutes
}
