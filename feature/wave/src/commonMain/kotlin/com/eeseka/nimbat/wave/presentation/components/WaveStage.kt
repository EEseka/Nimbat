package com.eeseka.nimbat.wave.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.components.illustrations.drawDevice
import com.eeseka.nimbat.core.design_system.components.illustrations.drawHand
import com.eeseka.nimbat.core.design_system.components.illustrations.drawWave
import com.eeseka.nimbat.core.design_system.components.illustrations.rememberLoopProgress
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.core.design_system.theme.extended
import com.eeseka.nimbat.core.presentation.util.DeviceConfiguration
import com.eeseka.nimbat.sound.GestureDirection
import kotlinx.coroutines.launch
import kotlin.math.max

private const val TONE_WAVE_COUNT = 3
private const val ECHO_WAVE_COUNT = 2

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WaveStage(
    configuration: DeviceConfiguration,
    isListening: Boolean,
    calibrationProgress: Float,
    towardLevel: Float,
    awayLevel: Float,
    lastGesture: GestureDirection?,
    gestureCount: Int,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val sky = scheme.extended.skyContainer
    val backdropShape = MaterialShapes.Cookie12Sided.toShape()
    val turn by rememberLoopProgress(durationMillis = 40_000, label = "stage_backdrop")
    val pulse by rememberLoopProgress(durationMillis = 1_800, label = "stage_tone")
    val echo by animateFloatAsState(
        targetValue = if (isListening) max(towardLevel, awayLevel) else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "stage_echo"
    )
    val calibration by animateFloatAsState(targetValue = calibrationProgress, label = "stage_calibration")
    val handNudge = remember { Animatable(0f) }
    val burst = remember { Animatable(1f) }

    LaunchedEffect(gestureCount) {
        if (gestureCount == 0) return@LaunchedEffect
        launch {
            burst.snapTo(0f)
            burst.animateTo(1f, tween(durationMillis = 700, easing = FastOutSlowInEasing))
        }
        handNudge.snapTo(if (lastGesture == GestureDirection.TOWARD) 1f else -1f)
        handNudge.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessLow))
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = turn * 360f }
                .clip(backdropShape)
                .background(sky.copy(alpha = 0.16f))
        )

        if (isListening && calibration < 1f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = turn * 360f }
                    .drawWithCache {
                        val edge = Path().apply { addOutline(backdropShape.createOutline(size, layoutDirection, this@drawWithCache)) }
                        val edgeMeasure = PathMeasure().apply { setPath(edge, forceClosed = true) }
                        val traced = Path()
                        val stroke = Stroke(width = size.minDimension * 0.016f, cap = StrokeCap.Round, join = StrokeJoin.Round)

                        onDrawBehind {
                            traced.reset()
                            edgeMeasure.getSegment(0f, edgeMeasure.length * calibration, traced)
                            drawPath(traced, color = sky, style = stroke)
                        }
                    }
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val side = size.minDimension
            val center = Offset(size.width / 2, size.height / 2)
            val deviceCenter = center + Offset(0f, side * 0.25f)
            val handCenter = center + Offset(0f, side * (-0.24f + handNudge.value * 0.06f))

            if (isListening) {
                repeat(TONE_WAVE_COUNT) { i ->
                    val progress = (pulse + i.toFloat() / TONE_WAVE_COUNT) % 1f
                    drawWave(
                        center = deviceCenter,
                        radius = side * (0.17f + progress * 0.14f),
                        facingDegrees = 270f,
                        color = scheme.tertiary.copy(alpha = (1f - progress) * 0.9f),
                        strokeWidth = side * 0.016f
                    )
                }
                repeat(ECHO_WAVE_COUNT) { i ->
                    drawWave(
                        center = handCenter,
                        radius = side * (0.12f + i * 0.04f),
                        facingDegrees = 90f,
                        color = scheme.secondary.copy(alpha = 0.15f + 0.85f * echo),
                        strokeWidth = side * 0.016f
                    )
                }
            }

            if (burst.value < 1f) {
                drawCircle(
                    color = sky.copy(alpha = 1f - burst.value),
                    radius = side * (0.12f + burst.value * 0.3f),
                    center = handCenter,
                    style = Stroke(width = side * 0.012f)
                )
            }

            drawHand(center = handCenter, height = side * 0.22f, color = scheme.onSurfaceVariant)
            drawDevice(
                configuration = configuration,
                center = deviceCenter,
                height = side * 0.24f,
                body = scheme.primary,
                screen = scheme.surface
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun WaveStagePreview() {
    NimbatTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            WaveStage(
                configuration = DeviceConfiguration.MOBILE_PORTRAIT,
                isListening = true,
                calibrationProgress = 1f,
                towardLevel = 0.6f,
                awayLevel = 0f,
                lastGesture = GestureDirection.TOWARD,
                gestureCount = 0,
                modifier = Modifier.size(240.dp)
            )
        }
    }
}
