package com.eeseka.nimbat.wave.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.components.illustrations.drawWave
import com.eeseka.nimbat.core.design_system.components.illustrations.rememberLoopProgress
import com.eeseka.nimbat.core.design_system.components.layouts.NimbatCard
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.components.util.AppHaptic
import com.eeseka.nimbat.core.design_system.components.util.rememberAppHaptic
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.core.design_system.theme.extended
import com.eeseka.nimbat.wave.presentation.model.BAT_X
import com.eeseka.nimbat.wave.presentation.model.BatDash
import com.eeseka.nimbat.wave.presentation.model.BatDashPhase
import com.eeseka.nimbat.wave.presentation.model.CaveLane
import com.eeseka.nimbat.wave.presentation.model.CaveRock
import kotlinx.collections.immutable.persistentListOf
import nimbat.feature.wave.generated.resources.Res
import nimbat.feature.wave.generated.resources.bat_dash_again
import nimbat.feature.wave.generated.resources.bat_dash_again_upright
import nimbat.feature.wave.generated.resources.bat_dash_best
import nimbat.feature.wave.generated.resources.bat_dash_crashed
import nimbat.feature.wave.generated.resources.bat_dash_fly_down
import nimbat.feature.wave.generated.resources.bat_dash_fly_up
import nimbat.feature.wave.generated.resources.bat_dash_ready
import nimbat.feature.wave.generated.resources.bat_dash_ready_upright
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/** Fractions of the cave's height, from the roof. */
private const val HIGH_LANE_Y = 0.27f
private const val LOW_LANE_Y = 0.73f

/** Past the middle, so a rock blocks its own lane only. */
private const val ROCK_HEIGHT = 0.52f
private const val ROCK_WIDTH = 0.24f
private const val ROCK_ALPHA = 0.55f

/** Rocks fade back while the bat waits, so the message reads on clear sky. */
private const val RESTING_ROCK_ALPHA = 0.2f

/** The rounded triangle stops short of its box, so its base runs past the cave's edge. */
private const val ROCK_OVERHANG = 0.1f
private const val CALL_WAVE_COUNT = 3

@Composable
fun BatDashDemo(
    game: BatDash,
    isScreenUpright: Boolean,
    onCaveTap: (CaveLane) -> Unit,
    onFrame: (frameTimeNanos: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val sky = MaterialTheme.colorScheme.extended.skyContainer
    val onSky = MaterialTheme.colorScheme.extended.onSkyContainer
    val hapticFeedback = rememberAppHaptic()
    val shake = remember { Animatable(0f) }
    val rockAlpha by animateFloatAsState(
        targetValue = if (game.phase == BatDashPhase.FLYING) ROCK_ALPHA else RESTING_ROCK_ALPHA,
        label = "rock_alpha"
    )

    BatDashFrames(isRunning = game.phase != BatDashPhase.READY, onFrame = onFrame)

    LaunchedEffect(game.phase) {
        if (game.phase != BatDashPhase.CRASHED) return@LaunchedEffect
        shake.snapTo(24f)
        shake.animateTo(0f, spring(dampingRatio = 0.2f, stiffness = Spring.StiffnessMedium))
    }

    NimbatCard(
        containerColor = sky,
        contentColor = onSky,
        modifier = modifier
            .padding(horizontal = 24.dp)
            .graphicsLayer { translationX = shake.value }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            game.rocks.forEach { rock ->
                Rock(
                    rock = rock,
                    color = onSky,
                    alpha = { rockAlpha },
                    caveWidth = maxWidth,
                    caveHeight = maxHeight
                )
            }

            Bat(
                lane = game.batLane,
                isCrashed = game.phase == BatDashPhase.CRASHED,
                color = onSky,
                eyeColor = sky,
                modifier = Modifier.fillMaxSize()
            )

            Column(modifier = Modifier.fillMaxSize()) {
                CaveHalf(
                    label = stringResource(Res.string.bat_dash_fly_up),
                    onClick = {
                        hapticFeedback(AppHaptic.ImpactLight)
                        onCaveTap(CaveLane.HIGH)
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
                CaveHalf(
                    label = stringResource(Res.string.bat_dash_fly_down),
                    onClick = {
                        hapticFeedback(AppHaptic.ImpactLight)
                        onCaveTap(CaveLane.LOW)
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }

            Score(
                game = game,
                color = onSky,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
            )

            GameMessage(
                game = game,
                isScreenUpright = isScreenUpright,
                color = onSky,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun BatDashFrames(isRunning: Boolean, onFrame: (Long) -> Unit) {
    val currentOnFrame by rememberUpdatedState(onFrame)

    LaunchedEffect(isRunning) {
        if (!isRunning) return@LaunchedEffect
        while (true) withFrameNanos { currentOnFrame(it) }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Rock(
    rock: CaveRock,
    color: Color,
    alpha: () -> Float,
    caveWidth: Dp,
    caveHeight: Dp
) {
    val isHanging = rock.lane == CaveLane.HIGH
    val shape = MaterialShapes.Triangle.toShape()
    val width = caveWidth * ROCK_WIDTH
    val overhang = caveHeight * ROCK_HEIGHT * ROCK_OVERHANG
    val height = caveHeight * ROCK_HEIGHT + overhang

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (rock.x * caveWidth.toPx() - width.toPx() / 2).roundToInt(),
                    y = if (isHanging) -overhang.roundToPx() else (caveHeight - height + overhang).roundToPx()
                )
            }
            .graphicsLayer {
                rotationZ = if (isHanging) 180f else 0f
                this.alpha = alpha()
            }
            .size(width = width, height = height)
            .clip(shape)
            .background(color)
    )
}

@Composable
private fun Bat(
    lane: CaveLane,
    isCrashed: Boolean,
    color: Color,
    eyeColor: Color,
    modifier: Modifier = Modifier
) {
    val flap by rememberLoopProgress(durationMillis = 360, label = "bat_flap")
    val call by rememberLoopProgress(durationMillis = 1_200, label = "bat_call")
    val laneY by animateFloatAsState(
        targetValue = if (lane == CaveLane.HIGH) HIGH_LANE_Y else LOW_LANE_Y,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "bat_lane"
    )
    val tilt by animateFloatAsState(
        targetValue = if (isCrashed) 150f else 0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow),
        label = "bat_tilt"
    )

    Canvas(modifier = modifier) {
        val batWidth = 88.dp.toPx()
        val center = Offset(size.width * BAT_X, size.height * laneY)

        if (!isCrashed) {
            repeat(CALL_WAVE_COUNT) { i ->
                val progress = (call + i.toFloat() / CALL_WAVE_COUNT) % 1f
                drawWave(
                    center = center,
                    radius = batWidth * (0.6f + progress * 1.2f),
                    facingDegrees = 0f,
                    color = color.copy(alpha = (1f - progress) * 0.9f),
                    strokeWidth = 3.dp.toPx(),
                    sweepDegrees = 64f
                )
            }
        }

        rotate(degrees = tilt, pivot = center) {
            drawBat(
                center = center,
                width = batWidth,
                wingLift = if (isCrashed) 0.2f else sin(flap * 2 * PI).toFloat(),
                color = color,
                eyeColor = eyeColor
            )
        }
    }
}

private fun DrawScope.drawBat(center: Offset, width: Float, wingLift: Float, color: Color, eyeColor: Color) {
    val half = width / 2
    val rightWing = Path().apply {
        moveTo(0.1f, -0.12f)
        quadraticTo(0.55f, -0.5f, 1f, -0.36f)
        quadraticTo(0.86f, -0.08f, 0.8f, 0.08f)
        quadraticTo(0.66f, -0.02f, 0.54f, 0.06f)
        quadraticTo(0.42f, -0.02f, 0.3f, 0.14f)
        quadraticTo(0.2f, 0.06f, 0.1f, 0.14f)
        close()
    }
    val wingScaleY = 0.3f + 0.7f * (wingLift + 1f) / 2f

    translate(left = center.x, top = center.y) {
        scale(scaleX = half, scaleY = half * wingScaleY, pivot = Offset.Zero) {
            drawPath(rightWing, color)
            scale(scaleX = -1f, scaleY = 1f, pivot = Offset.Zero) { drawPath(rightWing, color) }
        }

        val body = Path().apply {
            moveTo(-0.1f * half, -0.3f * half)
            lineTo(-0.04f * half, -0.16f * half)
            lineTo(0.04f * half, -0.16f * half)
            lineTo(0.1f * half, -0.3f * half)
            lineTo(0.14f * half, -0.06f * half)
            quadraticTo(0.16f * half, 0.3f * half, 0f, 0.32f * half)
            quadraticTo(-0.16f * half, 0.3f * half, -0.14f * half, -0.06f * half)
            close()
        }
        drawPath(body, color)
        drawCircle(eyeColor, radius = 0.035f * half, center = Offset(-0.05f * half, -0.06f * half))
        drawCircle(eyeColor, radius = 0.035f * half, center = Offset(0.05f * half, -0.06f * half))
    }
}

@Composable
private fun CaveHalf(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.clickable(
            interactionSource = null,
            indication = null,
            onClickLabel = label,
            role = Role.Button,
            onClick = onClick
        )
    )
}

@Composable
private fun Score(
    game: BatDash,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        NimbatText(
            text = game.score.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        if (game.bestScore > 0) {
            NimbatText(
                text = stringResource(Res.string.bat_dash_best, game.bestScore),
                style = MaterialTheme.typography.labelLarge,
                color = color
            )
        }
    }
}

@Composable
private fun GameMessage(
    game: BatDash,
    isScreenUpright: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val ready = if (isScreenUpright) Res.string.bat_dash_ready_upright else Res.string.bat_dash_ready
    val again = if (isScreenUpright) Res.string.bat_dash_again_upright else Res.string.bat_dash_again
    val message = when (game.phase) {
        BatDashPhase.READY -> GameMessageText(title = stringResource(ready), body = null)
        BatDashPhase.CRASHED -> GameMessageText(
            title = stringResource(Res.string.bat_dash_crashed),
            body = if (game.canTakeOff) stringResource(again) else null
        )
        else -> null
    }

    AnimatedContent(
        targetState = message,
        transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.8f)) togetherWith fadeOut() },
        label = "bat_dash_message",
        modifier = modifier
    ) { shown ->
        if (shown == null) return@AnimatedContent
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NimbatText(
                text = shown.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = color
            )
            shown.body?.let { body ->
                NimbatText(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = color,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

private data class GameMessageText(val title: String, val body: String?)

@PreviewLightDark
@Composable
private fun BatDashDemoPreview() {
    NimbatTheme {
        BatDashDemo(
            game = BatDash(
                phase = BatDashPhase.FLYING,
                batLane = CaveLane.LOW,
                rocks = persistentListOf(
                    CaveRock(id = 0, lane = CaveLane.HIGH, x = 0.42f),
                    CaveRock(id = 1, lane = CaveLane.LOW, x = 0.9f)
                ),
                score = 7,
                bestScore = 12
            ),
            onCaveTap = {},
            isScreenUpright = false,
            onFrame = {},
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .height(480.dp)
        )
    }
}
