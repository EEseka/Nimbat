package com.eeseka.nimbat.guard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.components.toggles_and_control.NimbatSegmentedControl
import com.eeseka.nimbat.core.design_system.components.toggles_and_control.NimbatSegmentedItem
import com.eeseka.nimbat.core.design_system.components.toggles_and_control.NimbatSegmentedStyle
import com.eeseka.nimbat.core.design_system.components.toggles_and_control.NimbatSwitch
import com.eeseka.nimbat.core.design_system.components.util.AppHaptic
import com.eeseka.nimbat.core.design_system.components.util.rememberAppHaptic
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.core.design_system.theme.extended
import com.eeseka.nimbat.core.domain.guard.model.GuardMode
import com.eeseka.nimbat.guard.presentation.GuardAction
import com.eeseka.nimbat.guard.presentation.mappers.getTitle
import kotlinx.collections.immutable.toImmutableList
import nimbat.feature.guard.generated.resources.Res
import nimbat.feature.guard.generated.resources.lock_computer
import nimbat.feature.guard.generated.resources.lock_computer_message
import org.jetbrains.compose.resources.stringResource

@Composable
fun GuardModePicker(
    mode: GuardMode,
    isScreenUpright: Boolean,
    onAction: (GuardAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = rememberAppHaptic()
    val modes = remember(isScreenUpright) {
        GuardMode.entries.filter { isScreenUpright || it != GuardMode.CURTAIN }
    }
    val items = modes.map { NimbatSegmentedItem(title = it.getTitle()) }.toImmutableList()

    NimbatSegmentedControl(
        items = items,
        selectedIndex = modes.indexOf(mode).coerceAtLeast(0),
        onItemSelected = { index ->
            hapticFeedback(AppHaptic.Selection)
            onAction(GuardAction.OnModeSelected(modes[index]))
        },
        style = NimbatSegmentedStyle.FIXED_BAR,
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LockComputerRow(
    isOn: Boolean,
    onAction: (GuardAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = rememberAppHaptic()
    val indigo = MaterialTheme.colorScheme.extended.indigoContainer
    val onIndigo = MaterialTheme.colorScheme.extended.onIndigoContainer

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ListItemDefaults.segmentedShapes(index = 0, count = 1).shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .background(indigo),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.Lock,
                contentDescription = null,
                tint = onIndigo,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            NimbatText(
                text = stringResource(Res.string.lock_computer),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            NimbatText(
                text = stringResource(Res.string.lock_computer_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        NimbatSwitch(
            checked = isOn,
            onCheckedChange = { checked ->
                hapticFeedback(AppHaptic.Selection)
                onAction(GuardAction.OnLockComputerToggled(checked))
            }
        )
    }
}

@PreviewLightDark
@Composable
private fun GuardOptionsPreview() {
    NimbatTheme {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            GuardModePicker(mode = GuardMode.CURTAIN, isScreenUpright = true, onAction = {})
            Spacer(modifier = Modifier.height(16.dp))
            LockComputerRow(isOn = true, onAction = {})
        }
    }
}
