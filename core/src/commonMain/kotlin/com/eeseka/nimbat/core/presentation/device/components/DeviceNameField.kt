package com.eeseka.nimbat.core.presentation.device.components

import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Smartphone
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatTextField
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import nimbat.core.generated.resources.Res
import nimbat.core.generated.resources.device_name
import nimbat.core.generated.resources.device_name_placeholder
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeviceNameField(
    state: TextFieldState,
    deviceIcon: ImageVector,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    NimbatTextField(
        state = state,
        modifier = modifier,
        label = stringResource(Res.string.device_name),
        placeholder = stringResource(Res.string.device_name_placeholder),
        errorMessage = errorMessage,
        leadingIcon = {
            Icon(
                imageVector = deviceIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done
        )
    )
}

@PreviewLightDark
@Composable
private fun DeviceNameFieldPreview() {
    NimbatTheme {
        DeviceNameField(
            state = TextFieldState("Emmanuel's iPhone"),
            deviceIcon = Lucide.Smartphone,
            errorMessage = null,
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
        )
    }
}
