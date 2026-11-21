package com.eeseka.nimbat.main_shell.presentation.mappers

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.FlaskConical
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Settings
import com.eeseka.nimbat.core.presentation.util.UiText
import com.eeseka.nimbat.main_shell.presentation.model.NimbatNavigationItem
import nimbat.feature.main_shell.generated.resources.Res
import nimbat.feature.main_shell.generated.resources.home
import nimbat.feature.main_shell.generated.resources.lab
import nimbat.feature.main_shell.generated.resources.settings

fun NimbatNavigationItem.toTitle(): UiText = when (this) {
    NimbatNavigationItem.HOME -> UiText.Resource(Res.string.home)
    NimbatNavigationItem.LAB -> UiText.Resource(Res.string.lab)
    NimbatNavigationItem.SETTINGS -> UiText.Resource(Res.string.settings)
}

fun NimbatNavigationItem.getIcon(): ImageVector = when (this) {
    NimbatNavigationItem.HOME -> Lucide.House
    NimbatNavigationItem.LAB -> Lucide.FlaskConical
    NimbatNavigationItem.SETTINGS -> Lucide.Settings
}

fun NimbatNavigationItem.toSfSymbolName(): String = when (this) {
    NimbatNavigationItem.HOME -> "house.fill"
    NimbatNavigationItem.LAB -> "flask.fill"
    NimbatNavigationItem.SETTINGS -> "gearshape.fill"
}
