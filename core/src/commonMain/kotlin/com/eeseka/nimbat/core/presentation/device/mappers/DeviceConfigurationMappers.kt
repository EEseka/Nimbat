package com.eeseka.nimbat.core.presentation.device.mappers

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.Laptop
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Smartphone
import com.composables.icons.lucide.Tablet
import com.eeseka.nimbat.core.presentation.util.DeviceConfiguration

fun DeviceConfiguration.getIcon(): ImageVector = when (this) {
    DeviceConfiguration.MOBILE_PORTRAIT, DeviceConfiguration.MOBILE_LANDSCAPE -> Lucide.Smartphone
    DeviceConfiguration.TABLET_PORTRAIT, DeviceConfiguration.TABLET_LANDSCAPE -> Lucide.Tablet
    DeviceConfiguration.DESKTOP -> Lucide.Laptop
}
