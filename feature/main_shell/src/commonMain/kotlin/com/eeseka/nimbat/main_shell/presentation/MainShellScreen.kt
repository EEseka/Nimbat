package com.eeseka.nimbat.main_shell.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.eeseka.nimbat.core.design_system.components.layouts.NimbatScaffold
import com.eeseka.nimbat.core.design_system.components.util.AppHaptic
import com.eeseka.nimbat.core.design_system.components.util.rememberAppHaptic
import com.eeseka.nimbat.core.domain.util.PlatformUtils.isIOS
import com.eeseka.nimbat.core.presentation.util.currentDeviceConfiguration
import com.eeseka.nimbat.home.presentation.navigation.HomeGraphRoutes
import com.eeseka.nimbat.home.presentation.navigation.homeGraph
import com.eeseka.nimbat.main_shell.presentation.components.NimbatBottomBar
import com.eeseka.nimbat.main_shell.presentation.components.NimbatNavigationRail
import com.eeseka.nimbat.main_shell.presentation.components.PlaceholderDestination
import com.eeseka.nimbat.main_shell.presentation.model.NimbatNavigationItem
import com.eeseka.nimbat.main_shell.presentation.navigation.PlaceholderRoutes
import com.eeseka.nimbat.settings.presentation.navigation.settingsGraph

@Composable
fun MainShellScreen(
    navigateToOnboarding: () -> Unit
) {
    val innerNavController = rememberNavController()
    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val haptic = rememberAppHaptic()

    // iOS keeps its native tab bar at every size
    val showRail = currentDeviceConfiguration().isWideScreen && !isIOS()

    val isNavigationBarVisible = remember(currentDestination) {
        currentDestination?.hierarchy?.none {
            it.hasRoute(HomeGraphRoutes.Wave::class) || it.hasRoute(HomeGraphRoutes.FeaturePlaceholder::class)
        } ?: true
    }

    val selectedItem = remember(currentDestination) {
        NimbatNavigationItem.entries.firstOrNull { item ->
            currentDestination?.hierarchy?.any { it.hasRoute(item.route::class) } == true
        } ?: NimbatNavigationItem.HOME
    }

    val onNavigate: (NimbatNavigationItem) -> Unit = { item ->
        if (item != selectedItem) haptic(AppHaptic.Selection)

        val startDestinationId = innerNavController.graph.findStartDestination().id

        if (item == NimbatNavigationItem.HOME) {
            innerNavController.popBackStack(startDestinationId, inclusive = false, saveState = true)
        } else {
            innerNavController.navigate(item.route) {
                popUpTo(startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    NimbatScaffold(
        applyHorizontalInsets = false,
        bottomBar = {
            if (!showRail) {
                // Can't animate native iOS components with Compose
                AnimatedVisibility(
                    visible = isNavigationBarVisible,
                    enter = if (isIOS()) EnterTransition.None else slideInVertically { it } + fadeIn(),
                    exit = if (isIOS()) ExitTransition.None else slideOutVertically { it } + fadeOut()
                ) {
                    NimbatBottomBar(
                        selectedItem = selectedItem,
                        onItemSelected = onNavigate
                    )
                }
            }
        }
    ) { paddingValues ->
        if (showRail) {
            Row(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = isNavigationBarVisible,
                    enter = expandHorizontally() + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut()
                ) {
                    NimbatNavigationRail(
                        selectedItem = selectedItem,
                        onItemSelected = onNavigate
                    )
                }

                MainShellNavHost(
                    navController = innerNavController,
                    paddingValues = PaddingValues(0.dp),
                    navigateToOnboarding = navigateToOnboarding,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        } else {
            MainShellNavHost(
                navController = innerNavController,
                paddingValues = paddingValues,
                navigateToOnboarding = navigateToOnboarding,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun MainShellNavHost(
    navController: NavHostController,
    paddingValues: PaddingValues,
    navigateToOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NimbatNavigationItem.HOME.route,
        modifier = modifier,
        enterTransition = {
            if (isIOS()) {
                EnterTransition.None
            } else {
                val fadeThrough = tween<Float>(durationMillis = 210, delayMillis = 90)
                fadeIn(fadeThrough) + scaleIn(fadeThrough, initialScale = 0.92f)
            }
        },
        exitTransition = {
            if (isIOS()) ExitTransition.None else fadeOut(tween(durationMillis = 90))
        }
    ) {
        homeGraph(
            navController = navController,
            mainShellPadding = paddingValues
        )
        composable<PlaceholderRoutes.Lab> {
            PlaceholderDestination(item = NimbatNavigationItem.LAB, mainShellPadding = paddingValues)
        }
        settingsGraph(
            mainShellPadding = paddingValues,
            onNavigateToOnboarding = navigateToOnboarding
        )
    }
}
