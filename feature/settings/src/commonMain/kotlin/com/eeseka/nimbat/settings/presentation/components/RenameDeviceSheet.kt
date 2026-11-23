package com.eeseka.nimbat.settings.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Smartphone
import com.eeseka.nimbat.core.design_system.components.buttons.NimbatButton
import com.eeseka.nimbat.core.design_system.components.modals_and_overlays.NimbatAdaptiveSheet
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.components.util.AppHaptic
import com.eeseka.nimbat.core.design_system.components.util.rememberAppHaptic
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.core.presentation.device.components.DeviceNameField
import com.eeseka.nimbat.core.presentation.util.clearFocusOnTap
import nimbat.feature.settings.generated.resources.Res
import nimbat.feature.settings.generated.resources.rename_device
import nimbat.feature.settings.generated.resources.rename_device_message
import nimbat.feature.settings.generated.resources.save
import org.jetbrains.compose.resources.stringResource

@Composable
fun RenameDeviceSheet(
    nameState: TextFieldState,
    deviceIcon: ImageVector,
    errorMessage: String?,
    isSaving: Boolean,
    onSaveClick: () -> Unit,
    onDismissRequest: () -> Unit
) {
    NimbatAdaptiveSheet(onDismissRequest = onDismissRequest) {
        RenameDeviceSheetContent(
            nameState = nameState,
            deviceIcon = deviceIcon,
            errorMessage = errorMessage,
            isSaving = isSaving,
            onSaveClick = onSaveClick
        )
    }
}

@Composable
private fun RenameDeviceSheetContent(
    nameState: TextFieldState,
    deviceIcon: ImageVector,
    errorMessage: String?,
    isSaving: Boolean,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = rememberAppHaptic()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearFocusOnTap()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        NimbatText(
            text = stringResource(Res.string.rename_device),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )

        NimbatText(
            text = stringResource(Res.string.rename_device_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        DeviceNameField(
            state = nameState,
            deviceIcon = deviceIcon,
            errorMessage = errorMessage
        )

        Spacer(modifier = Modifier.height(24.dp))

        NimbatButton(
            text = stringResource(Res.string.save),
            onClick = {
                hapticFeedback(AppHaptic.ImpactMedium)
                onSaveClick()
            },
            isLoading = isSaving,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@PreviewLightDark
@Composable
private fun RenameDeviceSheetContentPreview() {
    NimbatTheme {
        RenameDeviceSheetContent(
            nameState = TextFieldState("Emmanuel's iPhone"),
            deviceIcon = Lucide.Smartphone,
            errorMessage = null,
            isSaving = false,
            onSaveClick = {},
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        )
    }
}
