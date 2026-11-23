package com.eeseka.nimbat.onboarding.presentation.components.illustrations

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mic
import com.eeseka.nimbat.core.design_system.components.illustrations.drawIcon
import com.eeseka.nimbat.core.design_system.components.illustrations.drawWave
import com.eeseka.nimbat.core.design_system.components.illustrations.rememberLoopProgress
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme

private const val WAVE_COUNT = 3

@Composable
fun MicrophoneIllustration(
    isGranted: Boolean,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val waveColor by animateColorAsState(
        targetValue = if (isGranted) scheme.tertiary else scheme.outline,
        label = "waves"
    )
    val mic = rememberVectorPainter(Lucide.Mic)
    val check = rememberVectorPainter(Lucide.Check)
    val time by rememberLoopProgress(durationMillis = 1_800, label = "microphone")
    val badge by animateFloatAsState(
        targetValue = if (isGranted) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "badge"
    )

    Canvas(modifier) {
        val side = size.minDimension
        val stroke = side * 0.016f
        repeat(WAVE_COUNT) { wave ->
            val travel = (time + wave.toFloat() / WAVE_COUNT) % 1f
            val radius = side * (0.46f - 0.2f * travel)
            val alpha = 4 * travel * (1 - travel) * 0.85f
            val color = waveColor.copy(alpha = alpha)
            drawWave(center, radius, facingDegrees = 180f, color = color, strokeWidth = stroke, sweepDegrees = 60f)
            drawWave(center, radius, facingDegrees = 0f, color = color, strokeWidth = stroke, sweepDegrees = 60f)
        }

        drawCircle(scheme.primary, radius = side * 0.2f)
        drawIcon(mic, center, side * 0.18f, scheme.onPrimary)

        if (badge > 0f) {
            val badgeCenter = center + Offset(side * 0.15f, -side * 0.15f)
            drawCircle(scheme.background, radius = side * 0.09f * badge, center = badgeCenter)
            drawCircle(scheme.tertiary, radius = side * 0.075f * badge, center = badgeCenter)
            drawIcon(check, badgeCenter, side * 0.08f * badge, scheme.onTertiary)
        }
    }
}

@PreviewLightDark
@Composable
private fun MicrophoneIllustrationPreview() {
    NimbatTheme {
        MicrophoneIllustration(isGranted = true, modifier = Modifier.size(280.dp).background(MaterialTheme.colorScheme.background))
    }
}
