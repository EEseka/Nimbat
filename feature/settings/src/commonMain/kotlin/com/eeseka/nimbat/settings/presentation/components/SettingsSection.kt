package com.eeseka.nimbat.settings.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Thermometer
import com.composables.icons.lucide.Vibrate
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.components.toggles_and_control.NimbatSwitch
import com.eeseka.nimbat.core.design_system.components.util.AppHaptic
import com.eeseka.nimbat.core.design_system.components.util.rememberAppHaptic
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.core.design_system.theme.extended

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        NimbatText(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsRow(
    icon: ImageVector,
    badgeColor: Color,
    onBadgeColor: Color,
    title: String,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    control: @Composable (() -> Unit)? = null
) {
    val hapticFeedback = rememberAppHaptic()
    val shape = ListItemDefaults.segmentedShapes(index = index, count = count).shape

    val clickModifier = if (onClick != null) {
        Modifier.clickable {
            hapticFeedback(AppHaptic.ImpactLight)
            onClick()
        }
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(clickModifier)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingsIconBadge(
                icon = icon,
                color = badgeColor,
                onColor = onBadgeColor
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                NimbatText(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                subtitle?.let {
                    NimbatText(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (trailing != null) {
                Spacer(modifier = Modifier.width(16.dp))
                trailing()
            } else if (onClick != null) {
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    imageVector = Lucide.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (control != null) {
            Spacer(modifier = Modifier.height(12.dp))
            control()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsIconBadge(
    icon: ImageVector,
    color: Color,
    onColor: Color
) {
    val shape = MaterialShapes.Cookie9Sided.toShape()

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(shape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = onColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

@PreviewLightDark
@Composable
private fun SettingsSectionPreview() {
    NimbatTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            SettingsSection(title = "All") {
                SettingsRow(
                    icon = Lucide.Thermometer,
                    badgeColor = MaterialTheme.colorScheme.extended.coralContainer,
                    onBadgeColor = MaterialTheme.colorScheme.extended.onCoralContainer,
                    title = "Room Temperature",
                    subtitle = "Sound moves at 343 m/s at this temperature.",
                    index = 0,
                    count = 2,
                    trailing = { NimbatText(text = "20 °C", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                )
                SettingsRow(
                    icon = Lucide.Vibrate,
                    badgeColor = MaterialTheme.colorScheme.tertiaryContainer,
                    onBadgeColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    title = "Haptics",
                    index = 1,
                    count = 2,
                    trailing = { NimbatSwitch(checked = true, onCheckedChange = {}) }
                )
            }
        }
    }
}
