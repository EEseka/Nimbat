package com.eeseka.nimbat

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eeseka.nimbat.core.design_system.components.buttons.NimbatButton
import com.eeseka.nimbat.core.design_system.components.buttons.NimbatButtonStyle
import com.eeseka.nimbat.core.design_system.components.layouts.NimbatCard
import com.eeseka.nimbat.core.design_system.components.layouts.NimbatScaffold
import com.eeseka.nimbat.core.design_system.components.modals_and_overlays.NimbatDialog
import com.eeseka.nimbat.core.design_system.components.modals_and_overlays.NimbatFlashType
import com.eeseka.nimbat.core.design_system.components.modals_and_overlays.showFlashMessage
import com.eeseka.nimbat.core.design_system.components.progress_indicator.NimbatProgressIndicator
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatTextField
import com.eeseka.nimbat.core.design_system.components.toggles_and_control.NimbatSlider
import com.eeseka.nimbat.core.design_system.components.toggles_and_control.NimbatSwitch
import com.eeseka.nimbat.core.design_system.components.util.AppHaptic
import com.eeseka.nimbat.core.design_system.components.util.rememberAppHaptic
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme

@Composable
fun App() {
    val isSystemDark = isSystemInDarkTheme()
    var isDarkTheme by remember { mutableStateOf(isSystemDark) }
    var isLightsOut by remember { mutableStateOf(false) }

    NimbatTheme(darkTheme = isDarkTheme, pureBlack = isLightsOut) {
        DesignSystemPreviewScreen(
            isDarkTheme = isDarkTheme,
            onDarkThemeChange = { isDarkTheme = it },
            isLightsOut = isLightsOut,
            onLightsOutChange = { isLightsOut = it }
        )
    }
}

// Temporary screen that shows the design system working; replaced once the real screens exist
@Composable
private fun DesignSystemPreviewScreen(
    isDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    isLightsOut: Boolean,
    onLightsOutChange: (Boolean) -> Unit
) {
    val haptic = rememberAppHaptic()
    val snackbarHostState = remember { SnackbarHostState() }
    val deviceName = remember { TextFieldState("Living room laptop") }
    var volume by remember { mutableFloatStateOf(0.6f) }
    var isMeasuring by remember { mutableStateOf(false) }
    var isDialogVisible by remember { mutableStateOf(false) }
    var flashRequests by remember { mutableIntStateOf(0) }

    LaunchedEffect(flashRequests) {
        if (flashRequests > 0) {
            snackbarHostState.showFlashMessage(
                message = "Measurement saved",
                type = NimbatFlashType.Success
            )
        }
    }

    NimbatScaffold(
        snackbarHostState = snackbarHostState
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                )
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NimbatCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NimbatText(
                            text = "Sonar for your devices",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        NimbatText(
                            text = "Phone, laptop and browser sense each other with sound too high to hear.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NimbatText(
                                text = "1.48 m",
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (isMeasuring) {
                                NimbatProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }

                NimbatCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingRow(
                            title = "Dark mode",
                            checked = isDarkTheme,
                            onCheckedChange = {
                                haptic(AppHaptic.Selection)
                                onDarkThemeChange(it)
                            }
                        )
                        SettingRow(
                            title = "Lights Out",
                            checked = isLightsOut,
                            enabled = isDarkTheme,
                            onCheckedChange = {
                                haptic(AppHaptic.Selection)
                                onLightsOutChange(it)
                            }
                        )
                        NimbatText(
                            text = "Volume",
                            style = MaterialTheme.typography.titleSmall
                        )
                        NimbatSlider(
                            value = volume,
                            onValueChange = { volume = it }
                        )
                    }
                }

                NimbatTextField(
                    state = deviceName,
                    label = "Device name",
                    helperText = "Other devices see this name"
                )

                NimbatButton(
                    text = "Save measurement",
                    onClick = {
                        haptic(AppHaptic.ImpactMedium)
                        flashRequests++
                    }
                )
                NimbatButton(
                    text = if (isMeasuring) "Stop" else "Start measuring",
                    style = NimbatButtonStyle.SECONDARY,
                    onClick = {
                        haptic(AppHaptic.ImpactLight)
                        isMeasuring = !isMeasuring
                    }
                )
                NimbatButton(
                    text = "Delete measurement",
                    style = NimbatButtonStyle.DESTRUCTIVE_SECONDARY,
                    onClick = {
                        haptic(AppHaptic.ImpactLight)
                        isDialogVisible = true
                    }
                )
            }
        }
    }

    if (isDialogVisible) {
        NimbatDialog(
            onDismissRequest = { isDialogVisible = false },
            title = "Delete this measurement?",
            message = "It will be removed from your saved list. This can't be undone.",
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {}
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NimbatText(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f)
        )
        NimbatSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
