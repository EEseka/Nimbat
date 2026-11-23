package com.eeseka.nimbat.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.eeseka.nimbat.main_shell.presentation.navigation.MainShellGraphRoutes
import com.eeseka.nimbat.main_shell.presentation.navigation.mainShellGraph
import com.eeseka.nimbat.onboarding.presentation.navigation.OnboardingGraphRoutes
import com.eeseka.nimbat.onboarding.presentation.navigation.onboardingGraph

@Composable
fun NavigationRoot(
    navController: NavHostController,
    startDestination: Any
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        onboardingGraph(
            onNavigateToMain = {
                navController.navigate(MainShellGraphRoutes.Graph) {
                    popUpTo(OnboardingGraphRoutes.Graph) { inclusive = true }
                }
            }
        )

        mainShellGraph(
            onNavigateToOnboarding = {
                navController.navigate(OnboardingGraphRoutes.Graph) {
                    popUpTo(MainShellGraphRoutes.Graph) { inclusive = true }
                }
            }
        )
    }
}
