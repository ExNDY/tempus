package com.cappielloantonio.tempo.ui.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.components.TempusImage
import com.cappielloantonio.tempo.ui.components.TempusImageType
import com.cappielloantonio.tempo.viewmodel.SearchUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    uiState: SearchUiState,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onRecentSearchDelete: (String) -> Unit,
    onArtistClick: (ArtistID3) -> Unit,
    onArtistLongClick: (ArtistID3) -> Unit,
    onAlbumClick: (AlbumID3) -> Unit,
    onAlbumLongClick: (AlbumID3) -> Unit,
    onSongClick: (Child) -> Unit,
    onSongLongClick: (Child) -> Unit,
    onNavigateBack: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = { value ->
                            query = value
                            onQueryChange(value)
                        },
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = ""; onQueryChange("") }) {
                                    Icon(Icons.Default.Close, null)
                                }
                            }
                        },
                        singleLine = true
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (query.isEmpty()) {
                RecentSearches(
                    searches = uiState.recentSearches,
                    onSearchClick = { value ->
                        query = value
                        onSearch(value)
                    },
                    onDeleteClick = onRecentSearchDelete
                )
            } else {
                SearchResults(
                    uiState = uiState,
                    query = query,
                    onArtistClick = onArtistClick,
                    onArtistLongClick = onArtistLongClick,
                    onAlbumClick = onAlbumClick,
                    onAlbumLongClick = onAlbumLongClick,
                    onSongClick = onSongClick,
                    onSongLongClick = onSongLongClick,
                )
            }
        }
    }
}

@Composable
private fun RecentSearches(
    searches: List<String>,
    onSearchClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    LazyColumn {
        items(searches) { search ->
            ListItem(
                modifier = Modifier.clickable { onSearchClick(search) },
                headlineContent = { Text(search) },
                leadingContent = { Icon(Icons.Default.History, null) },
                trailingContent = {
                    IconButton(onClick = { onDeleteClick(search) }) {
                        Icon(Icons.Default.Close, null)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchResults(
    uiState: SearchUiState,
    query: String,
    onArtistClick: (ArtistID3) -> Unit,
    onArtistLongClick: (ArtistID3) -> Unit,
    onAlbumClick: (AlbumID3) -> Unit,
    onAlbumLongClick: (AlbumID3) -> Unit,
    onSongClick: (Child) -> Unit,
    onSongLongClick: (Child) -> Unit,
) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val results = uiState.results ?: return
    val hasAnyResult = !results.artists.isNullOrEmpty() || !results.albums.isNullOrEmpty() || !results.songs.isNullOrEmpty()

    if (!hasAnyResult && query.length >= 3) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(id = R.string.player_queue_empty),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        return
    }

    LazyColumn {
        if (!results.artists.isNullOrEmpty()) {
            item { SectionHeader(stringResource(R.string.search_title_artist)) }
            items(results.artists!!) { artist ->
                ListItem(
                    modifier = Modifier.combinedClickable(
                        onClick = { onArtistClick(artist) },
                        onLongClick = { onArtistLongClick(artist) },
                    ),
                    headlineContent = {
                        Text(
                            text = artist.name ?: "",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingContent = {
                        TempusImage(
                            coverArtId = artist.coverArtId,
                            imageType = TempusImageType.Artist,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                )
            }
        }

        if (!results.albums.isNullOrEmpty()) {
            item { SectionHeader(stringResource(R.string.search_title_album)) }
            items(results.albums!!) { album ->
                ListItem(
                    modifier = Modifier.combinedClickable(
                        onClick = { onAlbumClick(album) },
                        onLongClick = { onAlbumLongClick(album) },
                    ),
                    headlineContent = {
                        Text(
                            text = album.name ?: "",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = album.artist ?: "",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingContent = {
                        TempusImage(
                            coverArtId = album.coverArtId,
                            imageType = TempusImageType.Album,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                )
            }
        }

        if (!results.songs.isNullOrEmpty()) {
            item { SectionHeader(stringResource(R.string.search_title_song)) }
            items(results.songs!!) { song ->
                ListItem(
                    modifier = Modifier.combinedClickable(
                        onClick = { onSongClick(song) },
                        onLongClick = { onSongLongClick(song) },
                    ),
                    headlineContent = {
                        Text(
                            text = song.title ?: "",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = song.artist ?: "",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingContent = {
                        TempusImage(
                            coverArtId = song.coverArtId,
                            imageType = TempusImageType.Song,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(16.dp)
    )
}
