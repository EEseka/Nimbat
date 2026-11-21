package com.eeseka.nimbat.main_shell.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.main_shell.presentation.mappers.getIcon
import com.eeseka.nimbat.main_shell.presentation.mappers.toTitle
import com.eeseka.nimbat.main_shell.presentation.model.NimbatNavigationItem

@Composable
fun NimbatNavigationRail(
    selectedItem: NimbatNavigationItem,
    onItemSelected: (NimbatNavigationItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val colors = NavigationRailItemDefaults.colors(
        indicatorColor = scheme.primaryContainer,
        selectedIconColor = scheme.onPrimaryContainer,
        selectedTextColor = scheme.primary
    )
    Row(modifier = modifier) {
        NavigationRail(modifier = Modifier.fillMaxHeight()) {
            NimbatNavigationItem.entries.forEach { item ->
                NavigationRailItem(
                    selected = selectedItem == item,
                    onClick = { onItemSelected(item) },
                    icon = {
                        Icon(
                            imageVector = item.getIcon(),
                            contentDescription = null
                        )
                    },
                    label = {
                        NimbatText(
                            text = item.toTitle().asString(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = colors
                )
            }
        }
        VerticalDivider(
            thickness = 1.dp,
            color = scheme.outlineVariant.copy(alpha = 0.2f)
        )
    }
}

@PreviewLightDark
@Composable
private fun NimbatNavigationRailPreview() {
    NimbatTheme {
        NimbatNavigationRail(
            selectedItem = NimbatNavigationItem.LAB,
            onItemSelected = {}
        )
    }
}
