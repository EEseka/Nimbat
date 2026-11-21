package com.eeseka.nimbat.onboarding.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import kotlin.math.abs
import nimbat.feature.onboarding.generated.resources.Res
import nimbat.feature.onboarding.generated.resources.page_of
import org.jetbrains.compose.resources.stringResource

private val DOT_SIZE = 12.dp
private val PILL_WIDTH = 32.dp
private val DOT_GAP = 8.dp

@Composable
fun OnboardingPageIndicator(
    pageCount: Int,
    currentPage: Int,
    pagerPosition: () -> Float,
    modifier: Modifier = Modifier
) {
    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val description = stringResource(Res.string.page_of, currentPage + 1, pageCount)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(DOT_SIZE)
            .semantics { contentDescription = description }
    ) {
        val dot = DOT_SIZE.toPx()
        val pill = PILL_WIDTH.toPx()
        val gap = DOT_GAP.toPx()
        val position = pagerPosition()
        val totalWidth = dot * pageCount + (pill - dot) + gap * (pageCount - 1)
        var x = (size.width - totalWidth) / 2
        repeat(pageCount) { page ->
            val closeness = (1 - abs(position - page)).coerceIn(0f, 1f)
            val width = dot + (pill - dot) * closeness
            drawRoundRect(
                color = lerp(inactive, active, closeness),
                topLeft = Offset(x, 0f),
                size = Size(width, dot),
                cornerRadius = CornerRadius(dot / 2)
            )
            x += width + gap
        }
    }
}

@PreviewLightDark
@Composable
private fun OnboardingPageIndicatorPreview() {
    NimbatTheme {
        OnboardingPageIndicator(
            pageCount = 6,
            currentPage = 1,
            pagerPosition = { 1.3f },
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
        )
    }
}
