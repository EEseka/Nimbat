package com.eeseka.nimbat.settings.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.eeseka.nimbat.core.design_system.components.buttons.NimbatButton
import com.eeseka.nimbat.core.design_system.components.buttons.NimbatButtonStyle
import com.eeseka.nimbat.core.design_system.components.util.AppHaptic
import com.eeseka.nimbat.core.design_system.components.util.rememberAppHaptic
import com.eeseka.nimbat.core.presentation.util.DeviceConfiguration
import com.eeseka.nimbat.settings.presentation.SettingsAction
import com.eeseka.nimbat.settings.presentation.SettingsState
import nimbat.feature.settings.generated.resources.Res
import nimbat.feature.settings.generated.resources.clear_all_data
import nimbat.feature.settings.generated.resources.clearing_data
import org.jetbrains.compose.resources.stringResource

@Composable
fun SinglePaneSettings(
    state: SettingsState,
    configuration: DeviceConfiguration,
    canOpenAppSettings: Boolean,
    appVersion: String,
    topInset: Dp,
    bottomInset: Dp,
    onAction: (SettingsAction) -> Unit,
    onSourceCodeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .widthIn(max = 600.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = topInset, bottom = bottomInset, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        DeviceHeader(
            deviceName = state.deviceName,
            configuration = configuration,
            onRenameClick = { onAction(SettingsAction.OnRenameClick) }
        )

        SettingsPreferences(
            state = state,
            canOpenAppSettings = canOpenAppSettings,
            onAction = onAction,
            onSourceCodeClick = onSourceCodeClick
        )

        ClearDataButton(
            isClearingData = state.isClearingData,
            onClick = { onAction(SettingsAction.OnClearDataClick) }
        )

        SettingsFooter(
            appVersion = appVersion,
            isPlayingChirp = state.isPlayingChirp,
            onLogoClick = { onAction(SettingsAction.OnLogoClick) }
        )
    }
}

@Composable
fun TwoPaneSettings(
    state: SettingsState,
    configuration: DeviceConfiguration,
    canOpenAppSettings: Boolean,
    appVersion: String,
    topInset: Dp,
    bottomInset: Dp,
    onAction: (SettingsAction) -> Unit,
    onSourceCodeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(top = topInset, bottom = bottomInset),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))

            DeviceHeader(
                deviceName = state.deviceName,
                configuration = configuration,
                onRenameClick = { onAction(SettingsAction.OnRenameClick) }
            )

            Spacer(modifier = Modifier.weight(1f))

            ClearDataButton(
                isClearingData = state.isClearingData,
                onClick = { onAction(SettingsAction.OnClearDataClick) }
            )

            SettingsFooter(
                appVersion = appVersion,
                isPlayingChirp = state.isPlayingChirp,
                onLogoClick = { onAction(SettingsAction.OnLogoClick) }
            )
        }

        SettingsPreferences(
            state = state,
            canOpenAppSettings = canOpenAppSettings,
            onAction = onAction,
            onSourceCodeClick = onSourceCodeClick,
            modifier = Modifier
                .weight(1.4f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(top = topInset, bottom = bottomInset)
        )
    }
}

@Composable
private fun ClearDataButton(
    isClearingData: Boolean,
    onClick: () -> Unit
) {
    val hapticFeedback = rememberAppHaptic()

    NimbatButton(
        text = stringResource(Res.string.clear_all_data),
        loadingText = stringResource(Res.string.clearing_data),
        onClick = {
            hapticFeedback(AppHaptic.ImpactMedium)
            onClick()
        },
        style = NimbatButtonStyle.DESTRUCTIVE_SECONDARY,
        isLoading = isClearingData,
        leadingIcon = {
            Icon(
                imageVector = Lucide.Trash2,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        },
        modifier = Modifier.fillMaxWidth()
    )
}
