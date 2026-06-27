package com.cappielloantonio.tempo.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.components.TempusImage
import com.cappielloantonio.tempo.ui.components.TempusImageType
import com.cappielloantonio.tempo.viewmodel.PlayerUiState

@Composable
fun PlayerQueueScreen(
    uiState: PlayerUiState,
    currentSongId: String?,
    isPlaying: Boolean,
    onSongClick: (Int) -> Unit,
    onRemoveClick: (Int) -> Unit,
    onShuffleClick: () -> Unit,
    onClearClick: () -> Unit,
    onSaveToPlaylistClick: () -> Unit,
    onDownloadAllClick: () -> Unit,
    onLoadQueueClick: () -> Unit,
    isSyncEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var isFabMenuOpen by remember { mutableStateOf(false) }

    val songs = uiState.queue

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            itemsIndexed(songs) { index, song ->
                val isCurrent = song.id == currentSongId
                QueueItem(
                    song = song,
                    isCurrent = isCurrent,
                    isPlaying = isCurrent && isPlaying,
                    onClick = { onSongClick(index) },
                    onRemove = { onRemoveClick(index) }
                )
            }
        }

        // FAB Menu
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isFabMenuOpen) {
                if (isSyncEnabled) {
                    ExtendedFloatingActionButton(
                        onClick = { onLoadQueueClick(); isFabMenuOpen = false },
                        icon = { Icon(Icons.Default.CloudDownload, null) },
                        text = { Text(stringResource(R.string.player_queue_load_queue)) }
                    )
                }
                ExtendedFloatingActionButton(
                    onClick = { onSaveToPlaylistClick(); isFabMenuOpen = false },
                    icon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) },
                    text = { Text(stringResource(R.string.player_queue_save_to_playlist)) }
                )
                ExtendedFloatingActionButton(
                    onClick = { onDownloadAllClick(); isFabMenuOpen = false },
                    icon = { Icon(Icons.Default.Download, null) },
                    text = { Text(stringResource(R.string.song_bottom_sheet_download)) }
                )
                ExtendedFloatingActionButton(
                    onClick = { onClearClick(); isFabMenuOpen = false },
                    icon = { Icon(Icons.Default.ClearAll, null) },
                    text = { Text(stringResource(R.string.player_queue_clean_all_button)) }
                )
                ExtendedFloatingActionButton(
                    onClick = { onShuffleClick(); isFabMenuOpen = false },
                    icon = { Icon(Icons.Default.Shuffle, null) },
                    text = { Text(stringResource(R.string.content_description_shuffle_button)) }
                )
            }

            FloatingActionButton(
                onClick = { isFabMenuOpen = !isFabMenuOpen },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = if (isFabMenuOpen) Icons.Default.Close else Icons.Default.Menu,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun QueueItem(
    song: Child,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = song.title ?: "",
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = song.artist ?: "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            TempusImage(
                coverArtId = song.coverArtId,
                imageType = TempusImageType.Song,
                modifier = Modifier.size(40.dp)
            )
        },
        trailingContent = {
            if (isCurrent && isPlaying) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                IconButton(onClick = onRemove) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null)
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
        )
    )
}
