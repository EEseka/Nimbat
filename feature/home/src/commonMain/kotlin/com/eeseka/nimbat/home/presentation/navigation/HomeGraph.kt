package com.eeseka.nimbat.home.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.eeseka.nimbat.home.presentation.HomeRoot
import com.eeseka.nimbat.home.presentation.components.FeaturePlaceholder
import com.eeseka.nimbat.home.presentation.model.HomeFeature
import com.eeseka.nimbat.wave.presentation.WaveRoot

fun NavGraphBuilder.homeGraph(
    navController: NavController,
    mainShellPadding: PaddingValues
) {
    navigation<HomeGraphRoutes.Graph>(
        startDestination = HomeGraphRoutes.Home
    ) {
        composable<HomeGraphRoutes.Home> {
            HomeRoot(
                mainShellPadding = mainShellPadding,
                navigateToFeature = { feature ->
                    when (feature) {
                        HomeFeature.WAVE -> navController.navigate(HomeGraphRoutes.Wave)
                        else -> navController.navigate(HomeGraphRoutes.FeaturePlaceholder(featureName = feature.name))
                    }
                }
            )
        }

        composable<HomeGraphRoutes.Wave> {
            WaveRoot(
                navigateBack = { navController.navigateUp() }
            )
        }

        composable<HomeGraphRoutes.FeaturePlaceholder> { backStackEntry ->
            val route: HomeGraphRoutes.FeaturePlaceholder = backStackEntry.toRoute()
            FeaturePlaceholder(
                feature = HomeFeature.valueOf(route.featureName),
                mainShellPadding = mainShellPadding,
                onBackClick = { navController.navigateUp() }
            )
        }
    }
}
