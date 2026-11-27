package com.eeseka.nimbat.guard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.core.presentation.util.DeviceConfiguration

private val MAX_VISUAL_SIZE = 280.dp

private const val TALL_VISUAL_HEIGHT_FRACTION = 0.45f

@Composable
fun GuardOverlayLayout(
    configuration: DeviceConfiguration,
    visual: @Composable (size: Dp) -> Unit,
    words: @Composable ColumnScope.() -> Unit,
    button: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    NimbatTheme(darkTheme = true, pureBlack = true) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (configuration == DeviceConfiguration.MOBILE_LANDSCAPE) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        val visualSize = minOf(maxWidth, maxHeight, MAX_VISUAL_SIZE)

                        Box(modifier = Modifier.size(visualSize), contentAlignment = Alignment.Center) {
                            visual(visualSize)
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        words()
                        Spacer(modifier = Modifier.height(24.dp))
                        button()
                    }
                }
            } else {
                BoxWithConstraints(modifier = Modifier.widthIn(max = 480.dp).fillMaxSize()) {
                    val visualSize = minOf(maxWidth, maxHeight * TALL_VISUAL_HEIGHT_FRACTION, MAX_VISUAL_SIZE)

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.size(visualSize), contentAlignment = Alignment.Center) {
                            visual(visualSize)
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        words()
                        Spacer(modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.fillMaxWidth()) { button() }
                    }
                }
            }
        }
    }
}
