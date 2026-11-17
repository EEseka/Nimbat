package com.eeseka.nimbat.core.design_system.components.buttons

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Lucide
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.mohamedrejeb.calf.ui.button.AdaptiveIconButton

@Composable
fun NimbatIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    AdaptiveIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    ) {
        content()
    }
}

@PreviewLightDark
@Composable
private fun NimbatIconButtonPreview() {
    NimbatTheme {
        NimbatIconButton(
            onClick = {}
        ) {
            Icon(
                imageVector = Lucide.Info,
                contentDescription = null
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun NimbatDisabledIconButtonPreview() {
    NimbatTheme {
        NimbatIconButton(
            onClick = {},
            enabled = false
        ) {
            Icon(
                imageVector = Lucide.Info,
                contentDescription = null
            )
        }
    }
}
