package com.eeseka.nimbat.onboarding.presentation.components.illustrations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.components.illustrations.drawDevice
import com.eeseka.nimbat.core.design_system.components.illustrations.drawPhone
import com.eeseka.nimbat.core.design_system.components.illustrations.drawWave
import com.eeseka.nimbat.core.design_system.components.illustrations.rememberLoopProgress
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.core.presentation.util.DeviceConfiguration
import kotlin.math.PI
import kotlin.math.sin

private const val WAVE_COUNT = 3

@Composable
fun NameIllustration(
    name: String,
    deviceConfiguration: DeviceConfiguration,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val time by rememberLoopProgress(durationMillis = 2_000, label = "name")
    val sent = remember { Animatable(1f) }
    val tail = remember { Path() }

    LaunchedEffect(name) {
        sent.snapTo(0f)
        sent.animateTo(1f, tween(durationMillis = 900, easing = LinearEasing))
    }

    Canvas(modifier) {
        val side = size.minDimension
        val sender = Offset(center.x - side * 0.28f, center.y + side * 0.08f)
        val receiver = Offset(center.x + side * 0.28f, center.y + side * 0.08f)
        val waveStart = sender.x + side * 0.16f
        val waveEnd = receiver.x - side * 0.11f
        val span = waveEnd - waveStart
        val stroke = side * 0.016f
        val radius = side * 0.07f

        repeat(WAVE_COUNT) { wave ->
            val travel = (time + wave.toFloat() / WAVE_COUNT) % 1f
            drawWave(
                center = Offset(waveStart + span * travel, sender.y),
                radius = radius,
                facingDegrees = 0f,
                color = scheme.tertiary.copy(alpha = (1 - travel) * 0.4f),
                strokeWidth = stroke
            )
        }

        val pulse = sent.value
        if (pulse < 1f) {
            drawWave(
                center = Offset(waveStart + span * pulse, sender.y),
                radius = radius * 1.3f,
                facingDegrees = 0f,
                color = scheme.tertiary.copy(alpha = 1 - pulse * 0.4f),
                strokeWidth = stroke * 1.3f
            )
        }
        val bounce = sin(PI.toFloat() * ((pulse - 0.75f) / 0.25f).coerceIn(0f, 1f))

        drawDevice(deviceConfiguration, sender, side * 0.3f, scheme.primary, scheme.surface)

        scale(scale = 1f + 0.1f * bounce, pivot = receiver) {
            drawPhone(receiver, side * 0.3f, lerp(scheme.outline, scheme.tertiary, bounce), scheme.surface)
        }

        val tagWidth = side * 0.3f
        val tagHeight = side * 0.13f
        val tagBottom = receiver.y - side * 0.21f
        val tagTopLeft = Offset(receiver.x - tagWidth / 2, tagBottom - tagHeight)
        scale(scale = 1f + 0.18f * bounce, pivot = Offset(receiver.x, tagBottom)) {
            drawRoundRect(
                color = scheme.surfaceContainerHighest,
                topLeft = tagTopLeft,
                size = Size(tagWidth, tagHeight),
                cornerRadius = CornerRadius(tagHeight / 2)
            )
            tail.reset()
            tail.moveTo(receiver.x - side * 0.03f, tagBottom - 1f)
            tail.lineTo(receiver.x + side * 0.03f, tagBottom - 1f)
            tail.lineTo(receiver.x, tagBottom + side * 0.035f)
            tail.close()
            drawPath(tail, scheme.surfaceContainerHighest)

            val lineStart = tagTopLeft.x + tagHeight * 0.5f
            drawLine(
                color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                start = Offset(lineStart, tagTopLeft.y + tagHeight * 0.34f),
                end = Offset(lineStart + tagWidth * 0.32f, tagTopLeft.y + tagHeight * 0.34f),
                strokeWidth = side * 0.014f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = lerp(scheme.onSurface, scheme.tertiary, bounce),
                start = Offset(lineStart, tagTopLeft.y + tagHeight * 0.66f),
                end = Offset(lineStart + tagWidth * 0.58f, tagTopLeft.y + tagHeight * 0.66f),
                strokeWidth = side * 0.022f,
                cap = StrokeCap.Round
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun NameIllustrationPreview() {
    NimbatTheme {
        NameIllustration(
            name = "Emmanuel's iPhone",
            deviceConfiguration = DeviceConfiguration.MOBILE_PORTRAIT,
            modifier = Modifier.size(280.dp).background(MaterialTheme.colorScheme.background)
        )
    }
}
