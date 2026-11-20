package com.eeseka.nimbat.onboarding.presentation.components.illustrations

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Ear
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Waves
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.onboarding.presentation.components.illustrations.util.drawIcon
import com.eeseka.nimbat.onboarding.presentation.components.illustrations.util.rememberLoopProgress
import com.eeseka.nimbat.onboarding.presentation.model.SoundCheckStatus
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

private const val RIPPLE_COUNT = 3
private const val CONFETTI_COUNT = 18

@Composable
fun SoundCheckIllustration(
    status: SoundCheckStatus,
    clarity: Float,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val orbColor by animateColorAsState(
        targetValue = when (status) {
            SoundCheckStatus.NOT_STARTED -> scheme.primaryContainer
            SoundCheckStatus.LISTENING -> scheme.primary
            SoundCheckStatus.PASSED -> scheme.tertiary
            SoundCheckStatus.FAILED -> scheme.error
        },
        label = "orb"
    )
    val iconTint = when (status) {
        SoundCheckStatus.NOT_STARTED -> scheme.onPrimaryContainer
        SoundCheckStatus.LISTENING -> scheme.onPrimary
        SoundCheckStatus.PASSED -> scheme.onTertiary
        SoundCheckStatus.FAILED -> scheme.onError
    }
    val icon = rememberVectorPainter(
        when (status) {
            SoundCheckStatus.PASSED -> Lucide.Check
            SoundCheckStatus.FAILED -> Lucide.Ear
            else -> Lucide.Waves
        }
    )
    val confettiColors = listOf(scheme.primary, scheme.tertiary, scheme.secondaryContainer, scheme.tertiaryContainer)
    val shownClarity by animateFloatAsState(clarity, tween(durationMillis = 120), label = "clarity")
    val time by rememberLoopProgress(durationMillis = 1_600, label = "orb")
    val burst = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }

    LaunchedEffect(status) {
        when (status) {
            SoundCheckStatus.PASSED -> {
                burst.snapTo(0f)
                burst.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
            }
            SoundCheckStatus.FAILED -> shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 520
                    (-1f) at 60
                    1f at 140
                    (-0.7f) at 220
                    0.5f at 300
                    (-0.25f) at 380
                }
            )
            else -> Unit
        }
    }

    Canvas(modifier.graphicsLayer { translationX = shake.value * 16.dp.toPx() }) {
        val side = size.minDimension
        val orbRadius = side * 0.2f

        if (status == SoundCheckStatus.LISTENING) {
            repeat(RIPPLE_COUNT) { ripple ->
                val spread = (time + ripple.toFloat() / RIPPLE_COUNT) % 1f
                drawCircle(
                    color = orbColor.copy(alpha = (1 - spread) * 0.5f),
                    radius = orbRadius + spread * side * 0.28f,
                    style = Stroke(width = side * 0.012f)
                )
            }
        }
        drawCircle(orbColor.copy(alpha = 0.22f), radius = orbRadius + side * 0.03f + shownClarity * side * 0.16f)

        val breath = if (status == SoundCheckStatus.NOT_STARTED) 1f + 0.05f * sin(2 * PI.toFloat() * time) else 1f
        drawCircle(orbColor, radius = orbRadius * breath)
        drawIcon(icon, center, side * 0.17f * breath, iconTint)

        val progress = burst.value
        if (progress > 0f && progress < 1f) {
            repeat(CONFETTI_COUNT) { piece ->
                val degrees = piece * 360f / CONFETTI_COUNT + (piece % 3) * 9f
                val radians = degrees * PI.toFloat() / 180f
                val distance = orbRadius + progress * side * (0.2f + (piece % 4) * 0.02f)
                val at = center + Offset(cos(radians), sin(radians)) * distance
                val pieceSize = side * 0.032f
                rotate(degrees = degrees + progress * 540f, pivot = at) {
                    drawRoundRect(
                        color = confettiColors[piece % confettiColors.size].copy(alpha = 1 - progress),
                        topLeft = at - Offset(pieceSize / 2, pieceSize / 4),
                        size = Size(pieceSize, pieceSize / 2),
                        cornerRadius = CornerRadius(pieceSize / 4)
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SoundCheckIllustrationPreview() {
    var status by remember { mutableStateOf(SoundCheckStatus.LISTENING) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_200.milliseconds)
            status = SoundCheckStatus.PASSED
            delay(1_800.milliseconds)
            status = SoundCheckStatus.LISTENING
            delay(1_200.milliseconds)
            status = SoundCheckStatus.FAILED
            delay(1_800.milliseconds)
            status = SoundCheckStatus.LISTENING
        }
    }

    NimbatTheme {
        SoundCheckIllustration(
            status = status,
            clarity = if (status == SoundCheckStatus.PASSED) 1f else 0.5f,
            modifier = Modifier.size(280.dp).background(MaterialTheme.colorScheme.background)
        )
    }
}
