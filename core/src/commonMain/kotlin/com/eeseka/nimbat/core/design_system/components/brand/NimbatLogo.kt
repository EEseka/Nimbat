package com.eeseka.nimbat.core.design_system.components.brand

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import nimbat.core.generated.resources.Res
import nimbat.core.generated.resources.nimbat_logo
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun NimbatLogo(modifier: Modifier = Modifier) {
    Image(
        imageVector = vectorResource(Res.drawable.nimbat_logo),
        contentDescription = stringResource(Res.string.nimbat_logo),
        modifier = modifier
    )
}

@PreviewLightDark
@Composable
private fun NimbatLogoPreview() {
    NimbatTheme {
        NimbatLogo(modifier = Modifier.background(MaterialTheme.colorScheme.background))
    }
}
