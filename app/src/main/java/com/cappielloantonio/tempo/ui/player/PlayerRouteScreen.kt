package com.cappielloantonio.tempo.ui.player

import android.os.Bundle
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.cappielloantonio.tempo.navigation.BottomMenuConfig
import com.cappielloantonio.tempo.navigation.Screen
import com.cappielloantonio.tempo.util.Preferences

object PlayerRouteScreen : Screen.DefaultScreen {
    override val screenName: String = "player"

    override fun bottomMenuConfig(): BottomMenuConfig = BottomMenuConfig.Hidden

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Composable
    override fun Content(navController: NavController, args: Bundle?) {
        val state = LocalPlayerChromeState.current ?: return
        val actions = LocalPlayerChromeActions.current ?: return
        val progressController = LocalPlayerProgressController.current ?: return
        val sharedTransitionScope = LocalPlayerSharedTransitionScope.current
        val animatedVisibilityScope = LocalPlayerAnimatedVisibilityScope.current

        var requestedVerticalPage by remember { mutableIntStateOf(0) }
        var verticalPageRequestId by remember { mutableIntStateOf(0) }
        var requestedHorizontalPage by remember { mutableIntStateOf(0) }
        var horizontalPageRequestId by remember { mutableIntStateOf(0) }
        var isVerticalPagerDraggable by remember { mutableStateOf(true) }

        val backgroundModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
            with(sharedTransitionScope) {
                Modifier.sharedBounds(
                    sharedContentState = rememberSharedContentState(PlayerSharedContentKeys.Background),
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }
        } else {
            Modifier
        }

        val coverArtModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
            with(sharedTransitionScope) {
                Modifier.sharedElement(
                    sharedContentState = rememberSharedContentState(PlayerSharedContentKeys.CoverArt),
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }
        } else {
            Modifier
        }

        PlayerScreen(
            uiState = state.uiState,
            isPlaying = state.playbackSnapshot.isPlaying,
            playbackState = state.transport.playbackState,
            progressController = progressController,
            progressRefreshToken = state.progressRefreshToken,
            shuffleModeEnabled = state.transport.shuffleModeEnabled,
            repeatMode = state.transport.repeatMode,
            currentSongId = state.playbackSnapshot.currentSongId,
            isPlayPauseEnabled = state.transport.isPlayPauseEnabled,
            isPreviousEnabled = state.transport.isPreviousEnabled,
            isNextEnabled = state.transport.isNextEnabled,
            onPlayPauseClick = actions.onPlayPauseClick,
            onPreviousClick = actions.onPreviousClick,
            onNextClick = actions.onNextClick,
            onShuffleClick = actions.onShuffleClick,
            onRepeatClick = actions.onRepeatClick,
            onFavoriteClick = actions.onFavoriteClick,
            onRatingChange = actions.onRatingChange,
            onPlaybackSpeedClick = actions.onPlaybackSpeedClick,
            onSleepTimerClick = actions.onSleepTimerClick,
            onEqualizerClick = actions.onEqualizerClick,
            onTrackInfoClick = actions.onTrackInfoClick,
            onTitleClick = actions.onTitleClick,
            onArtistClick = actions.onArtistClick,
            onQueueSongClick = actions.onQueueSongClick,
            onQueueRemoveClick = actions.onQueueRemoveClick,
            onQueueShuffleClick = actions.onQueueShuffleClick,
            onQueueClearClick = actions.onQueueClearClick,
            onQueueSaveToPlaylistClick = actions.onQueueSaveToPlaylistClick,
            onQueueDownloadAllClick = actions.onQueueDownloadAllClick,
            onQueueLoadQueueClick = actions.onQueueLoadQueueClick,
            onDownloadClick = actions.onDownloadClick,
            onAddToPlaylistClick = actions.onAddToPlaylistClick,
            onInstantMixClick = actions.onInstantMixClick,
            onSaveQueueClick = actions.onSaveQueueClick,
            onLyricsLineClick = actions.onLyricsLineClick,
            onLyricsSyncToggle = actions.onLyricsSyncToggle,
            onLyricsDownloadClick = actions.onLyricsDownloadClick,
            onChipClick = actions.onChipClick,
            onChipLongClick = actions.onChipLongClick,
            isSyncEnabled = Preferences.isSyncronizationEnabled(),
            requestedVerticalPage = requestedVerticalPage,
            verticalPageRequestId = verticalPageRequestId,
            requestedHorizontalPage = requestedHorizontalPage,
            horizontalPageRequestId = horizontalPageRequestId,
            isVerticalPagerDraggable = isVerticalPagerDraggable,
            backgroundModifier = backgroundModifier,
            coverArtModifier = coverArtModifier,
        )
    }
}
