package com.eeseka.nimbat.onboarding.presentation.components.illustrations

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.components.illustrations.drawHand
import com.eeseka.nimbat.core.design_system.components.illustrations.drawPhone
import com.eeseka.nimbat.core.design_system.components.illustrations.drawWave
import com.eeseka.nimbat.core.design_system.components.illustrations.rememberLoopProgress
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val WAVE_COUNT = 3
private const val ECHO_COUNT = 4
private const val ECHO_GAP = 0.2f

@Composable
fun MotionIllustration(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val time by rememberLoopProgress(durationMillis = 2_400, label = "motion")

    Canvas(modifier) {
        val side = size.minDimension
        val angle = 2 * PI.toFloat() * time
        val phone = Offset(center.x - side * 0.28f, center.y)
        val hand = Offset(center.x + side * 0.22f + side * 0.12f * sin(angle), center.y)
        val toward = -cos(angle)
        val echoGap = ECHO_GAP * (1 - 0.4f * toward)
        val waveStart = phone.x + side * 0.1f
        val waveEnd = hand.x - side * 0.1f
        val span = waveEnd - waveStart
        val stroke = side * 0.016f
        val radius = side * 0.07f

        repeat(WAVE_COUNT) { wave ->
            val travel = (time * 3 + wave.toFloat() / WAVE_COUNT) % 1f
            drawWave(
                center = Offset(waveStart + span * travel, phone.y - side * 0.07f),
                radius = radius,
                facingDegrees = 0f,
                color = scheme.tertiary.copy(alpha = 1 - travel * 0.6f),
                strokeWidth = stroke
            )
        }
        repeat(ECHO_COUNT) { wave ->
            val travel = (time * 3 + wave * echoGap) % 1f
            drawWave(
                center = Offset(waveEnd - span * travel, phone.y + side * 0.07f),
                radius = radius,
                facingDegrees = 180f,
                color = scheme.secondary.copy(alpha = 1 - travel * 0.7f),
                strokeWidth = stroke
            )
        }

        drawPhone(phone, side * 0.3f, scheme.primary, scheme.surface)
        drawHand(hand, side * 0.34f, scheme.secondaryContainer)
    }
}

@PreviewLightDark
@Composable
private fun MotionIllustrationPreview() {
    NimbatTheme {
        MotionIllustration(Modifier.size(280.dp).background(MaterialTheme.colorScheme.background))
    }
}
