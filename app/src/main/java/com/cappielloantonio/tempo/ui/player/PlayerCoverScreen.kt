package com.cappielloantonio.tempo.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.components.TempusImage
import com.cappielloantonio.tempo.ui.components.TempusImageType
import com.cappielloantonio.tempo.ui.theme.TempusTheme

@Composable
fun PlayerCoverScreen(
    currentSong: Child?,
    onDownloadClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onInstantMixClick: () -> Unit,
    onSaveQueueClick: () -> Unit,
    onLyricsClick: () -> Unit,
    isSyncEnabled: Boolean,
    coverArtModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    var showOverlay by remember { mutableStateOf(false) }
    val hasCurrentSong = currentSong != null

    Box(
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(1f)
            .clickable { showOverlay = !showOverlay },
        contentAlignment = Alignment.Center
    ) {
        // Localized blurred background (slightly larger than the cover)
        TempusImage(
            coverArtId = currentSong?.coverArtId,
            imageType = TempusImageType.Song,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .blur(30.dp),
            contentScale = ContentScale.Crop
        )

        // Gradient for readability (bottom fade)
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        )

        // Fixed-size foreground cover (reduced size)
        TempusImage(
            coverArtId = currentSong?.coverArtId,
            imageType = TempusImageType.Song,
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .aspectRatio(1f)
                .then(coverArtModifier)
                .shadow(16.dp, MaterialTheme.shapes.large)
                .clip(MaterialTheme.shapes.large),
            contentScale = ContentScale.Crop
        )

        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
            exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        OverlayButton(
                            icon = Icons.Default.Download,
                            enabled = hasCurrentSong,
                            onClick = {
                                showOverlay = false
                                onDownloadClick()
                            }
                        )
                        OverlayButton(
                            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                            enabled = hasCurrentSong,
                            onClick = {
                                showOverlay = false
                                onAddToPlaylistClick()
                            }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        OverlayButton(
                            icon = Icons.Default.AutoAwesome,
                            enabled = hasCurrentSong,
                            onClick = {
                                showOverlay = false
                                onInstantMixClick()
                            }
                        )
                        if (isSyncEnabled) {
                            OverlayButton(
                                icon = Icons.Default.Save,
                                enabled = hasCurrentSong,
                                onClick = {
                                    showOverlay = false
                                    onSaveQueueClick()
                                }
                            )
                        } else {
                            OverlayButton(
                                icon = Icons.Default.Lyrics,
                                onClick = {
                                    showOverlay = false
                                    onLyricsClick()
                                }
                            )
                        }
                    }

                    if (isSyncEnabled) {
                        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                            OverlayButton(
                                icon = Icons.Default.Lyrics,
                                onClick = {
                                    showOverlay = false
                                    onLyricsClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlayButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(64.dp)
            .background(
                if (enabled) {
                    Color.White.copy(alpha = 0.1f)
                } else {
                    Color.White.copy(alpha = 0.05f)
                },
                MaterialTheme.shapes.medium
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.38f),
            modifier = Modifier.size(32.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PlayerCoverScreenPreview() {
    TempusTheme {
        PlayerCoverScreen(
            currentSong = Child(
                id = "1",
                title = "Song Title",
                artist = "Artist Name",
                coverArtId = "1"
            ),
            onDownloadClick = {},
            onAddToPlaylistClick = {},
            onInstantMixClick = {},
            onSaveQueueClick = {},
            onLyricsClick = {},
            isSyncEnabled = true
        )
    }
}
