package com.cappielloantonio.tempo.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.viewmodel.PlayerUiState
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    isPlaying: Boolean,
    playbackState: Int,
    progressController: PlayerProgressController,
    progressRefreshToken: Int,
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
    currentSongId: String?,
    isPlayPauseEnabled: Boolean,
    isPreviousEnabled: Boolean,
    isNextEnabled: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onRatingChange: (Int) -> Unit,
    onPlaybackSpeedClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onTrackInfoClick: () -> Unit,
    onTitleClick: () -> Unit,
    onArtistClick: () -> Unit,
    onQueueSongClick: (Int) -> Unit,
    onQueueRemoveClick: (Int) -> Unit,
    onQueueShuffleClick: () -> Unit,
    onQueueClearClick: () -> Unit,
    onQueueSaveToPlaylistClick: () -> Unit,
    onQueueDownloadAllClick: () -> Unit,
    onQueueLoadQueueClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onInstantMixClick: () -> Unit,
    onSaveQueueClick: () -> Unit,
    onLyricsLineClick: (Long) -> Unit,
    onLyricsSyncToggle: () -> Unit,
    onLyricsDownloadClick: () -> Unit,
    onChipClick: (String, String) -> Unit,
    onChipLongClick: (String, String) -> Unit,
    isSyncEnabled: Boolean,
    requestedVerticalPage: Int,
    verticalPageRequestId: Int,
    requestedHorizontalPage: Int,
    horizontalPageRequestId: Int,
    isVerticalPagerDraggable: Boolean,
    backgroundModifier: Modifier = Modifier,
    coverArtModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    val verticalPagerState = rememberPagerState(pageCount = { 2 })
    val horizontalPagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(verticalPageRequestId) {
        verticalPagerState.scrollToPage(requestedVerticalPage)
    }
    LaunchedEffect(horizontalPageRequestId) {
        horizontalPagerState.scrollToPage(requestedHorizontalPage)
    }

    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
        Surface(
            modifier = Modifier
                .matchParentSize()
                .then(backgroundModifier),
            color = MaterialTheme.colorScheme.surface,
            content = {}
        )

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            VerticalPager(
                state = verticalPagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = isVerticalPagerDraggable,
            ) { verticalPage ->
                when (verticalPage) {
                    0 -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f)) {
                                HorizontalPager(
                                    state = horizontalPagerState,
                                    modifier = Modifier.fillMaxSize()
                                ) { horizontalPage ->
                                    when (horizontalPage) {
                                        0 -> PlayerCoverScreen(
                                            currentSong = uiState.currentSong,
                                            onDownloadClick = onDownloadClick,
                                            onAddToPlaylistClick = onAddToPlaylistClick,
                                            onInstantMixClick = onInstantMixClick,
                                            onSaveQueueClick = onSaveQueueClick,
                                            onLyricsClick = {
                                                coroutineScope.launch { horizontalPagerState.animateScrollToPage(1) }
                                            },
                                            isSyncEnabled = isSyncEnabled,
                                            coverArtModifier = coverArtModifier
                                        )
                                        1 -> PlayerLyricsScreen(
                                            uiState = uiState,
                                            isPlaying = isPlaying,
                                            playbackState = playbackState,
                                            progressController = progressController,
                                            progressRefreshToken = progressRefreshToken,
                                            onLineClick = onLyricsLineClick,
                                            onSyncToggle = onLyricsSyncToggle,
                                            onDownloadClick = onLyricsDownloadClick
                                        )
                                    }
                                }
                            }

                            PlayerControllerScreen(
                                uiState = uiState,
                                isPlaying = isPlaying,
                                playbackState = playbackState,
                                progressController = progressController,
                                progressRefreshToken = progressRefreshToken,
                                shuffleModeEnabled = shuffleModeEnabled,
                                repeatMode = repeatMode,
                                isPlayPauseEnabled = isPlayPauseEnabled,
                                isPreviousEnabled = isPreviousEnabled,
                                isNextEnabled = isNextEnabled,
                                onPlayPauseClick = onPlayPauseClick,
                                onPreviousClick = onPreviousClick,
                                onNextClick = onNextClick,
                                onShuffleClick = onShuffleClick,
                                onRepeatClick = onRepeatClick,
                                onFavoriteClick = onFavoriteClick,
                                onRatingChange = (onRatingChange),
                                onPlaybackSpeedClick = onPlaybackSpeedClick,
                                onSleepTimerClick = onSleepTimerClick,
                                onEqualizerClick = onEqualizerClick,
                                onQueueClick = {
                                    coroutineScope.launch { verticalPagerState.animateScrollToPage(1) }
                                },
                                onLyricsClick = {
                                    coroutineScope.launch {
                                        val target = if (horizontalPagerState.currentPage == 0) 1 else 0
                                        horizontalPagerState.animateScrollToPage(target)
                                    }
                                },
                                onTrackInfoClick = onTrackInfoClick,
                                onTitleClick = onTitleClick,
                                onArtistClick = onArtistClick,
                                onChipClick = onChipClick,
                                onChipLongClick = onChipLongClick,
                            )
                        }
                    }
                    1 -> {
                        PlayerQueueScreen(
                            uiState = uiState,
                            currentSongId = currentSongId,
                            isPlaying = isPlaying,
                            onSongClick = onQueueSongClick,
                            onRemoveClick = onQueueRemoveClick,
                            onShuffleClick = onQueueShuffleClick,
                            onClearClick = onQueueClearClick,
                            onSaveToPlaylistClick = onQueueSaveToPlaylistClick,
                            onDownloadAllClick = onQueueDownloadAllClick,
                            onLoadQueueClick = onQueueLoadQueueClick,
                            onSaveQueueClick = onSaveQueueClick,
                            isSyncEnabled = isSyncEnabled
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlayerScreenPreview() {
    val progressController = remember {
        object : PlayerProgressController {
            override fun snapshot() = PlayerProgressSnapshot(30_000L, 180_000L)
            override fun seekTo(positionMs: Long) = Unit
        }
    }
    TempusTheme {
        PlayerScreen(
            uiState = PlayerUiState(
                currentSong = Child(id = "1", title = "Title", artist = "Artist")
            ),
            isPlaying = true,
            playbackState = 1,
            progressController = progressController,
            progressRefreshToken = 0,
            shuffleModeEnabled = false,
            repeatMode = 0,
            currentSongId = "1",
            isPlayPauseEnabled = true,
            isPreviousEnabled = true,
            isNextEnabled = true,
            onPlayPauseClick = {},
            onPreviousClick = {},
            onNextClick = {},
            onShuffleClick = {},
            onRepeatClick = {},
            onFavoriteClick = {},
            onRatingChange = {},
            onPlaybackSpeedClick = {},
            onSleepTimerClick = {},
            onEqualizerClick = {},
            onTrackInfoClick = {},
            onTitleClick = {},
            onArtistClick = {},
            onQueueSongClick = {},
            onQueueRemoveClick = {},
            onQueueShuffleClick = {},
            onQueueClearClick = {},
            onQueueSaveToPlaylistClick = {},
            onQueueDownloadAllClick = {},
            onQueueLoadQueueClick = {},
            onDownloadClick = {},
            onAddToPlaylistClick = {},
            onInstantMixClick = {},
            onSaveQueueClick = {},
            onLyricsLineClick = {},
            onLyricsSyncToggle = {},
            onLyricsDownloadClick = {},
            onChipClick = { _, _ -> },
            onChipLongClick = { _, _ -> },
            isSyncEnabled = true,
            requestedVerticalPage = 0,
            verticalPageRequestId = 0,
            requestedHorizontalPage = 0,
            horizontalPageRequestId = 0,
            isVerticalPagerDraggable = true
        )
    }
}
