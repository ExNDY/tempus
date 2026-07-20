package com.cappielloantonio.tempo.ui.artist

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.ui.components.TempusImage
import com.cappielloantonio.tempo.ui.components.TempusImageType
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.viewmodel.ArtistListUiState
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ArtistListPageScreen(
    uiState: ArtistListUiState,
    title: String,
    onArtistClick: (ArtistID3) -> Unit,
    onArtistLongClick: (ArtistID3) -> Unit,
    onRefresh: (() -> Unit)? = null,
    onNavigateBack: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var isGridMode by rememberSaveable { mutableStateOf(true) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var sortOrder by rememberSaveable { mutableStateOf(Preferences.getArtistSortOrder()) }

    val filteredArtists = remember(uiState.artists, query, sortOrder) {
        uiState.artists
            .filter { artist ->
                query.isBlank() || artist.name?.contains(query, ignoreCase = true) == true
            }
            .sortedWith(artistComparator(sortOrder))
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
                    if (onRefresh != null) {
                        IconButton(onClick = onRefresh) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(id = R.string.menu_refresh),
                            )
                        }
                    }
                    IconButton(onClick = { isGridMode = !isGridMode }) {
                        Icon(
                            imageVector = if (isGridMode) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                            contentDescription = null,
                        )
                    }
                    if (uiState.supportsSort) {
                        Box {
                            IconButton(onClick = { sortMenuExpanded = true }) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(id = R.string.menu_sort_name)) },
                                    onClick = {
                                        sortOrder = Constants.ARTIST_ORDER_BY_NAME
                                        Preferences.setArtistSortOrder(sortOrder)
                                        sortMenuExpanded = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(id = R.string.menu_sort_album_count)) },
                                    onClick = {
                                        sortOrder = Constants.ARTIST_ORDER_BY_ALBUM_COUNT
                                        Preferences.setArtistSortOrder(sortOrder)
                                        sortMenuExpanded = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(id = R.string.menu_sort_most_recently_starred)) },
                                    onClick = {
                                        sortOrder = Constants.ARTIST_ORDER_BY_MOST_RECENTLY_STARRED
                                        Preferences.setArtistSortOrder(sortOrder)
                                        sortMenuExpanded = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(id = R.string.menu_sort_least_recently_starred)) },
                                    onClick = {
                                        sortOrder = Constants.ARTIST_ORDER_BY_LEAST_RECENTLY_STARRED
                                        Preferences.setArtistSortOrder(sortOrder)
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
            uiState.isLoading && uiState.artists.isEmpty() -> {
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
                            text = "(${filteredArtists.size})",
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

                    if (filteredArtists.isEmpty()) {
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
                            items(filteredArtists, key = { it.id.orEmpty() }) { artist ->
                                ArtistGridItem(
                                    artist = artist,
                                    isDownloaded = artist.id in uiState.downloadedArtistIds || artist.name in uiState.downloadedArtistIds,
                                    onClick = { onArtistClick(artist) },
                                    onLongClick = { onArtistLongClick(artist) },
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(filteredArtists, key = { it.id.orEmpty() }) { artist ->
                                ArtistListItem(
                                    artist = artist,
                                    isDownloaded = artist.id in uiState.downloadedArtistIds || artist.name in uiState.downloadedArtistIds,
                                    onClick = { onArtistClick(artist) },
                                    onLongClick = { onArtistLongClick(artist) },
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
private fun ArtistGridItem(
    artist: ArtistID3,
    isDownloaded: Boolean,
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
            coverArtId = artist.coverArtId,
            imageType = TempusImageType.Artist,
            modifier = Modifier
                .size(164.dp)
                .clip(MaterialTheme.shapes.extraLarge),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artist.name.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (artist.albumCount > 0) {
            Text(
                text = pluralStringResource(
                    id = R.plurals.artist_album_count,
                    count = artist.albumCount,
                    artist.albumCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (artist.starred != null || isDownloaded) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (artist.starred != null) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
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
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArtistListItem(
    artist: ArtistID3,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        headlineContent = {
            Text(
                text = artist.name.orEmpty(),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            if (artist.albumCount > 0) {
                Text(
                    text = pluralStringResource(
                        id = R.plurals.artist_album_count,
                        count = artist.albumCount,
                        artist.albumCount,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        leadingContent = {
            TempusImage(
                coverArtId = artist.coverArtId,
                imageType = TempusImageType.Artist,
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.large),
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (artist.starred != null) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (isDownloaded) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = stringResource(id = R.string.song_list_page_downloaded),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
            }
        },
    )
}

private fun artistComparator(sortOrder: String): Comparator<ArtistID3> {
    return when (sortOrder) {
        Constants.ARTIST_ORDER_BY_ALBUM_COUNT -> compareByDescending { it.albumCount }
        Constants.ARTIST_ORDER_BY_MOST_RECENTLY_STARRED -> compareByDescending { it.starred ?: Date(0) }
        Constants.ARTIST_ORDER_BY_LEAST_RECENTLY_STARRED -> compareBy { it.starred ?: Date(0) }
        else -> compareBy { it.name?.lowercase().orEmpty() }
    }
}
