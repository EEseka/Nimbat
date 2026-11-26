package com.eeseka.nimbat.core.design_system.components.illustrations

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.VectorPainter
import com.eeseka.nimbat.core.domain.util.PlatformUtils
import com.eeseka.nimbat.core.presentation.util.DeviceConfiguration

@Composable
fun rememberLoopProgress(durationMillis: Int, label: String): State<Float> =
    rememberInfiniteTransition(label = label).animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing), RepeatMode.Restart),
        label = label
    )

fun DrawScope.drawPhone(center: Offset, height: Float, body: Color, screen: Color) {
    val width = height * 0.52f
    val topLeft = Offset(center.x - width / 2, center.y - height / 2)
    drawRoundRect(body, topLeft, Size(width, height), CornerRadius(width * 0.24f))
    val inset = width * 0.1f
    drawRoundRect(
        color = screen,
        topLeft = topLeft + Offset(inset, inset * 1.8f),
        size = Size(width - 2 * inset, height - inset * 3.6f),
        cornerRadius = CornerRadius(width * 0.14f)
    )
}

fun DrawScope.drawLaptop(center: Offset, width: Float, body: Color, screen: Color) {
    val lidWidth = width * 0.84f
    val lidHeight = width * 0.56f
    val lidTopLeft = Offset(center.x - lidWidth / 2, center.y - lidHeight * 0.6f)
    drawRoundRect(body, lidTopLeft, Size(lidWidth, lidHeight), CornerRadius(width * 0.06f))
    val inset = width * 0.05f
    drawRoundRect(
        color = screen,
        topLeft = lidTopLeft + Offset(inset, inset),
        size = Size(lidWidth - 2 * inset, lidHeight - 2 * inset),
        cornerRadius = CornerRadius(width * 0.03f)
    )
    drawRoundRect(
        color = body,
        topLeft = Offset(center.x - width / 2, lidTopLeft.y + lidHeight),
        size = Size(width, width * 0.07f),
        cornerRadius = CornerRadius(width * 0.035f)
    )
}

fun DrawScope.drawTablet(center: Offset, height: Float, body: Color, screen: Color) {
    val width = height * 0.76f
    val topLeft = Offset(center.x - width / 2, center.y - height / 2)
    drawRoundRect(body, topLeft, Size(width, height), CornerRadius(width * 0.14f))
    val inset = width * 0.08f
    drawRoundRect(
        color = screen,
        topLeft = topLeft + Offset(inset, inset),
        size = Size(width - 2 * inset, height - 2 * inset),
        cornerRadius = CornerRadius(width * 0.07f)
    )
}

fun DrawScope.drawDevice(
    configuration: DeviceConfiguration,
    center: Offset,
    height: Float,
    body: Color,
    screen: Color
) = when {
    PlatformUtils.isComputer() -> drawLaptop(center, height * 1.2f, body, screen)
    configuration.isMobile -> drawPhone(center, height, body, screen)
    else -> drawTablet(center, height, body, screen)
}

fun DrawScope.drawHand(center: Offset, height: Float, color: Color) {
    val palmWidth = height * 0.5f
    val palmHeight = height * 0.46f
    val palmTop = center.y - palmHeight * 0.1f
    drawRoundRect(
        color = color,
        topLeft = Offset(center.x - palmWidth / 2, palmTop),
        size = Size(palmWidth, palmHeight),
        cornerRadius = CornerRadius(palmWidth * 0.32f)
    )
    val fingerWidth = palmWidth / 4.6f
    val fingerHeights = listOf(0.34f, 0.44f, 0.42f, 0.32f)
    fingerHeights.forEachIndexed { i, share ->
        val fingerHeight = height * share
        val left = center.x - palmWidth / 2 + i * (palmWidth - fingerWidth) / 3
        drawRoundRect(
            color = color,
            topLeft = Offset(left, palmTop - fingerHeight + palmWidth * 0.2f),
            size = Size(fingerWidth, fingerHeight),
            cornerRadius = CornerRadius(fingerWidth / 2)
        )
    }
    val thumbPivot = Offset(center.x - palmWidth / 2 + fingerWidth * 0.4f, palmTop + palmHeight * 0.55f)
    rotate(degrees = -38f, pivot = thumbPivot) {
        drawRoundRect(
            color = color,
            topLeft = Offset(thumbPivot.x - fingerWidth / 2, thumbPivot.y - height * 0.3f),
            size = Size(fingerWidth, height * 0.3f),
            cornerRadius = CornerRadius(fingerWidth / 2)
        )
    }
}

fun DrawScope.drawWave(
    center: Offset,
    radius: Float,
    facingDegrees: Float,
    color: Color,
    strokeWidth: Float,
    sweepDegrees: Float = 70f
) {
    drawArc(
        color = color,
        startAngle = facingDegrees - sweepDegrees / 2,
        sweepAngle = sweepDegrees,
        useCenter = false,
        topLeft = Rect(center, radius).topLeft,
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

fun DrawScope.drawIcon(icon: VectorPainter, center: Offset, size: Float, tint: Color) {
    translate(left = center.x - size / 2, top = center.y - size / 2) {
        with(icon) { draw(Size(size, size), colorFilter = ColorFilter.tint(tint)) }
    }
}
