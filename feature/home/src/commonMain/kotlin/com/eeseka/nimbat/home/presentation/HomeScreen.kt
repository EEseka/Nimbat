package com.eeseka.nimbat.home.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eeseka.nimbat.core.design_system.components.layouts.NimbatScaffold
import com.eeseka.nimbat.core.design_system.components.modals_and_overlays.NimbatFlashType
import com.eeseka.nimbat.core.design_system.components.modals_and_overlays.showFlashMessage
import com.eeseka.nimbat.core.design_system.components.navigation.NimbatTopAppBar
import com.eeseka.nimbat.core.design_system.components.util.AppHaptic
import com.eeseka.nimbat.core.design_system.components.util.rememberAppHaptic
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.core.presentation.util.ObserveAsEvents
import com.eeseka.nimbat.core.presentation.util.currentDeviceConfiguration
import com.eeseka.nimbat.home.presentation.components.HomeContent
import com.eeseka.nimbat.home.presentation.model.HomeFeature
import nimbat.feature.home.generated.resources.Res
import nimbat.feature.home.generated.resources.home
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeRoot(
    mainShellPadding: PaddingValues,
    navigateToFeature: (HomeFeature) -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val hapticFeedback = rememberAppHaptic()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is HomeEvent.SoundCheckFinished -> {
                hapticFeedback(if (event.isHeard) AppHaptic.Success else AppHaptic.Error)
            }

            is HomeEvent.Error -> {
                snackbarHostState.showFlashMessage(
                    message = event.message.asStringAsync(),
                    type = NimbatFlashType.Error
                )
            }
        }
    }

    HomeScreen(
        state = state,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
        onFeatureClick = navigateToFeature,
        mainShellPadding = mainShellPadding
    )
}

@Composable
fun HomeScreen(
    state: HomeState,
    onAction: (HomeAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    onFeatureClick: (HomeFeature) -> Unit,
    mainShellPadding: PaddingValues
) {
    val config = currentDeviceConfiguration()

    NimbatScaffold(
        snackbarHostState = snackbarHostState,
        topBar = {
            NimbatTopAppBar(title = stringResource(Res.string.home))
        }
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            HomeContent(
                state = state,
                configuration = config,
                topInset = scaffoldPadding.calculateTopPadding() + 16.dp,
                bottomInset = mainShellPadding.calculateBottomPadding() + 24.dp,
                onAction = onAction,
                onFeatureClick = onFeatureClick
            )
        }
    }
}

@PreviewLightDark
@Preview(name = "Tablet", widthDp = 800, heightDp = 1100)
@Preview(name = "Desktop", widthDp = 1280, heightDp = 800)
@Composable
private fun HomeScreenPreview() {
    NimbatTheme {
        HomeScreen(
            state = HomeState(isSoundCheckPassed = false),
            onAction = {},
            snackbarHostState = remember { SnackbarHostState() },
            onFeatureClick = {},
            mainShellPadding = PaddingValues(0.dp)
        )
    }
}
