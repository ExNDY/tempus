package com.cappielloantonio.tempo.ui.player

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.components.TempusImage
import com.cappielloantonio.tempo.ui.components.TempusImageType

@Composable
fun PlayerCoverScreen(
    currentSong: Child?,
    onDownloadClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onInstantMixClick: () -> Unit,
    onSaveQueueClick: () -> Unit,
    onLyricsClick: () -> Unit,
    isSyncEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    var showOverlay by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable { showOverlay = !showOverlay }
    ) {
        TempusImage(
            coverArtId = currentSong?.coverArtId,
            imageType = TempusImageType.Song,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f)
                        )
                    )
                )
        )

        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
            exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        OverlayButton(Icons.Default.Download, onDownloadClick)
                        OverlayButton(Icons.AutoMirrored.Filled.PlaylistAdd, onAddToPlaylistClick)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        OverlayButton(Icons.Default.AutoAwesome, onInstantMixClick)
                        if (isSyncEnabled) {
                            OverlayButton(Icons.Default.Save, onSaveQueueClick)
                        } else {
                            OverlayButton(Icons.Default.Lyrics, onLyricsClick)
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
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(64.dp)
            .background(Color.White.copy(alpha = 0.1f), MaterialTheme.shapes.medium)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}
