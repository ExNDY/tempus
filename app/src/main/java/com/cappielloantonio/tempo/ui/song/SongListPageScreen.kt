package com.cappielloantonio.tempo.ui.song

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.components.TempusImage
import com.cappielloantonio.tempo.ui.components.TempusImageType
import com.cappielloantonio.tempo.viewmodel.SongListUiState
import java.util.Date

enum class SongListSort {
    TITLE,
    MOST_RECENTLY_STARRED,
    LEAST_RECENTLY_STARRED,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongListPageScreen(
    uiState: SongListUiState,
    currentSongId: String?,
    isPlaying: Boolean,
    onSongClick: (List<Child>, Int) -> Unit,
    onSongLongClick: (Child, Int) -> Unit,
    onPlayAllClick: (List<Child>) -> Unit,
    onShuffleAllClick: (List<Child>) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedSort by rememberSaveable { mutableStateOf(SongListSort.TITLE) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val filteredSongs = remember(uiState.songs, query, selectedSort) {
        uiState.songs
            .filter { song ->
                query.isBlank() || listOf(song.title, song.artist, song.album)
                    .any { value -> value?.contains(query, ignoreCase = true) == true }
            }
            .sortedWith(sortComparator(selectedSort))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = uiState.title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (uiState.supportsSort) {
                        Box {
                            IconButton(onClick = { sortMenuExpanded = true }) {
                                Icon(imageVector = Icons.Default.SortByAlpha, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(text = "Sort by title") },
                                    onClick = {
                                        selectedSort = SongListSort.TITLE
                                        sortMenuExpanded = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(text = "Most recently starred") },
                                    onClick = {
                                        selectedSort = SongListSort.MOST_RECENTLY_STARRED
                                        sortMenuExpanded = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(text = "Least recently starred") },
                                    onClick = {
                                        selectedSort = SongListSort.LEAST_RECENTLY_STARRED
                                        sortMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    IconButton(onClick = { onPlayAllClick(filteredSongs) }) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    }
                    IconButton(onClick = { onShuffleAllClick(filteredSongs) }) {
                        Icon(imageVector = Icons.Default.Shuffle, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.isLoading && uiState.songs.isEmpty() -> {
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
                    SongListHeader(
                        subtitle = uiState.subtitle,
                        query = query,
                        onQueryChange = { query = it },
                        onPlayAllClick = { onPlayAllClick(filteredSongs) },
                        onShuffleAllClick = { onShuffleAllClick(filteredSongs) },
                    )

                    if (filteredSongs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = stringResource(id = R.string.player_queue_empty))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp),
                        ) {
                            itemsIndexed(filteredSongs, key = { _, song -> song.id }) { _, song ->
                                val originalIndex = uiState.songs.indexOfFirst { it.id == song.id }
                                SongListItem(
                                    song = song,
                                    isCurrent = song.id == currentSongId,
                                    isPlaying = isPlaying && song.id == currentSongId,
                                    onClick = {
                                        if (originalIndex >= 0) {
                                            onSongClick(uiState.songs, originalIndex)
                                        }
                                    },
                                    onLongClick = {
                                        if (originalIndex >= 0) {
                                            onSongLongClick(song, originalIndex)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongListHeader(
    subtitle: String?,
    query: String,
    onQueryChange: (String) -> Unit,
    onPlayAllClick: () -> Unit,
    onShuffleAllClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            },
            placeholder = {
                Text(text = stringResource(id = R.string.search_hint))
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
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
private fun SongListItem(
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
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = buildString {
                    append(song.artist.orEmpty())
                    if (!song.album.isNullOrBlank()) {
                        append(" • ")
                        append(song.album)
                    }
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            TempusImage(
                coverArtId = song.coverArtId,
                imageType = TempusImageType.Song,
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small),
            )
        },
        trailingContent = {
            if (isCurrent && isPlaying) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
            } else {
                IconButton(onClick = onLongClick) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
                }
            }
        },
    )
}

private fun sortComparator(sort: SongListSort): Comparator<Child> {
    return when (sort) {
        SongListSort.TITLE -> compareBy { it.title?.lowercase().orEmpty() }
        SongListSort.MOST_RECENTLY_STARRED -> compareByDescending { it.starred ?: Date(0) }
        SongListSort.LEAST_RECENTLY_STARRED -> compareBy { it.starred ?: Date(0) }
    }
}
