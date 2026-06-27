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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.ui.components.AssetLinkChips
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.PlayerUiState

@Composable
fun PlayerControllerScreen(
    uiState: PlayerUiState,
    isPlaying: Boolean,
    playbackState: Int,
    progress: Long,
    duration: Long,
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
    val mediaTypeLabel = when (uiState.currentSong?.type) {
        Constants.MEDIA_TYPE_PODCAST -> stringResource(R.string.aa_podcast)
        Constants.MEDIA_TYPE_RADIO -> stringResource(R.string.aa_radio)
        else -> stringResource(R.string.home_section_music)
    }
    val playbackStatusLabel = when {
        isPlaying -> stringResource(R.string.player_status_playing)
        playbackState == Player.STATE_BUFFERING -> stringResource(R.string.player_status_loading)
        playbackState == Player.STATE_READY -> stringResource(R.string.player_status_paused)
        else -> stringResource(R.string.widget_not_playing)
    }
    val hasCurrentSong = uiState.currentSong != null

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

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(mediaTypeLabel) },
                leadingIcon = {
                    Icon(
                        imageVector = when (uiState.currentSong?.type) {
                            Constants.MEDIA_TYPE_PODCAST -> Icons.Default.Mic
                            Constants.MEDIA_TYPE_RADIO -> Icons.Default.Radio
                            else -> Icons.Default.MusicNote
                        },
                        contentDescription = null
                    )
                }
            )
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(playbackStatusLabel) },
                leadingIcon = {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.PauseCircleFilled,
                        contentDescription = null
                    )
                }
            )
        }

        if (uiState.currentSong?.id != null || uiState.currentAlbum?.id != null || uiState.currentArtist?.id != null) {
            Spacer(modifier = Modifier.height(12.dp))
            AssetLinkChips(
                songId = uiState.currentSong?.id,
                albumId = uiState.currentAlbum?.id,
                artistId = uiState.currentArtist?.id,
                onChipClick = onChipClick,
                onChipLongClick = onChipLongClick,
            )
        }

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
            IconButton(onClick = onFavoriteClick, enabled = hasCurrentSong) {
                Icon(
                    imageVector = if (uiState.currentSong?.starred != null) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
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
