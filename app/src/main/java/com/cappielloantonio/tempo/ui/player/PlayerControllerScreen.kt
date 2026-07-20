package com.cappielloantonio.tempo.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.viewmodel.PlayerUiState

@Composable
fun PlayerControllerScreen(
    uiState: PlayerUiState,
    isPlaying: Boolean,
    playbackState: Int,
    progressController: PlayerProgressController,
    progressRefreshToken: Int,
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
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
    onQueueClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onTrackInfoClick: () -> Unit,
    onTitleClick: () -> Unit,
    onArtistClick: () -> Unit,
    onChipClick: (String, String) -> Unit,
    onChipLongClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasCurrentSong = uiState.currentSong != null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Metadata
        Text(
            text = uiState.currentSong?.title ?: "",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTitleClick() }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = uiState.currentSong?.displayArtist ?: uiState.currentSong?.artist ?: "",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onArtistClick)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = uiState.currentSong?.album ?: "",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTitleClick() } // onTitleClick navigates to album
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Progress
        PlayerProgressBar(
            controller = progressController,
            isPlaying = isPlaying,
            playbackState = playbackState,
            mediaId = uiState.currentSong?.id,
            refreshToken = progressRefreshToken,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Playback Controls
        PlaybackControls(
            isPlaying = isPlaying,
            isPlayPauseEnabled = isPlayPauseEnabled,
            isPreviousEnabled = isPreviousEnabled,
            isNextEnabled = isNextEnabled,
            onPlayPauseClick = onPlayPauseClick,
            onPreviousClick = onPreviousClick,
            onNextClick = onNextClick,
            onShuffleClick = onShuffleClick,
            onRepeatClick = onRepeatClick,
            shuffleModeEnabled = shuffleModeEnabled,
            repeatMode = repeatMode
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Actions Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = onFavoriteClick,
                enabled = hasCurrentSong
            ) {
                Icon(
                    imageVector = if (uiState.currentSong?.starred != null) {
                        Icons.Default.Favorite
                    } else {
                        Icons.Default.FavoriteBorder
                    },
                    contentDescription = null,
                    tint = when {
                        uiState.currentSong?.starred != null -> MaterialTheme.colorScheme.primary
                        hasCurrentSong -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
            IconButton(onClick = onPlaybackSpeedClick) {
                Icon(imageVector = Icons.Default.Speed, contentDescription = null)
            }
            IconButton(onClick = onSleepTimerClick) {
                Icon(imageVector = Icons.Default.Timer, contentDescription = null)
            }
            IconButton(onClick = onEqualizerClick) {
                Icon(imageVector = Icons.Default.Equalizer, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = onQueueClick) {
                Icon(imageVector = Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null)
            }
            IconButton(onClick = onLyricsClick) {
                Icon(imageVector = Icons.Default.Lyrics, contentDescription = null)
            }
            IconButton(onClick = onTrackInfoClick) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        SimpleRatingBar(
            rating = uiState.currentSong?.userRating ?: 0,
            enabled = hasCurrentSong,
            onRatingChange = onRatingChange
        )
    }
}
