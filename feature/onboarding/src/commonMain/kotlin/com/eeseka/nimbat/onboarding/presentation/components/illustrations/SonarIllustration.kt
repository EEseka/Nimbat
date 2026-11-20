package com.eeseka.nimbat.onboarding.presentation.components.illustrations

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.onboarding.presentation.components.illustrations.util.drawLaptop
import com.eeseka.nimbat.onboarding.presentation.components.illustrations.util.drawPhone
import com.eeseka.nimbat.onboarding.presentation.components.illustrations.util.drawTablet
import com.eeseka.nimbat.onboarding.presentation.components.illustrations.util.rememberLoopProgress
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val RING_COUNT = 3

private enum class Gadget { LAPTOP, PHONE, TABLET }

private val gadgets = listOf(205f to Gadget.LAPTOP, 335f to Gadget.PHONE, 90f to Gadget.TABLET)

@Composable
fun SonarIllustration(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val phase by rememberLoopProgress(durationMillis = 2_400, label = "sonar")

    Canvas(modifier) {
        val side = size.minDimension
        val innerRadius = side * 0.12f
        val outerRadius = side * 0.5f
        val ringRadii = List(RING_COUNT) { ring ->
            innerRadius + ((phase + ring.toFloat() / RING_COUNT) % 1f) * (outerRadius - innerRadius)
        }

        ringRadii.forEach { radius ->
            val spread = (radius - innerRadius) / (outerRadius - innerRadius)
            drawCircle(
                color = scheme.tertiary.copy(alpha = (1 - spread) * 0.7f),
                radius = radius,
                style = Stroke(width = side * 0.012f)
            )
        }

        val gadgetDistance = side * 0.37f
        gadgets.forEach { (degrees, gadget) ->
            val radians = degrees * PI.toFloat() / 180f
            val at = center + Offset(cos(radians), sin(radians)) * gadgetDistance
            val glow = ringRadii.maxOf { (1 - abs(it - gadgetDistance) / (side * 0.08f)).coerceIn(0f, 1f) }
            val body = lerp(scheme.outline, scheme.tertiary, glow)
            scale(scale = 1f + 0.22f * glow, pivot = at) {
                when (gadget) {
                    Gadget.LAPTOP -> drawLaptop(at, side * 0.2f, body, scheme.surface)
                    Gadget.PHONE -> drawPhone(at, side * 0.17f, body, scheme.surface)
                    Gadget.TABLET -> drawTablet(at, side * 0.19f, body, scheme.surface)
                }
            }
        }

        drawPhone(center, side * 0.3f, scheme.primary, scheme.surface)
    }
}

@PreviewLightDark
@Composable
private fun SonarIllustrationPreview() {
    NimbatTheme {
        SonarIllustration(Modifier.size(280.dp).background(MaterialTheme.colorScheme.background))
    }
}
