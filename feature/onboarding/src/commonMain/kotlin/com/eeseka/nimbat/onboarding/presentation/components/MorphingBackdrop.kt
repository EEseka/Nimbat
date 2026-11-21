package com.eeseka.nimbat.onboarding.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.graphics.shapes.Morph
import com.eeseka.nimbat.onboarding.presentation.components.illustrations.util.rememberLoopProgress
import kotlin.math.abs

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MorphingBackdrop(
    pageIndex: Int,
    pageOffset: () -> Float,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val colors = listOf(scheme.primaryContainer, scheme.tertiaryContainer, scheme.secondaryContainer).map { it.copy(alpha = 0.22f) }
    val shapes = remember {
        listOf(
            MaterialShapes.Sunny,
            MaterialShapes.Cookie9Sided,
            MaterialShapes.SoftBurst,
            MaterialShapes.Cookie12Sided,
            MaterialShapes.Burst,
            MaterialShapes.Clover8Leaf
        )
    }
    val own = shapes[pageIndex % shapes.size]
    val toNext = remember(pageIndex) { Morph(own, shapes[(pageIndex + 1) % shapes.size]) }
    val toPrevious = remember(pageIndex) { Morph(own, shapes[(pageIndex - 1).mod(shapes.size)]) }
    val path = remember { Path() }
    val turn by rememberLoopProgress(durationMillis = 40_000, label = "backdrop")

    Canvas(modifier) {
        val offset = pageOffset().coerceIn(-1f, 1f)
        val progress = abs(offset)
        val morph = if (offset >= 0) toNext else toPrevious
        val neighbour = if (offset >= 0) pageIndex + 1 else pageIndex - 1
        morph.toPath(progress = progress, path = path)
        val color = lerp(colors[pageIndex % colors.size], colors[neighbour.mod(colors.size)], progress)
        val side = size.minDimension

        rotate(degrees = turn * 360f + offset * 120f) {
            translate(left = (size.width - side) / 2, top = (size.height - side) / 2) {
                scale(scale = side, pivot = Offset.Zero) {
                    drawPath(path, color)
                }
            }
        }
    }
}
