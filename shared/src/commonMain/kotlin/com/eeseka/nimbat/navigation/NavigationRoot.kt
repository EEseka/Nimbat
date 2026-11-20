package com.eeseka.nimbat.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.eeseka.nimbat.onboarding.presentation.navigation.OnboardingGraphRoutes
import com.eeseka.nimbat.onboarding.presentation.navigation.onboardingGraph
import com.eeseka.nimbat.sound_check.SoundCheckRoot

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

        navigation<MainShellGraphRoutes.Graph>(
            startDestination = MainShellGraphRoutes.SoundCheck
        ) {
            composable<MainShellGraphRoutes.SoundCheck> {
                SoundCheckRoot()
            }
        }
    }
}
