package com.cappielloantonio.tempo.ui.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.components.TempusImage
import com.cappielloantonio.tempo.ui.components.TempusImageType
import com.cappielloantonio.tempo.viewmodel.PlaylistPageUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistPageScreen(
    uiState: PlaylistPageUiState,
    searchQuery: String,
    currentSongId: String?,
    isPlaying: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSongClick: (Int) -> Unit,
    onSongLongClick: (Child, Int) -> Unit,
    onPlayAllClick: () -> Unit,
    onShuffleAllClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val filteredSongs = remember(uiState.songs, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.songs
        } else {
            uiState.songs.filter { it.title?.contains(searchQuery, ignoreCase = true) == true }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.playlist?.name ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onEditClick) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading && uiState.songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    PlaylistHeader(
                        uiState = uiState,
                        searchQuery = searchQuery,
                        onSearchQueryChange = onSearchQueryChange,
                        onPlayAllClick = onPlayAllClick,
                        onShuffleAllClick = onShuffleAllClick
                    )
                }

                if (filteredSongs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = stringResource(id = R.string.player_queue_empty))
                        }
                    }
                } else {
                    itemsIndexed(filteredSongs) { index, song ->
                        SongListItem(
                            song = song,
                            isCurrent = song.id == currentSongId,
                            isPlaying = isPlaying && song.id == currentSongId,
                            onClick = { onSongClick(uiState.songs.indexOfFirst { it.id == song.id }.coerceAtLeast(index)) },
                            onLongClick = { onSongLongClick(song, uiState.songs.indexOfFirst { it.id == song.id }.coerceAtLeast(index)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistHeader(
    uiState: PlaylistPageUiState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onPlayAllClick: () -> Unit,
    onShuffleAllClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TempusImage(
            coverArtId = uiState.playlist?.coverArtId,
            imageType = TempusImageType.Playlist,
            modifier = Modifier
                .size(200.dp)
                .clip(MaterialTheme.shapes.large)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = uiState.playlist?.owner ?: "",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${uiState.songs.size} tracks • ${uiState.playlist?.duration?.let { it / 60 } ?: 0} min",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            },
            placeholder = {
                Text(text = stringResource(id = R.string.search_hint))
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onPlayAllClick) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(id = R.string.playlist_page_play_button))
            }
            OutlinedButton(onClick = onShuffleAllClick) {
                Icon(imageVector = Icons.Default.Shuffle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(id = R.string.playlist_page_shuffle_button))
            }
        }
    }
}

@Composable
fun SongListItem(
    song: Child,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
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
                text = "${song.artist ?: ""} • ${song.album ?: ""}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            TempusImage(
                coverArtId = song.coverArtId,
                imageType = TempusImageType.Song,
                modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.small)
            )
        },
        trailingContent = {
            IconButton(onClick = onLongClick) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
            }
        }
    )
}
