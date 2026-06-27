package com.cappielloantonio.tempo.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import com.cappielloantonio.tempo.viewmodel.PlayerUiState
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    isPlaying: Boolean,
    progress: Long,
    duration: Long,
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
    currentSongId: String?,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onSeek: (Long) -> Unit,
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
    onLyricsLineClick: (Int) -> Unit,
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

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
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
                                        isSyncEnabled = isSyncEnabled
                                    )
                                    1 -> PlayerLyricsScreen(
                                        uiState = uiState,
                                        currentPosition = progress,
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
                            progress = progress,
                            duration = duration,
                            shuffleModeEnabled = shuffleModeEnabled,
                            repeatMode = repeatMode,
                            onPlayPauseClick = onPlayPauseClick,
                            onPreviousClick = onPreviousClick,
                            onNextClick = onNextClick,
                            onShuffleClick = onShuffleClick,
                            onRepeatClick = onRepeatClick,
                            onSeek = onSeek,
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
                            onChipLongClick = onChipLongClick
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
                        isSyncEnabled = isSyncEnabled
                    )
                }
            }
        }
    }
}
