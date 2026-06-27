package com.cappielloantonio.tempo.ui.playlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.ui.components.TempusImage
import com.cappielloantonio.tempo.ui.components.TempusImageType
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.viewmodel.PlaylistCatalogueUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistCatalogueScreen(
    uiState: PlaylistCatalogueUiState,
    title: String,
    onPlaylistClick: (Playlist) -> Unit,
    onPlaylistLongClick: (Playlist) -> Unit,
    onCreatePlaylist: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredPlaylists = remember(uiState.playlists, query) {
        uiState.playlists.filter { playlist ->
            query.isBlank() || playlist.name?.contains(query, ignoreCase = true) == true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onCreatePlaylist) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(id = R.string.menu_add_button),
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(id = R.string.menu_refresh),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.isLoading && uiState.playlists.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.generic_list_page_count, filteredPlaylists.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null)
                            },
                            placeholder = {
                                Text(text = stringResource(id = R.string.search_hint))
                            },
                        )
                    }

                    if (filteredPlaylists.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = stringResource(id = R.string.player_queue_empty))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(filteredPlaylists, key = { it.id }) { playlist ->
                                PlaylistCatalogueItem(
                                    playlist = playlist,
                                    isPinned = playlist.id in uiState.pinnedPlaylistIds,
                                    isDownloaded = playlist.id in uiState.downloadedPlaylistIds,
                                    onClick = { onPlaylistClick(playlist) },
                                    onLongClick = { onPlaylistLongClick(playlist) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistCatalogueItem(
    playlist: Playlist,
    isPinned: Boolean,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        headlineContent = {
            Text(
                text = playlist.name.orEmpty(),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = stringResource(
                        id = R.string.playlist_counted_tracks,
                        playlist.songCount,
                        MusicUtil.getReadableDurationString(playlist.duration, false),
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!playlist.owner.isNullOrBlank()) {
                    Text(
                        text = playlist.owner.orEmpty(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        leadingContent = {
            TempusImage(
                coverArtId = playlist.coverArtId,
                imageType = TempusImageType.Playlist,
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.large),
            )
        },
        trailingContent = {
            if (isPinned || isDownloaded) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPinned) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = stringResource(id = R.string.menu_pin_button),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (isPinned && isDownloaded) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (isDownloaded) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = stringResource(id = R.string.song_list_page_downloaded),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
    )
}
