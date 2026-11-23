package com.eeseka.nimbat.settings.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.eeseka.nimbat.settings.presentation.SettingsRoot

fun NavGraphBuilder.settingsGraph(
    mainShellPadding: PaddingValues,
    onNavigateToOnboarding: () -> Unit
) {
    navigation<SettingsGraphRoutes.Graph>(
        startDestination = SettingsGraphRoutes.Settings
    ) {
        composable<SettingsGraphRoutes.Settings> {
            SettingsRoot(
                mainShellPadding = mainShellPadding,
                navigateToOnboarding = onNavigateToOnboarding
            )
        }
    }
}
