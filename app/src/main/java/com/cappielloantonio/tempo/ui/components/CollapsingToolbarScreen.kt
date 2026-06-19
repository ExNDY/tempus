package com.cappielloantonio.tempo.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.ui.theme.TempusTheme

@Composable
fun CollapsingToolbarScreen(
    title: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    contentPadding: PaddingValues = PaddingValues(
        start = TempusTheme.spacing.sm,
        end = TempusTheme.spacing.sm,
        bottom = TempusTheme.spacing.sm
    ),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(TempusTheme.spacing.s),
    expandedHeaderHeight: Dp = 168.dp,
    collapsedHeaderHeight: Dp = 64.dp,
    minimizedHeaderHeight: Dp = 48.dp,
    content: LazyListScope.() -> Unit,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val phase1ScrollPx = with(density) { (expandedHeaderHeight - collapsedHeaderHeight).toPx() }
    val totalScrollPx = with(density) { (expandedHeaderHeight - minimizedHeaderHeight).toPx() }

    val listState = rememberLazyListState()
    val isHeaderItemVisible by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }

    val scrollY by remember(listState, density, expandedHeaderHeight, totalScrollPx) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) {
                listState.firstVisibleItemScrollOffset.toFloat()
            } else if (listState.firstVisibleItemIndex > 0) {
                totalScrollPx
            } else {
                0f
            }
        }
    }

    LaunchedEffect(listState.isScrollInProgress, isHeaderItemVisible) {
        if (!listState.isScrollInProgress) {
            if (isHeaderItemVisible && scrollY > 0.0001f && scrollY < totalScrollPx) {
                val targetOffset = when {
                    scrollY < phase1ScrollPx / 2f -> 0
                    scrollY < (phase1ScrollPx + totalScrollPx) / 2f -> phase1ScrollPx.toInt()
                    else -> totalScrollPx.toInt()
                }
                listState.animateScrollToItem(0, targetOffset)
            }
        }
    }

    val phase1Progress = (scrollY / phase1ScrollPx).coerceIn(0f, 1f)
    val phase2Progress = if (scrollY > phase1ScrollPx) {
        ((scrollY - phase1ScrollPx) / (totalScrollPx - phase1ScrollPx)).coerceIn(0f, 1f)
    } else {
        0f
    }
    val largeTitleAlpha = if (phase1Progress < 0.35f) {
        1f
    } else {
        (1f - ((phase1Progress - 0.35f) / 0.65f)).coerceIn(0f, 1f)
    }
    val smallTitleAlpha = if (phase2Progress > 0f) {
        (1f - (phase2Progress / 0.5f)).coerceIn(0f, 1f)
    } else {
        ((phase1Progress - 0.97f) / 0.03f).coerceIn(0f, 1f)
    }
    val buttonBackgroundAlpha = (phase2Progress * 1.5f).coerceIn(0f, 1f)
    val backButtonShadowElevation by animateDpAsState(
        targetValue = if (phase2Progress > 0.9f && smallTitleAlpha < 0.02f) 8.dp else 0.dp,
        label = "BackButtonShadowElevation"
    )

    val currentHeaderHeight = with(density) {
        when {
            scrollY <= phase1ScrollPx -> (expandedHeaderHeight.toPx() - scrollY).toDp()
            else -> {
                val phase2DistancePx = totalScrollPx - phase1ScrollPx
                val remainingPhase2Fraction = ((totalScrollPx - scrollY) / phase2DistancePx).coerceIn(0f, 1f)
                (collapsedHeaderHeight.toPx() * remainingPhase2Fraction).toDp()
            }
        }
    }.coerceAtLeast(0.dp)
    val topBarContentHeight = (collapsedHeaderHeight - with(density) {
        (scrollY - phase1ScrollPx).coerceAtLeast(0f).toDp()
    }).coerceAtLeast(minimizedHeaderHeight)
    val topBarHeight = topBarContentHeight

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(containerColor)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = contentPadding.calculateStartPadding(layoutDirection),
                end = contentPadding.calculateEndPadding(layoutDirection),
                bottom = contentPadding.calculateBottomPadding()
            ),
            verticalArrangement = verticalArrangement
        ) {
            item {
                Spacer(modifier = Modifier.height(expandedHeaderHeight))
            }
            content()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(currentHeaderHeight)
                .background(containerColor)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor.copy(alpha = largeTitleAlpha),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        start = 24.dp,
                        end = contentPadding.calculateEndPadding(layoutDirection),
                        bottom = 12.dp
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(topBarHeight)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(topBarContentHeight)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor.copy(alpha = smallTitleAlpha),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(
                            start = 64.dp,
                            end = contentPadding.calculateEndPadding(layoutDirection)
                        )
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 6.dp)
                        .size(40.dp)
                        .shadow(
                            elevation = backButtonShadowElevation,
                            shape = CircleShape,
                            clip = false
                        )
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = buttonBackgroundAlpha),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = contentColor
                        )
                    }
                }
            }
        }
    }
}
