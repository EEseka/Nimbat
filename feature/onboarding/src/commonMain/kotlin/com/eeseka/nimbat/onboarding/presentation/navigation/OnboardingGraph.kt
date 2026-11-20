package com.eeseka.nimbat.onboarding.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.eeseka.nimbat.onboarding.presentation.OnboardingRoot

fun NavGraphBuilder.onboardingGraph(
    onNavigateToMain: () -> Unit
) {
    navigation<OnboardingGraphRoutes.Graph>(
        startDestination = OnboardingGraphRoutes.Welcome
    ) {
        composable<OnboardingGraphRoutes.Welcome> {
            OnboardingRoot(navigateToMain = onNavigateToMain)
        }
    }
}
