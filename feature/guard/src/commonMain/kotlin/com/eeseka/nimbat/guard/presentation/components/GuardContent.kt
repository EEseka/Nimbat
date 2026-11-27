package com.eeseka.nimbat.guard.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.components.buttons.NimbatButton
import com.eeseka.nimbat.core.design_system.components.util.AppHaptic
import com.eeseka.nimbat.core.design_system.components.util.rememberAppHaptic
import com.eeseka.nimbat.core.domain.guard.model.GuardMode
import com.eeseka.nimbat.core.domain.guard.model.GuardPhase
import com.eeseka.nimbat.core.presentation.permissions.PermissionState
import com.eeseka.nimbat.core.presentation.util.DeviceConfiguration
import com.eeseka.nimbat.guard.presentation.GuardAction
import com.eeseka.nimbat.guard.presentation.GuardState
import nimbat.feature.guard.generated.resources.Res
import nimbat.feature.guard.generated.resources.start
import nimbat.feature.guard.generated.resources.try_again
import org.jetbrains.compose.resources.stringResource

@Composable
fun SinglePaneGuard(
    state: GuardState,
    configuration: DeviceConfiguration,
    canOpenAppSettings: Boolean,
    isScreenUpright: Boolean,
    isWeb: Boolean,
    topInset: Dp,
    bottomInset: Dp,
    onAction: (GuardAction) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .widthIn(max = 600.dp)
            .fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = topInset, end = 16.dp, bottom = bottomInset)
    ) {
        item(key = "setup") {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                GuardModePicker(mode = state.mode, isScreenUpright = isScreenUpright, onAction = onAction)
                GuardHero(
                    mode = state.mode,
                    configuration = configuration,
                    isScreenUpright = isScreenUpright,
                    isWeb = isWeb,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(416.dp)
                )
                GuardControls(state = state, canOpenAppSettings = canOpenAppSettings, onAction = onAction)
            }
        }
        guardLog(state = state, onAction = onAction, topPadding = 40.dp)
    }
}

@Composable
fun TwoPaneGuard(
    state: GuardState,
    configuration: DeviceConfiguration,
    canOpenAppSettings: Boolean,
    isScreenUpright: Boolean,
    isWeb: Boolean,
    topInset: Dp,
    bottomInset: Dp,
    onAction: (GuardAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 24.dp, top = topInset, end = 24.dp, bottom = bottomInset),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GuardModePicker(mode = state.mode, isScreenUpright = isScreenUpright, onAction = onAction)
            GuardHero(
                mode = state.mode,
                configuration = configuration,
                isScreenUpright = isScreenUpright,
                isWeb = isWeb,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
            GuardControls(state = state, canOpenAppSettings = canOpenAppSettings, onAction = onAction)
        }

        LazyColumn(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            contentPadding = PaddingValues(start = 24.dp, top = topInset, end = 24.dp, bottom = bottomInset)
        ) {
            guardLog(state = state, onAction = onAction, topPadding = 0.dp)
        }
    }
}

@Composable
private fun GuardControls(
    state: GuardState,
    canOpenAppSettings: Boolean,
    onAction: (GuardAction) -> Unit
) {
    val hapticFeedback = rememberAppHaptic()

    if (state.mode == GuardMode.CURTAIN && state.canLockComputer) {
        LockComputerRow(isOn = state.locksComputer, onAction = onAction)
    }

    GuardProblemMessage(state = state, canOpenAppSettings = canOpenAppSettings, onAction = onAction)

    val permission = state.microphonePermission
    if (permission == null || permission == PermissionState.GRANTED) {
        NimbatButton(
            text = stringResource(if (state.phase == GuardPhase.FAILED) Res.string.try_again else Res.string.start),
            onClick = {
                hapticFeedback(AppHaptic.ImpactMedium)
                onAction(GuardAction.OnStartClick)
            },
            enabled = permission == PermissionState.GRANTED
        )
    }
}

private fun LazyListScope.guardLog(
    state: GuardState,
    onAction: (GuardAction) -> Unit,
    topPadding: Dp
) {
    item(key = "log_header") {
        GuardLogHeader(
            hasSessions = state.sessions.isNotEmpty(),
            onClearClick = { onAction(GuardAction.OnClearLogClick) },
            modifier = Modifier.padding(top = topPadding, bottom = 8.dp)
        )
    }
    if (state.sessions.isEmpty()) {
        item(key = "log_empty") {
            GuardLogEmpty(modifier = Modifier.fillMaxWidth())
        }
    }
    items(items = state.sessions, key = { it.id }) { session ->
        GuardSessionCard(
            session = session,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .animateItem()
        )
    }
}
