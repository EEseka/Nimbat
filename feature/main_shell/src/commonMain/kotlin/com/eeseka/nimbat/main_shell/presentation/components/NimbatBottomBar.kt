package com.eeseka.nimbat.main_shell.presentation.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.main_shell.presentation.mappers.getIcon
import com.eeseka.nimbat.main_shell.presentation.mappers.toSfSymbolName
import com.eeseka.nimbat.main_shell.presentation.mappers.toTitle
import com.eeseka.nimbat.main_shell.presentation.model.NimbatNavigationItem
import com.mohamedrejeb.calf.ui.ExperimentalCalfUiApi
import com.mohamedrejeb.calf.ui.navigation.AdaptiveNavigationBar
import com.mohamedrejeb.calf.ui.navigation.UIKitTabBarConfiguration
import com.mohamedrejeb.calf.ui.navigation.UIKitUITabBarItem
import com.mohamedrejeb.calf.ui.uikit.UIKitImage

@OptIn(ExperimentalCalfUiApi::class)
@Composable
fun NimbatBottomBar(
    selectedItem: NimbatNavigationItem,
    onItemSelected: (NimbatNavigationItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val entries = NimbatNavigationItem.entries

    val scheme = MaterialTheme.colorScheme

    AdaptiveNavigationBar(
        modifier = modifier,
        iosItems = entries.map { item ->
            UIKitUITabBarItem(
                title = item.toTitle().asString(),
                image = UIKitImage.SystemName(item.toSfSymbolName())
            )
        },
        iosSelectedIndex = selectedItem.ordinal,
        iosOnItemSelected = { index -> onItemSelected(entries[index]) },
        iosConfiguration = UIKitTabBarConfiguration(selectedItemColor = scheme.primary),
        content = {
            entries.forEach { item ->
                NavigationBarItem(
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
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = scheme.onPrimaryContainer,
                        selectedTextColor = scheme.primary,
                        indicatorColor = scheme.primaryContainer
                    )
                )
            }
        }
    )
}

@PreviewLightDark
@Composable
private fun NimbatBottomBarPreview() {
    NimbatTheme {
        NimbatBottomBar(
            selectedItem = NimbatNavigationItem.HOME,
            onItemSelected = {}
        )
    }
}
