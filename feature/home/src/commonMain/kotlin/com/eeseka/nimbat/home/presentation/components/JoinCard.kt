package com.eeseka.nimbat.home.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.components.illustrations.drawPhone
import com.eeseka.nimbat.core.design_system.components.illustrations.drawWave
import com.eeseka.nimbat.core.design_system.components.illustrations.rememberLoopProgress
import com.eeseka.nimbat.core.design_system.components.layouts.NimbatCard
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.home.presentation.mappers.getAccentColor
import com.eeseka.nimbat.home.presentation.mappers.getDescription
import com.eeseka.nimbat.home.presentation.mappers.getOnAccentColor
import com.eeseka.nimbat.home.presentation.mappers.getPulseMillis
import com.eeseka.nimbat.home.presentation.mappers.getTitle
import com.eeseka.nimbat.home.presentation.model.HomeFeature
import com.eeseka.nimbat.home.presentation.util.cardEntrance
import com.eeseka.nimbat.home.presentation.util.rememberCardEntrance

private const val WAVE_COUNT = 2

@Composable
fun JoinCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    playEntrance: Boolean = false,
    enterDelayMillis: Long = 0L
) {
    val contentColor = HomeFeature.JOIN.getOnAccentColor()
    val entrance = rememberCardEntrance(playEntrance = playEntrance, delayMillis = enterDelayMillis)

    NimbatCard(
        containerColor = HomeFeature.JOIN.getAccentColor(),
        contentColor = contentColor,
        onClick = onClick,
        modifier = modifier.cardEntrance { entrance.value }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                NimbatText(
                    text = HomeFeature.JOIN.getTitle(),
                    style = MaterialTheme.typography.titleLarge,
                    color = contentColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                NimbatText(
                    text = HomeFeature.JOIN.getDescription(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            JoinIllustration(modifier = Modifier.size(112.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun JoinIllustration(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val backdropShape = MaterialShapes.Cookie12Sided.toShape()
    val turn by rememberLoopProgress(durationMillis = 40_000, label = "join_backdrop")
    val pulse by rememberLoopProgress(durationMillis = HomeFeature.JOIN.getPulseMillis(), label = "join_waves")

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = turn * 360f }
                .clip(backdropShape)
                .background(scheme.onPrimaryContainer)
        )

        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val side = size.minDimension
            val phoneHeight = side * 0.42f
            val ours = Offset(side * 0.22f, size.height / 2)
            val theirs = Offset(side * 0.78f, size.height / 2)
            val strokeWidth = 3.dp.toPx()

            repeat(WAVE_COUNT) { i ->
                val progress = (pulse + i.toFloat() / WAVE_COUNT) % 1f
                val radius = side * (0.16f + progress * 0.2f)
                drawWave(
                    center = ours,
                    radius = radius,
                    facingDegrees = 0f,
                    color = scheme.tertiaryContainer.copy(alpha = 1f - progress),
                    strokeWidth = strokeWidth,
                    sweepDegrees = 64f
                )
                drawWave(
                    center = theirs,
                    radius = radius,
                    facingDegrees = 180f,
                    color = scheme.onSecondaryContainer.copy(alpha = 1f - progress),
                    strokeWidth = strokeWidth,
                    sweepDegrees = 64f
                )
            }

            drawPhone(ours, phoneHeight, body = scheme.primaryContainer, screen = scheme.onPrimaryContainer)
            drawPhone(theirs, phoneHeight, body = scheme.tertiaryContainer, screen = scheme.onPrimaryContainer)
        }
    }
}

@PreviewLightDark
@Composable
private fun JoinCardPreview() {
    NimbatTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            JoinCard(onClick = {})
        }
    }
}
