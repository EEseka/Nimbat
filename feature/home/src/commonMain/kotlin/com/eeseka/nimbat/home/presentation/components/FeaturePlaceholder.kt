package com.eeseka.nimbat.home.presentation.components

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
import com.eeseka.nimbat.core.design_system.components.buttons.NimbatButton
import com.eeseka.nimbat.core.design_system.components.buttons.NimbatButtonStyle
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.home.presentation.mappers.getIcon
import com.eeseka.nimbat.home.presentation.mappers.getTitle
import com.eeseka.nimbat.home.presentation.model.HomeFeature

@Composable
fun FeaturePlaceholder(
    feature: HomeFeature,
    mainShellPadding: PaddingValues,
    onBackClick: () -> Unit,
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
            imageVector = feature.getIcon(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        NimbatText(
            text = "Welcome to ${feature.getTitle()} 🙏🏽",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        NimbatButton(
            text = "Back",
            onClick = onBackClick,
            style = NimbatButtonStyle.TEXT,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@PreviewLightDark
@Composable
private fun FeaturePlaceholderPreview() {
    NimbatTheme {
        FeaturePlaceholder(
            feature = HomeFeature.WAVE,
            mainShellPadding = PaddingValues(0.dp),
            onBackClick = {},
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
        )
    }
}
