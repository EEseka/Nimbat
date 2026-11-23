package com.eeseka.nimbat.main_shell.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.eeseka.nimbat.main_shell.presentation.MainShellScreen

fun NavGraphBuilder.mainShellGraph(
    onNavigateToOnboarding: () -> Unit
) {
    navigation<MainShellGraphRoutes.Graph>(
        startDestination = MainShellGraphRoutes.MainShell
    ) {
        composable<MainShellGraphRoutes.MainShell> {
            MainShellScreen(navigateToOnboarding = onNavigateToOnboarding)
        }
    }
}
