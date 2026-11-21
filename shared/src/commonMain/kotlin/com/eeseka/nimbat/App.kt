package com.eeseka.nimbat

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.eeseka.nimbat.core.design_system.components.util.LocalHapticsEnabled
import com.eeseka.nimbat.core.design_system.theme.ApplyNativeTheme
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.core.domain.settings.AppTheme
import com.eeseka.nimbat.navigation.MainShellGraphRoutes
import com.eeseka.nimbat.navigation.NavigationRoot
import com.eeseka.nimbat.onboarding.presentation.navigation.OnboardingGraphRoutes
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(
    onOnboardingChecked: () -> Unit = {},
    viewModel: MainViewModel = koinViewModel()
) {
    val navController = rememberNavController()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val isDarkTheme = when (state.theme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    LaunchedEffect(state.isCheckingOnboarding) {
        if (!state.isCheckingOnboarding) {
            onOnboardingChecked()
        }
    }

    ApplyNativeTheme(theme = state.theme)

    NimbatTheme(darkTheme = isDarkTheme, pureBlack = state.isPureBlackEnabled) {
        CompositionLocalProvider(LocalHapticsEnabled provides state.areHapticsEnabled) {
            if (!state.isCheckingOnboarding) {
                val startDestination = if (state.hasSeenOnboarding) {
                    MainShellGraphRoutes.Graph
                } else {
                    OnboardingGraphRoutes.Graph
                }

                NavigationRoot(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }
}
