package com.cappielloantonio.tempo.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> Carousel(
    items: List<T>,
    modifier: Modifier = Modifier,
    itemWidth: Dp = 248.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    pageSpacing: Dp = 12.dp,
    itemContent: @Composable (T) -> Unit,
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { items.size })

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        pageSize = PageSize.Fixed(itemWidth),
        contentPadding = contentPadding,
        pageSpacing = pageSpacing,
    ) { page ->
        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
        val scale = animateFloatAsState(
            targetValue = 1f - (pageOffset.coerceIn(0f, 1f) * 0.08f),
            label = "CarouselScale"
        )
        val alpha = animateFloatAsState(
            targetValue = 1f - (pageOffset.coerceIn(0f, 1f) * 0.18f),
            label = "CarouselAlpha"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                },
            contentAlignment = Alignment.Center
        ) {
            itemContent(items[page])
        }
    }
}
