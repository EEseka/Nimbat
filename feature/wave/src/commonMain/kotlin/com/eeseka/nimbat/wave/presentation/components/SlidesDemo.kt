package com.eeseka.nimbat.wave.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.components.illustrations.drawWave
import com.eeseka.nimbat.core.design_system.components.illustrations.rememberLoopProgress
import com.eeseka.nimbat.core.design_system.components.layouts.NimbatCard
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.core.design_system.theme.extended
import com.eeseka.nimbat.wave.presentation.mappers.getBody
import com.eeseka.nimbat.wave.presentation.mappers.getIcon
import com.eeseka.nimbat.wave.presentation.mappers.getTitle
import com.eeseka.nimbat.wave.presentation.model.WaveSlide
import com.eeseka.nimbat.wave.presentation.util.pageDepth
import com.eeseka.nimbat.wave.presentation.util.rememberSyncedPagerState
import nimbat.feature.wave.generated.resources.Res
import nimbat.feature.wave.generated.resources.slide_count
import org.jetbrains.compose.resources.stringResource

private const val SLIDE_WAVE_COUNT = 3

@Composable
fun SlidesDemo(
    slideIndex: Int,
    isCompactHeight: Boolean,
    onSlideSettled: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val slides = WaveSlide.entries
    val pagerState = rememberSyncedPagerState(page = slideIndex, pageCount = slides.size, onPageSettled = onSlideSettled)

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 24.dp),
        pageSpacing = 12.dp,
        modifier = modifier
    ) { page ->
        SlideCard(
            slide = slides[page],
            body = slides[page].getBody(),
            count = stringResource(Res.string.slide_count, page + 1, slides.size),
            isCompactHeight = isCompactHeight,
            modifier = Modifier
                .fillMaxSize()
                .pageDepth(pagerState, page)
        )
    }
}

@Composable
private fun SlideCard(
    slide: WaveSlide,
    body: String,
    count: String,
    isCompactHeight: Boolean,
    modifier: Modifier = Modifier
) {
    val sky = MaterialTheme.colorScheme.extended.skyContainer
    val onSky = MaterialTheme.colorScheme.extended.onSkyContainer

    NimbatCard(
        containerColor = sky,
        contentColor = onSky,
        modifier = modifier
    ) {
        if (isCompactHeight) {
            Row(modifier = Modifier.fillMaxSize()) {
                SlideBadge(
                    icon = slide.getIcon(),
                    color = onSky,
                    onColor = sky,
                    modifier = Modifier.weight(0.8f).fillMaxHeight()
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    SlideCount(count = count, color = onSky, modifier = Modifier.align(Alignment.End))
                    SlideWords(title = slide.getTitle(), body = body, color = onSky)
                }
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val badgeHeight = (maxHeight * 0.4f).coerceIn(112.dp, 176.dp)

                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    SlideCount(count = count, color = onSky, modifier = Modifier.align(Alignment.End))
                    SlideBadge(
                        icon = slide.getIcon(),
                        color = onSky,
                        onColor = sky,
                        modifier = Modifier
                            .height(badgeHeight)
                            .fillMaxWidth()
                    )
                    SlideWords(title = slide.getTitle(), body = body, color = onSky)
                }
            }
        }
    }
}

@Composable
private fun SlideCount(count: String, color: Color, modifier: Modifier = Modifier) {
    NimbatText(
        text = count,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        modifier = modifier
    )
}

@Composable
private fun SlideWords(title: String, body: String, color: Color) {
    Column {
        NimbatText(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )

        Spacer(modifier = Modifier.height(8.dp))

        NimbatText(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = color
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SlideBadge(
    icon: ImageVector,
    color: Color,
    onColor: Color,
    modifier: Modifier = Modifier
) {
    val pulse by rememberLoopProgress(durationMillis = 2_400, label = "slide_badge_waves")

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val badgeRadius = minOf(48.dp, maxHeight / 2.9f, maxWidth / 5.4f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val badgeRadius = badgeRadius.toPx()
            val center = Offset(size.width / 2, size.height / 2)

            repeat(SLIDE_WAVE_COUNT) { i ->
                val progress = (pulse + i.toFloat() / SLIDE_WAVE_COUNT) % 1f
                val radius = badgeRadius * (1.3f + progress * 1.4f)
                val waveColor = color.copy(alpha = (1f - progress) * 0.9f)
                drawWave(center, radius, facingDegrees = 0f, color = waveColor, strokeWidth = 3.dp.toPx(), sweepDegrees = 64f)
                drawWave(center, radius, facingDegrees = 180f, color = waveColor, strokeWidth = 3.dp.toPx(), sweepDegrees = 64f)
            }
        }

        Box(
            modifier = Modifier
                .size(badgeRadius * 2)
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = onColor,
                modifier = Modifier.size(badgeRadius)
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SlidesDemoPreview() {
    NimbatTheme {
        SlidesDemo(
            slideIndex = 3,
            isCompactHeight = false,
            onSlideSettled = {},
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .height(480.dp)
        )
    }
}
