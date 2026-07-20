package com.cappielloantonio.tempo.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    onSaveQueueClick: () -> Unit,
    isSyncEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var isFabMenuOpen by remember { mutableStateOf(false) }

    val songs = remember(uiState.queue) {
        uiState.queue.mapIndexedNotNull { index, song ->
            if (song.title.isNullOrBlank() &&
                song.artist.isNullOrBlank() &&
                song.coverArtId.isNullOrBlank()
            ) {
                null
            } else {
                IndexedValue(index, song)
            }
        }
    }
    val currentQueueIndex = uiState.queue.indexOfFirst { it.id == currentSongId }
    val currentVisibleIndex = songs.indexOfFirst { it.value.id == currentSongId }
    val hasSongs = songs.isNotEmpty()
    val hasUpcomingSongs =
        currentQueueIndex >= 0 && currentQueueIndex < uiState.queue.lastIndex

    LaunchedEffect(currentVisibleIndex, songs.size) {
        if (currentVisibleIndex >= 0) {
            listState.animateScrollToItem(currentVisibleIndex)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (hasSongs) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(
                    items = songs,
                    key = { "${it.index}:${it.value.id}" }
                ) { entry ->
                    val song = entry.value
                    val isCurrent = song.id == currentSongId
                    QueueItem(
                        song = song,
                        isCurrent = isCurrent,
                        isPlaying = isCurrent && isPlaying,
                        onClick = { onSongClick(entry.index) },
                        onRemove = { onRemoveClick(entry.index) }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.player_queue_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    QueueActionFab(
                        label = stringResource(R.string.player_queue_load_queue),
                        icon = Icons.Default.CloudDownload,
                        onClick = { onLoadQueueClick(); isFabMenuOpen = false }
                    )
                }
                QueueActionFab(
                    label = stringResource(R.string.player_queue_save_queue),
                    icon = Icons.Default.Save,
                    enabled = hasSongs,
                    onClick = { onSaveQueueClick(); isFabMenuOpen = false }
                )
                QueueActionFab(
                    label = stringResource(R.string.player_queue_save_to_playlist),
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    enabled = hasSongs,
                    onClick = { onSaveToPlaylistClick(); isFabMenuOpen = false }
                )
                QueueActionFab(
                    label = stringResource(R.string.song_bottom_sheet_download),
                    icon = Icons.Default.Download,
                    enabled = hasSongs,
                    onClick = { onDownloadAllClick(); isFabMenuOpen = false }
                )
                QueueActionFab(
                    label = stringResource(R.string.player_queue_clean_all_button),
                    icon = Icons.Default.ClearAll,
                    enabled = hasUpcomingSongs,
                    onClick = { onClearClick(); isFabMenuOpen = false }
                )
                QueueActionFab(
                    label = stringResource(R.string.content_description_shuffle_button),
                    icon = Icons.Default.Shuffle,
                    enabled = hasUpcomingSongs,
                    onClick = { onShuffleClick(); isFabMenuOpen = false }
                )
            }

            FloatingActionButton(
                onClick = { isFabMenuOpen = !isFabMenuOpen },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = if (isFabMenuOpen) Icons.Default.Close else Icons.Default.Menu,
                    contentDescription = stringResource(R.string.player_queue_toggle_fab_menu_content_description)
                )
            }
        }
    }
}

@Composable
private fun QueueActionFab(
    label: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val containerColor = if (enabled) {
        FloatingActionButtonDefaults.containerColor
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    ExtendedFloatingActionButton(
        text = { Text(label) },
        icon = { Icon(icon, null) },
        onClick = {
            if (enabled) {
                onClick()
            }
        },
        modifier = Modifier.alpha(if (enabled) 1f else 0.6f),
        containerColor = containerColor,
        contentColor = contentColorFor(containerColor)
    )
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
            if (isCurrent) {
                Icon(
                    imageVector = if (isPlaying) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.PlayArrow,
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
