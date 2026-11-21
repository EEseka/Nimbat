package com.eeseka.nimbat.main_shell.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.main_shell.presentation.mappers.getIcon
import com.eeseka.nimbat.main_shell.presentation.mappers.toTitle
import com.eeseka.nimbat.main_shell.presentation.model.NimbatNavigationItem

@Composable
fun PlaceholderDestination(
    item: NimbatNavigationItem,
    mainShellPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(mainShellPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = item.getIcon(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        NimbatText(
            text = "Welcome to ${item.toTitle().asString()} 🙏🏽",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
    }
}

@PreviewLightDark
@Composable
private fun PlaceholderDestinationPreview() {
    NimbatTheme {
        PlaceholderDestination(
            item = NimbatNavigationItem.HOME,
            mainShellPadding = PaddingValues(0.dp),
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
        )
    }
}
