package com.eeseka.nimbat.home.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.components.illustrations.drawWave
import com.eeseka.nimbat.core.design_system.components.illustrations.rememberLoopProgress
import com.eeseka.nimbat.core.design_system.components.layouts.NimbatCard
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.home.presentation.mappers.getAccentColor
import com.eeseka.nimbat.home.presentation.mappers.getDescription
import com.eeseka.nimbat.home.presentation.mappers.getIcon
import com.eeseka.nimbat.home.presentation.mappers.getOnAccentColor
import com.eeseka.nimbat.home.presentation.mappers.getPulseMillis
import com.eeseka.nimbat.home.presentation.mappers.getTitle
import com.eeseka.nimbat.home.presentation.model.HomeFeature
import com.eeseka.nimbat.home.presentation.util.cardEntrance
import com.eeseka.nimbat.home.presentation.util.rememberCardEntrance

private const val WAVE_COUNT = 2

@Composable
fun FeatureCard(
    feature: HomeFeature,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    playEntrance: Boolean = false,
    enterDelayMillis: Long = 0L
) {
    val accentColor = feature.getAccentColor()
    val onAccentColor = feature.getOnAccentColor()
    val entrance = rememberCardEntrance(playEntrance = playEntrance, delayMillis = enterDelayMillis)

    NimbatCard(
        containerColor = accentColor,
        contentColor = onAccentColor,
        onClick = onClick,
        modifier = modifier.cardEntrance { entrance.value }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            FeatureBadge(
                icon = feature.getIcon(),
                color = onAccentColor,
                onColor = accentColor,
                pulseMillis = feature.getPulseMillis()
            )

            Spacer(modifier = Modifier.height(16.dp))

            NimbatText(
                text = feature.getTitle(),
                style = MaterialTheme.typography.titleLarge,
                color = onAccentColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            NimbatText(
                text = feature.getDescription(),
                style = MaterialTheme.typography.bodyMedium,
                color = onAccentColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FeatureBadge(
    icon: ImageVector,
    color: Color,
    onColor: Color,
    pulseMillis: Int
) {
    val badgeShape = MaterialShapes.Cookie9Sided.toShape()
    val pulse by rememberLoopProgress(durationMillis = pulseMillis, label = "badge_waves")

    Box(
        modifier = Modifier.width(96.dp).height(48.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val badgeRadius = size.height / 2
            val center = Offset(badgeRadius, size.height / 2)

            repeat(WAVE_COUNT) { i ->
                val progress = (pulse + i.toFloat() / WAVE_COUNT) % 1f
                drawWave(
                    center = center,
                    radius = badgeRadius * (1.3f + progress * 0.9f),
                    facingDegrees = 0f,
                    color = color.copy(alpha = (1f - progress) * 0.9f),
                    strokeWidth = 3.dp.toPx(),
                    sweepDegrees = 64f
                )
            }
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(badgeShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = onColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun FeatureCardPreview() {
    NimbatTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureCard(feature = HomeFeature.WAVE, onClick = {})
            FeatureCard(feature = HomeFeature.BREATHE, onClick = {})
        }
    }
}
