package com.eeseka.nimbat.core.design_system.components.buttons

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.components.progress_indicator.NimbatProgressIndicator
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.core.design_system.theme.extended
import com.mohamedrejeb.calf.ui.button.AdaptiveButton
import com.mohamedrejeb.calf.ui.button.LiquidGlassButtonColors

enum class NimbatButtonStyle {
    PRIMARY,
    DESTRUCTIVE_PRIMARY,
    SECONDARY,
    DESTRUCTIVE_SECONDARY,
    TEXT
}

@Composable
fun NimbatButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: NimbatButtonStyle = NimbatButtonStyle.PRIMARY,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    loadingText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    val isEffectivelyEnabled = enabled && !isLoading
    val shape = CircleShape
    val scheme = MaterialTheme.colorScheme

    val disabledContainer = scheme.onSurface.copy(alpha = 0.1f)
    val disabledContent = scheme.onSurfaceVariant.copy(alpha = 0.38f)

    val colors = when (style) {
        NimbatButtonStyle.PRIMARY -> ButtonDefaults.buttonColors(
            containerColor = scheme.primaryContainer,
            contentColor = scheme.onPrimaryContainer,
            disabledContainerColor = disabledContainer,
            disabledContentColor = disabledContent
        )

        NimbatButtonStyle.DESTRUCTIVE_PRIMARY -> ButtonDefaults.buttonColors(
            containerColor = scheme.errorContainer,
            contentColor = scheme.onErrorContainer,
            disabledContainerColor = disabledContainer,
            disabledContentColor = disabledContent
        )

        NimbatButtonStyle.SECONDARY -> ButtonDefaults.buttonColors(
            containerColor = scheme.extended.neutralSurface,
            contentColor = scheme.onSurface,
            disabledContainerColor = disabledContainer,
            disabledContentColor = disabledContent
        )

        NimbatButtonStyle.DESTRUCTIVE_SECONDARY -> ButtonDefaults.buttonColors(
            containerColor = scheme.extended.neutralSurface,
            contentColor = scheme.error,
            disabledContainerColor = disabledContainer,
            disabledContentColor = disabledContent
        )

        NimbatButtonStyle.TEXT -> ButtonDefaults.buttonColors(
            containerColor = scheme.extended.neutralSurface,
            contentColor = scheme.tertiary,
            disabledContainerColor = disabledContainer,
            disabledContentColor = disabledContent
        )
    }

    val liquidGlassColors = when (style) {
        NimbatButtonStyle.PRIMARY -> LiquidGlassButtonColors(
            tintColor = scheme.primaryContainer,
            surfaceColor = scheme.primaryContainer,
            contentColor = scheme.onPrimaryContainer,
            disabledContentColor = disabledContent
        )

        NimbatButtonStyle.DESTRUCTIVE_PRIMARY -> LiquidGlassButtonColors(
            tintColor = scheme.errorContainer,
            surfaceColor = scheme.errorContainer,
            contentColor = scheme.onErrorContainer,
            disabledContentColor = disabledContent
        )

        NimbatButtonStyle.SECONDARY -> LiquidGlassButtonColors(
            tintColor = scheme.extended.neutralSurface,
            surfaceColor = scheme.extended.neutralSurface,
            contentColor = scheme.onSurface,
            disabledContentColor = disabledContent
        )

        NimbatButtonStyle.DESTRUCTIVE_SECONDARY -> LiquidGlassButtonColors(
            tintColor = scheme.extended.neutralSurface,
            surfaceColor = scheme.extended.neutralSurface,
            contentColor = scheme.error,
            disabledContentColor = disabledContent
        )

        NimbatButtonStyle.TEXT -> LiquidGlassButtonColors(
            tintColor = scheme.extended.neutralSurface,
            surfaceColor = scheme.extended.neutralSurface,
            contentColor = scheme.tertiary,
            disabledContentColor = disabledContent
        )
    }

    AdaptiveButton(
        onClick = onClick,
        enabled = isEffectivelyEnabled,
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = colors,
        liquidGlassColors = liquidGlassColors,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
    ) {
        NimbatButtonContent(text, isLoading, loadingText, leadingIcon)
    }
}

@Composable
private fun NimbatButtonContent(
    text: String,
    isLoading: Boolean,
    loadingText: String?,
    icon: @Composable (() -> Unit)?
) {
    val label = if (isLoading && loadingText != null) loadingText else text

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        AnimatedContent(
            targetState = isLoading,
            transitionSpec = {
                fadeIn() togetherWith fadeOut() using SizeTransform(clip = false)
            },
            label = "NimbatButtonIconTransition"
        ) { loading ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (loading) {
                    NimbatProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current
                    )
                } else {
                    icon?.invoke()
                }

                if ((loading || icon != null) && label.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }

        AnimatedContent(
            targetState = label,
            transitionSpec = {
                fadeIn() togetherWith fadeOut() using SizeTransform(clip = false)
            },
            label = "NimbatButtonLabelTransition"
        ) { animatedLabel ->
            NimbatText(
                text = animatedLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun NimbatButtonPreview() {
    NimbatTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            NimbatButtonStyle.entries.forEach { style ->
                NimbatButton(text = style.name, onClick = {}, style = style)
            }
            NimbatButton(text = "Disabled", onClick = {}, enabled = false)
        }
    }
}
