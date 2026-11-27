package com.eeseka.nimbat.guard.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.components.illustrations.drawDevice
import com.eeseka.nimbat.core.design_system.components.illustrations.drawWave
import com.eeseka.nimbat.core.design_system.components.illustrations.rememberLoopProgress
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.core.design_system.theme.extended
import com.eeseka.nimbat.core.presentation.util.DeviceConfiguration

private const val WAVE_COUNT = 3
private const val WAVE_DIRECTIONS = 4

@Composable
fun GuardScene(
    configuration: DeviceConfiguration,
    color: Color,
    screenColor: Color,
    modifier: Modifier = Modifier,
    pulseMillis: Int = 2_400,
    movementLevel: Float = 0f,
    movementCount: Int = 0
) {
    val pulse by rememberLoopProgress(durationMillis = pulseMillis, label = "guard_scene_waves")
    val glow by animateFloatAsState(
        targetValue = movementLevel,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "guard_scene_glow"
    )
    val ripple = remember { Animatable(1f) }

    LaunchedEffect(movementCount) {
        if (movementCount == 0) return@LaunchedEffect
        ripple.snapTo(0f)
        ripple.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
    }

    Canvas(modifier = modifier) {
        val side = size.minDimension
        val center = Offset(size.width / 2, size.height / 2)
        val deviceHeight = side * 0.3f
        val strokeWidth = 3.dp.toPx()

        repeat(WAVE_COUNT) { i ->
            val progress = (pulse + i.toFloat() / WAVE_COUNT) % 1f
            val alpha = (1f - progress) * (0.6f + 0.4f * glow)
            repeat(WAVE_DIRECTIONS) { direction ->
                drawWave(
                    center = center,
                    radius = side * (0.24f + progress * 0.24f),
                    facingDegrees = direction * 90f,
                    color = color.copy(alpha = alpha),
                    strokeWidth = strokeWidth,
                    sweepDegrees = 64f
                )
            }
        }

        if (ripple.value < 1f) {
            drawCircle(
                color = color.copy(alpha = 1f - ripple.value),
                radius = side * (0.2f + ripple.value * 0.3f),
                center = center,
                style = Stroke(width = strokeWidth)
            )
        }

        drawDevice(
            configuration = configuration,
            center = center,
            height = deviceHeight,
            body = color,
            screen = screenColor
        )
    }
}

@Preview
@Composable
private fun GuardScenePreview() {
    NimbatTheme {
        val coral = MaterialTheme.colorScheme.extended.coralContainer

        GuardScene(
            configuration = DeviceConfiguration.MOBILE_PORTRAIT,
            color = MaterialTheme.colorScheme.extended.onCoralContainer,
            screenColor = coral,
            modifier = Modifier
                .background(coral)
                .size(240.dp)
        )
    }
}
