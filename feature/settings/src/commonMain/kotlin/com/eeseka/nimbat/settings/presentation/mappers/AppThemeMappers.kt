package com.eeseka.nimbat.settings.presentation.mappers

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Moon
import com.composables.icons.lucide.Smartphone
import com.composables.icons.lucide.Sun
import com.eeseka.nimbat.core.domain.settings.AppTheme
import nimbat.feature.settings.generated.resources.Res
import nimbat.feature.settings.generated.resources.theme_dark
import nimbat.feature.settings.generated.resources.theme_light
import nimbat.feature.settings.generated.resources.theme_system
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppTheme.getTitle(): String = stringResource(
    when (this) {
        AppTheme.SYSTEM -> Res.string.theme_system
        AppTheme.LIGHT -> Res.string.theme_light
        AppTheme.DARK -> Res.string.theme_dark
    }
)

fun AppTheme.getIcon(): ImageVector = when (this) {
    AppTheme.SYSTEM -> Lucide.Smartphone
    AppTheme.LIGHT -> Lucide.Sun
    AppTheme.DARK -> Lucide.Moon
}
