package com.eeseka.nimbat.core.design_system.components.toggles_and_control

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.mohamedrejeb.calf.ui.toggle.AdaptiveSwitch

@Composable
fun NimbatSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    AdaptiveSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled
    )
}

@PreviewLightDark
@Composable
private fun NimbatSwitchPreview() {
    NimbatTheme {
        NimbatSwitch(
            checked = true,
            onCheckedChange = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun NimbatDisabledSwitchPreview() {
    NimbatTheme {
        NimbatSwitch(
            checked = false,
            onCheckedChange = {},
            enabled = false
        )
    }
}
