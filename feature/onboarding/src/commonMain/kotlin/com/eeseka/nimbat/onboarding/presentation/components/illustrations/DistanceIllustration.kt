package com.eeseka.nimbat.onboarding.presentation.components.illustrations

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp as lerpOffset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eeseka.nimbat.core.design_system.components.illustrations.drawPhone
import com.eeseka.nimbat.core.design_system.components.illustrations.rememberLoopProgress
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin
import nimbat.feature.onboarding.generated.resources.Res
import nimbat.feature.onboarding.generated.resources.distance_example
import org.jetbrains.compose.resources.stringResource

private const val RIPPLE_STEPS = 48
private const val RIPPLE_CYCLES = 3

@Composable
fun DistanceIllustration(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val time by rememberLoopProgress(durationMillis = 2_600, label = "distance")
    val measurer = rememberTextMeasurer()
    val text = stringResource(Res.string.distance_example)
    val style = MaterialTheme.typography.labelMedium.copy(fontSize = 18.sp, color = scheme.onBackground)
    val label = remember(text, style) { measurer.measure(text, style) }
    val ripple = remember { Path() }

    Canvas(modifier) {
        val side = size.minDimension
        val left = Offset(center.x - side * 0.32f, center.y - side * 0.06f)
        val right = Offset(center.x + side * 0.32f, center.y - side * 0.06f)
        val isGoing = time < 0.5f
        val leg = if (isGoing) time * 2 else time * 2 - 1
        val eased = leg * leg * (3 - 2 * leg)
        val from = if (isGoing) left else right
        val to = if (isGoing) right else left
        val rippleColor = if (isGoing) scheme.tertiary else scheme.secondary

        val arrivedRight = (1 - abs(time - 0.5f) / 0.08f).coerceIn(0f, 1f)
        val arrivedLeft = (1 - min(time, 1 - time) / 0.08f).coerceIn(0f, 1f)

        val rippleCenter = lerpOffset(from, to, eased)
        val rippleLength = side * 0.24f
        val amplitude = side * 0.045f
        ripple.reset()
        repeat(RIPPLE_STEPS + 1) { step ->
            val along = step.toFloat() / RIPPLE_STEPS
            val taper = sin(PI.toFloat() * along)
            val x = rippleCenter.x + (along - 0.5f) * rippleLength
            val y = rippleCenter.y + amplitude * taper * sin(2 * PI.toFloat() * (RIPPLE_CYCLES * along - leg * 4))
            if (step == 0) ripple.moveTo(x, y) else ripple.lineTo(x, y)
        }
        drawPath(ripple, rippleColor, style = Stroke(width = side * 0.016f, cap = StrokeCap.Round))

        listOf(left to arrivedLeft, right to arrivedRight).forEach { (at, flash) ->
            scale(scale = 1f + 0.12f * flash, pivot = at) {
                drawPhone(at, side * 0.28f, lerp(scheme.primary, rippleColor, flash), scheme.surface)
            }
        }

        val tapeY = center.y + side * 0.27f
        val tapeColor = scheme.onSurfaceVariant
        val stroke = side * 0.01f
        drawLine(tapeColor, Offset(left.x, tapeY), Offset(right.x, tapeY), stroke, StrokeCap.Round)
        listOf(left.x, right.x).forEach { x ->
            drawLine(tapeColor, Offset(x, tapeY - side * 0.03f), Offset(x, tapeY + side * 0.03f), stroke, StrokeCap.Round)
        }
        val labelTopLeft = Offset(center.x - label.size.width / 2, tapeY - side * 0.04f - label.size.height)
        scale(scale = 1f + 0.15f * arrivedLeft, pivot = Offset(center.x, labelTopLeft.y + label.size.height / 2)) {
            drawText(label, topLeft = labelTopLeft)
        }
    }
}

@PreviewLightDark
@Composable
private fun DistanceIllustrationPreview() {
    NimbatTheme {
        DistanceIllustration(Modifier.size(280.dp).background(MaterialTheme.colorScheme.background))
    }
}
