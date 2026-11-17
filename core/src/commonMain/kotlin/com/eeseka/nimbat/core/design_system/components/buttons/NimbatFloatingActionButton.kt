package com.eeseka.nimbat.core.design_system.components.buttons

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.mohamedrejeb.calf.ui.button.AdaptiveButton
import com.mohamedrejeb.calf.ui.button.LiquidGlassButtonColors

@Composable
fun NimbatFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    AdaptiveButton(
        onClick = onClick,
        modifier = modifier.size(56.dp),
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = scheme.primaryContainer,
            contentColor = scheme.onPrimaryContainer,
            disabledContainerColor = scheme.onSurface.copy(alpha = 0.1f),
            disabledContentColor = scheme.onSurfaceVariant.copy(alpha = 0.38f)
        ),
        liquidGlassColors = LiquidGlassButtonColors(
            tintColor = scheme.primaryContainer,
            surfaceColor = scheme.primaryContainer,
            contentColor = scheme.onPrimaryContainer,
            disabledContentColor = scheme.onSurfaceVariant.copy(alpha = 0.38f)
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        content()
    }
}

@PreviewLightDark
@Composable
private fun NimbatFloatingActionButtonPreview() {
    NimbatTheme {
        NimbatFloatingActionButton(
            onClick = {}
        ) {
            Icon(
                imageVector = Lucide.Plus,
                contentDescription = null
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun NimbatDisabledFloatingActionButtonPreview() {
    NimbatTheme {
        NimbatFloatingActionButton(
            onClick = {},
            enabled = false
        ) {
            Icon(
                imageVector = Lucide.Plus,
                contentDescription = null
            )
        }
    }
}
