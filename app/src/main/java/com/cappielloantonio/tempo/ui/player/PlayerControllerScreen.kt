package com.cappielloantonio.tempo.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
    progress: Long,
    duration: Long,
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
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
    onQueueClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onTrackInfoClick: () -> Unit,
    onTitleClick: () -> Unit,
    onArtistClick: () -> Unit,
    onChipClick: (String, String) -> Unit,
    onChipLongClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
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
            modifier = Modifier.clickable { onTitleClick() }
        )
        Text(
            text = uiState.currentSong?.artist ?: "",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable { onArtistClick() }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Progress
        PlayerProgressBar(
            progress = progress,
            duration = duration,
            onValueChange = onSeek
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Playback Controls
        PlaybackControls(
            isPlaying = isPlaying,
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
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (uiState.currentSong?.starred != null) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (uiState.currentSong?.starred != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
            onRatingChange = onRatingChange
        )
    }
}
