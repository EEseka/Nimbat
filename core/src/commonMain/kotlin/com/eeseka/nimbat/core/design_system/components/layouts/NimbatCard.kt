package com.eeseka.nimbat.core.design_system.components.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme

enum class NimbatCardStyle {
    FILLED,
    OUTLINED,
    ELEVATED
}

@Composable
fun NimbatCard(
    modifier: Modifier = Modifier,
    style: NimbatCardStyle = NimbatCardStyle.FILLED,
    shape: Shape = MaterialTheme.shapes.large,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    when (style) {
        NimbatCardStyle.FILLED -> {
            if (onClick != null) {
                Card(
                    onClick = onClick,
                    modifier = modifier,
                    shape = shape,
                    content = content
                )
            } else {
                Card(
                    modifier = modifier,
                    shape = shape,
                    content = content
                )
            }
        }

        NimbatCardStyle.OUTLINED -> {
            if (onClick != null) {
                OutlinedCard(
                    onClick = onClick,
                    modifier = modifier,
                    shape = shape,
                    content = content
                )
            } else {
                OutlinedCard(
                    modifier = modifier,
                    shape = shape,
                    content = content
                )
            }
        }

        NimbatCardStyle.ELEVATED -> {
            if (onClick != null) {
                ElevatedCard(
                    onClick = onClick,
                    modifier = modifier,
                    shape = shape,
                    content = content
                )
            } else {
                ElevatedCard(
                    modifier = modifier,
                    shape = shape,
                    content = content
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun NimbatCardPreview() {
    NimbatTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            NimbatCardStyle.entries.forEach { style ->
                NimbatCard(style = style) {
                    NimbatText(
                        text = "Hello World",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(4.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    NimbatText(
                        text = "This is a ${style.name} card",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }
    }
}
