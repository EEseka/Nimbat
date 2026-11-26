package com.eeseka.nimbat.wave.presentation.util

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue

/** Moves to [page] when the ViewModel changes it, and reports where a swipe settles through [onPageSettled]. */
@Composable
fun rememberSyncedPagerState(
    page: Int,
    pageCount: Int,
    onPageSettled: (Int) -> Unit
): PagerState {
    val pagerState = rememberPagerState(initialPage = page) { pageCount }
    val currentOnPageSettled by rememberUpdatedState(onPageSettled)

    LaunchedEffect(page) {
        if (pagerState.currentPage != page) pagerState.animateScrollToPage(page)
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { currentOnPageSettled(it) }
    }

    return pagerState
}

/** Shrinks and fades a page as it slides away from the middle. */
fun Modifier.pageDepth(pagerState: PagerState, page: Int): Modifier = graphicsLayer {
    val distance = pagerState.getOffsetDistanceInPages(page).absoluteValue.coerceAtMost(1f)
    val scale = lerp(1f, 0.88f, distance)
    scaleX = scale
    scaleY = scale
    alpha = lerp(1f, 0.6f, distance)
}
