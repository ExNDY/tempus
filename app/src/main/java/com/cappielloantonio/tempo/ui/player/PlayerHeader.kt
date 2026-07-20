package com.cappielloantonio.tempo.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.components.TempusImage
import com.cappielloantonio.tempo.ui.components.TempusImageType
import java.util.Locale

@Composable
fun PlayerHeader(
    currentSong: Child?,
    description: String?,
    isPlaying: Boolean,
    playbackState: Int,
    progressController: PlayerProgressController,
    progressRefreshToken: Int,
    isNextEnabled: Boolean,
    isSeekControlsEnabled: Boolean,
    onHeaderClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeekBackClick: () -> Unit,
    onSeekForwardClick: () -> Unit,
    backgroundModifier: Modifier = Modifier,
    coverArtModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    val subtitle = currentSong?.displayArtist ?: currentSong?.artist.orEmpty()
    val bookmarkPosition = currentSong?.bookmarkPosition?.takeIf { it > 0L }
    val progressSnapshot = rememberPlayerProgressSnapshot(
        controller = progressController,
        isPlaying = isPlaying,
        playbackState = playbackState,
        mediaId = currentSong?.id,
        refreshToken = progressRefreshToken,
    )
    val progress = if (progressSnapshot.durationMs > 0L) {
        progressSnapshot.positionMs.toFloat() / progressSnapshot.durationMs.toFloat()
    } else {
        0f
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clickable(onClick = onHeaderClick),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Box {
            Surface(
                modifier = Modifier
                    .matchParentSize()
                    .then(backgroundModifier),
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                content = {}
            )

            Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TempusImage(
                    coverArtId = currentSong?.coverArtId,
                    imageType = TempusImageType.Song,
                    modifier = Modifier
                        .size(42.dp)
                        .then(coverArtModifier)
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop
                )
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = currentSong?.title ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (bookmarkPosition != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = formatHeaderDuration(bookmarkPosition),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                IconButton(onClick = onPlayPauseClick) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                }

                IconButton(
                    onClick = onNextClick,
                    enabled = isNextEnabled
                ) {
                    Icon(imageVector = Icons.Default.SkipNext, contentDescription = null)
                }
            }
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
            }
        }
    }
}

private fun formatHeaderDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    val hours = durationMs / (1000 * 60 * 60)
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
