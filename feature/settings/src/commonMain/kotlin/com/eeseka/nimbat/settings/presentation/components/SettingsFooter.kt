package com.eeseka.nimbat.settings.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.components.brand.NimbatLogo
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.components.util.AppHaptic
import com.eeseka.nimbat.core.design_system.components.util.rememberAppHaptic
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import kotlinx.coroutines.flow.filter
import nimbat.feature.settings.generated.resources.Res
import nimbat.feature.settings.generated.resources.app_version
import nimbat.feature.settings.generated.resources.made_with_kotlin
import nimbat.feature.settings.generated.resources.send_chirp
import org.jetbrains.compose.resources.stringResource

private const val RING_COUNT = 2
private const val RING_DELAY = 0.3f
private const val RING_START = 0.65f
private const val RING_END = 0.95f

@Composable
fun SettingsFooter(
    appVersion: String,
    isPlayingChirp: Boolean,
    onLogoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChirpingLogo(isPlayingChirp = isPlayingChirp, onClick = onLogoClick)

        NimbatText(
            text = stringResource(Res.string.app_version, appVersion),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp)
        )

        NimbatText(
            text = stringResource(Res.string.made_with_kotlin),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ChirpingLogo(
    isPlayingChirp: Boolean,
    onClick: () -> Unit
) {
    val hapticFeedback = rememberAppHaptic()
    val sendChirpLabel = stringResource(Res.string.send_chirp)
    val rippleColor = MaterialTheme.colorScheme.tertiary
    val currentIsPlayingChirp by rememberUpdatedState(isPlayingChirp)
    val ripple = remember { Animatable(1f) }

    // Each new chirp restarts the rings; a chirp ending doesn't cut them short
    LaunchedEffect(Unit) {
        snapshotFlow { currentIsPlayingChirp }
            .filter { it }
            .collect {
                ripple.snapTo(0f)
                ripple.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
            }
    }

    Box(modifier = Modifier.size(32.dp)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val progress = ripple.value
            if (progress >= 1f) return@Canvas
            repeat(RING_COUNT) { i ->
                val ringProgress = ((progress - i * RING_DELAY) / (1f - RING_DELAY)).coerceIn(0f, 1f)
                drawCircle(
                    color = rippleColor.copy(alpha = 1f - ringProgress),
                    radius = size.minDimension * (RING_START + ringProgress * (RING_END - RING_START)),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        NimbatLogo(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = null,
                    indication = null,
                    enabled = !isPlayingChirp,
                    onClickLabel = sendChirpLabel,
                    role = Role.Button
                ) {
                    hapticFeedback(AppHaptic.ImpactLight)
                    onClick()
                }
        )
    }
}

@PreviewLightDark
@Composable
private fun SettingsFooterPreview() {
    NimbatTheme {
        SettingsFooter(
            appVersion = "1.0.0",
            isPlayingChirp = false,
            onLogoClick = {},
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
        )
    }
}
