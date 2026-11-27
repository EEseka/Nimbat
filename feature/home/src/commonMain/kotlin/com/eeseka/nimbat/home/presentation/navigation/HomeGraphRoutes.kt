package com.eeseka.nimbat.home.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface HomeGraphRoutes {
    @Serializable
    data object Graph : HomeGraphRoutes

    @Serializable
    data object Home : HomeGraphRoutes

    @Serializable
    data object Wave : HomeGraphRoutes

    @Serializable
    data object Guard : HomeGraphRoutes

    @Serializable
    data class FeaturePlaceholder(val featureName: String) : HomeGraphRoutes
}
