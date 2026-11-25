package com.eeseka.nimbat.home.presentation.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun rememberCardEntrance(
    playEntrance: Boolean,
    delayMillis: Long
): Animatable<Float, AnimationVector1D> {
    val progress = remember { Animatable(if (playEntrance) 0f else 1f) }

    LaunchedEffect(Unit) {
        if (!playEntrance) return@LaunchedEffect
        delay(delayMillis.milliseconds)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    return progress
}

fun Modifier.cardEntrance(progress: () -> Float): Modifier = graphicsLayer {
    val value = progress()
    val scale = 0.8f + 0.2f * value
    scaleX = scale
    scaleY = scale
    alpha = value.coerceIn(0f, 1f)
    translationY = (1f - value) * 24.dp.toPx()
}
