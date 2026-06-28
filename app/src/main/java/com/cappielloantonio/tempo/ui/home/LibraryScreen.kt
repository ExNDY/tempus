package com.cappielloantonio.tempo.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Genre
import com.cappielloantonio.tempo.subsonic.models.MusicFolder
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.ui.components.SectionCard
import com.cappielloantonio.tempo.ui.components.TempusImage
import com.cappielloantonio.tempo.ui.components.TempusImageType
import com.cappielloantonio.tempo.viewmodel.LibraryUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    onNavigateBack: () -> Unit,
    onRefreshAll: () -> Unit,
    onMusicFolderClick: (MusicFolder) -> Unit,
    onAlbumClick: (AlbumID3) -> Unit,
    onAlbumLongClick: (AlbumID3) -> Unit,
    onArtistClick: (ArtistID3) -> Unit,
    onArtistLongClick: (ArtistID3) -> Unit,
    onGenreClick: (Genre) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onPlaylistLongClick: (Playlist) -> Unit,
    onSeeAllAlbumsClick: () -> Unit,
    onSeeAllArtistsClick: () -> Unit,
    onSeeAllGenresClick: () -> Unit,
    onSeeAllPlaylistsClick: () -> Unit,
    onRefreshAlbums: () -> Unit,
    onRefreshArtists: () -> Unit,
    onRefreshGenres: () -> Unit,
    onRefreshPlaylists: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.menu_library_label)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onRefreshAll) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isLoading && uiState.musicFolders.isEmpty() && uiState.albums.isEmpty() && uiState.artists.isEmpty() && uiState.genres.isEmpty() && uiState.playlists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (uiState.musicFolders.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.library_title_music_folder),
                            onRefresh = null,
                            onSeeAll = null,
                        )
                    }
                    items(uiState.musicFolders, key = { it.id.orEmpty() }) { folder ->
                        SectionCard {
                            ListItem(
                                headlineContent = { Text(folder.name.orEmpty()) },
                                modifier = Modifier.combinedClickable(onClick = { onMusicFolderClick(folder) }),
                            )
                        }
                    }
                }

                if (uiState.albums.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.library_title_album),
                            onRefresh = onRefreshAlbums,
                            onSeeAll = onSeeAllAlbumsClick,
                            seeAllLabel = stringResource(R.string.library_title_album_see_all_button),
                        )
                    }
                    item {
                        LibraryCarousel(
                            items = uiState.albums,
                            title = { it.name.orEmpty() },
                            subtitle = { it.artist.orEmpty() },
                            imageType = TempusImageType.Album,
                            coverArtId = { it.coverArtId },
                            onClick = onAlbumClick,
                            onLongClick = onAlbumLongClick,
                        )
                    }
                }

                if (uiState.artists.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.library_title_artist),
                            onRefresh = onRefreshArtists,
                            onSeeAll = onSeeAllArtistsClick,
                            seeAllLabel = stringResource(R.string.library_title_artist_see_all_button),
                        )
                    }
                    item {
                        LibraryCarousel(
                            items = uiState.artists,
                            title = { it.name.orEmpty() },
                            subtitle = { "" },
                            imageType = TempusImageType.Artist,
                            coverArtId = { it.coverArtId },
                            onClick = onArtistClick,
                            onLongClick = onArtistLongClick,
                        )
                    }
                }

                if (uiState.genres.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.library_title_genre),
                            onRefresh = onRefreshGenres,
                            onSeeAll = onSeeAllGenresClick,
                            seeAllLabel = stringResource(R.string.library_title_genre_see_all_button),
                        )
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(uiState.genres, key = { it.genre.orEmpty() }) { genre ->
                                SectionCard(
                                    modifier = Modifier
                                        .width(160.dp)
                                        .combinedClickable(onClick = { onGenreClick(genre) }),
                                ) {
                                    Text(
                                        text = genre.genre.orEmpty(),
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.playlists.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.library_title_playlist),
                            onRefresh = onRefreshPlaylists,
                            onSeeAll = onSeeAllPlaylistsClick,
                            seeAllLabel = stringResource(R.string.library_title_playlist_see_all_button),
                        )
                    }
                    item {
                        LibraryCarousel(
                            items = uiState.playlists,
                            title = { it.name.orEmpty() },
                            subtitle = { "" },
                            imageType = TempusImageType.Playlist,
                            coverArtId = { it.coverArtId },
                            onClick = onPlaylistClick,
                            onLongClick = onPlaylistLongClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onRefresh: (() -> Unit)?,
    onSeeAll: (() -> Unit)?,
    seeAllLabel: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        if (onRefresh != null) {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = null)
            }
        }
        if (onSeeAll != null) {
            Text(
                text = seeAllLabel ?: stringResource(R.string.common_see_all),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.combinedClickable(onClick = onSeeAll),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T> LibraryCarousel(
    items: List<T>,
    title: (T) -> String,
    subtitle: (T) -> String,
    imageType: TempusImageType,
    coverArtId: (T) -> String?,
    onClick: (T) -> Unit,
    onLongClick: (T) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = { title(it) + subtitle(it) }) { item ->
            Column(
                modifier = Modifier
                    .width(152.dp)
                    .combinedClickable(
                        onClick = { onClick(item) },
                        onLongClick = { onLongClick(item) },
                    ),
            ) {
                TempusImage(
                    coverArtId = coverArtId(item),
                    imageType = imageType,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(
                            if (imageType == TempusImageType.Artist) {
                                MaterialTheme.shapes.extraLarge
                            } else {
                                MaterialTheme.shapes.large
                            },
                        ),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title(item),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitleValue = subtitle(item)
                if (subtitleValue.isNotBlank()) {
                    Text(
                        text = subtitleValue,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
