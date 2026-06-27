package com.cappielloantonio.tempo.ui.album

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.ui.components.TempusImage
import com.cappielloantonio.tempo.ui.components.TempusImageType
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.viewmodel.AlbumListUiState
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AlbumListPageScreen(
    uiState: AlbumListUiState,
    title: String,
    onAlbumClick: (AlbumID3) -> Unit,
    onAlbumLongClick: (AlbumID3) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var isGridMode by rememberSaveable(uiState.preferListLayout) { mutableStateOf(!uiState.preferListLayout) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var sortOrder by rememberSaveable { mutableStateOf(Preferences.getAlbumSortOrder()) }

    val filteredAlbums = remember(uiState.albums, query, sortOrder) {
        uiState.albums
            .filter { album ->
                query.isBlank() || album.name?.contains(query, ignoreCase = true) == true
            }
            .sortedWith(albumComparator(sortOrder))
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
                    IconButton(onClick = { isGridMode = !isGridMode }) {
                        Icon(
                            imageVector = if (isGridMode) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                            contentDescription = null,
                        )
                    }
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
                                    text = { Text(text = "Sort by name") },
                                    onClick = {
                                        sortOrder = Constants.ALBUM_ORDER_BY_NAME
                                        Preferences.setAlbumSortOrder(sortOrder)
                                        sortMenuExpanded = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(text = "Most recently starred") },
                                    onClick = {
                                        sortOrder = Constants.ALBUM_ORDER_BY_MOST_RECENTLY_STARRED
                                        Preferences.setAlbumSortOrder(sortOrder)
                                        sortMenuExpanded = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(text = "Least recently starred") },
                                    onClick = {
                                        sortOrder = Constants.ALBUM_ORDER_BY_LEAST_RECENTLY_STARRED
                                        Preferences.setAlbumSortOrder(sortOrder)
                                        sortMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.isLoading && uiState.albums.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                            text = "(${filteredAlbums.size})",
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

                    if (filteredAlbums.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = stringResource(id = R.string.player_queue_empty))
                        }
                    } else if (isGridMode) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(filteredAlbums, key = { it.id.orEmpty() }) { album ->
                                AlbumGridItem(
                                    album = album,
                                    onClick = { onAlbumClick(album) },
                                    onLongClick = { onAlbumLongClick(album) },
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(filteredAlbums, key = { it.id.orEmpty() }) { album ->
                                AlbumListItem(
                                    album = album,
                                    onClick = { onAlbumClick(album) },
                                    onLongClick = { onAlbumLongClick(album) },
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
private fun AlbumGridItem(
    album: AlbumID3,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TempusImage(
            coverArtId = album.coverArtId,
            imageType = TempusImageType.Album,
            modifier = Modifier
                .size(164.dp)
                .clip(MaterialTheme.shapes.medium),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.name.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = album.artist.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumListItem(
    album: AlbumID3,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        headlineContent = {
            Text(
                text = album.name.orEmpty(),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = album.artist.orEmpty(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            TempusImage(
                coverArtId = album.coverArtId,
                imageType = TempusImageType.Album,
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.small),
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                album.songCount?.takeIf { it > 0 }?.let {
                    Text(
                        text = "$it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
            }
        },
    )
}

private fun albumComparator(sortOrder: String): Comparator<AlbumID3> {
    return when (sortOrder) {
        Constants.ALBUM_ORDER_BY_MOST_RECENTLY_STARRED -> {
            compareByDescending<AlbumID3> { it.starred ?: Date(0) }
        }
        Constants.ALBUM_ORDER_BY_LEAST_RECENTLY_STARRED -> {
            compareBy<AlbumID3> { it.starred ?: Date(0) }
        }
        else -> {
            compareBy<AlbumID3> { it.name?.lowercase().orEmpty() }
        }
    }
}
