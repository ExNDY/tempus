package com.cappielloantonio.tempo.ui.artist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.SimilarArtistID3
import com.cappielloantonio.tempo.ui.components.TempusImage
import com.cappielloantonio.tempo.ui.components.TempusImageType
import com.cappielloantonio.tempo.viewmodel.ArtistPageUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistPageScreen(
    uiState: ArtistPageUiState,
    currentSongId: String?,
    isPlaying: Boolean,
    isBiographyVisible: Boolean,
    onFavoriteClick: () -> Unit,
    onToggleBiographyVisibility: () -> Unit,
    onBiographyMoreClick: (() -> Unit)?,
    onShuffleClick: () -> Unit,
    onRadioClick: () -> Unit,
    onSeeAllTopSongsClick: () -> Unit,
    onAlbumClick: (AlbumID3) -> Unit,
    onSongClick: (Int) -> Unit,
    onSongLongClick: (Child) -> Unit,
    onSimilarArtistClick: (ArtistID3) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = uiState.artist?.name ?: "",
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
                    IconButton(onClick = onFavoriteClick) {
                        Icon(
                            imageVector = if (uiState.artist?.starred != null) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (uiState.artist?.starred != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                ArtistHeader(
                    uiState = uiState,
                    isBiographyVisible = isBiographyVisible,
                    onToggleBiography = onToggleBiographyVisibility,
                    onBiographyMore = onBiographyMoreClick,
                    onShuffleClick = onShuffleClick,
                    onRadioClick = onRadioClick
                )
            }

            if (uiState.topSongs.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = stringResource(id = R.string.artist_page_title_most_streamed_song_section),
                        onSeeAll = onSeeAllTopSongsClick
                    )
                }
                itemsIndexed(uiState.topSongs.take(5)) { index, song ->
                    SongItem(
                        song = song,
                        isCurrent = song.id == currentSongId,
                        isPlaying = isPlaying && song.id == currentSongId,
                        onClick = { onSongClick(index) },
                        onLongClick = { onSongLongClick(song) }
                    )
                }
            } else {
                item {
                    Text(
                        text = stringResource(R.string.artist_page_title_most_streamed_song_unavailable),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            uiState.albums.forEach { (category, albums) ->
                item {
                    SectionHeader(title = getCategoryTitle(category))
                }
                items(albums) { album ->
                    AlbumItem(album = album, onClick = { onAlbumClick(album) })
                }
            }

            val similarArtists = uiState.artistInfo?.similarArtists.orEmpty()
            if (similarArtists.isNotEmpty()) {
                item {
                    SectionHeader(title = stringResource(id = R.string.artist_page_title_similar_artists_section))
                }
                items(similarArtists) { artist ->
                    SimilarArtistItem(artist = artist, onClick = { onSimilarArtistClick(artist.toArtist()) })
                }
            }
        }
    }
}

@Composable
fun ArtistHeader(
    uiState: ArtistPageUiState,
    isBiographyVisible: Boolean,
    onToggleBiography: () -> Unit,
    onBiographyMore: (() -> Unit)?,
    onShuffleClick: () -> Unit,
    onRadioClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TempusImage(
            coverArtId = uiState.artist?.coverArtId,
            imageType = TempusImageType.Artist,
            modifier = Modifier
                .size(200.dp)
                .clip(MaterialTheme.shapes.extraLarge),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.artistInfo?.biography != null) {
            Column(modifier = Modifier.clickable(onClick = onToggleBiography)) {
                Text(
                    text = uiState.artistInfo.biography ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (isBiographyVisible) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (!isBiographyVisible) {
                    Text(
                        text = stringResource(id = R.string.artist_page_title_biography_more_button),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onShuffleClick) {
                Icon(imageVector = Icons.Default.Shuffle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(id = R.string.artist_page_shuffle_button))
            }
            OutlinedButton(onClick = onRadioClick) {
                Icon(imageVector = Icons.Default.Radio, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(id = R.string.artist_page_radio_button))
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, onSeeAll: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text(text = stringResource(id = R.string.artist_page_title_most_streamed_song_see_all_button))
            }
        }
    }
}

@Composable
fun SongItem(
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
                text = song.album ?: "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            TempusImage(
                coverArtId = song.coverArtId,
                imageType = TempusImageType.Song,
                modifier = Modifier.size(40.dp).clip(MaterialTheme.shapes.small)
            )
        },
        trailingContent = {
            IconButton(onClick = onLongClick) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
            }
        }
    )
}

@Composable
fun AlbumItem(album: AlbumID3, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = album.name ?: "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = "${album.year ?: ""} • ${album.songCount} tracks",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            TempusImage(
                coverArtId = album.coverArtId,
                imageType = TempusImageType.Album,
                modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.small)
            )
        },
        trailingContent = {
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
        }
    )
}

@Composable
fun SimilarArtistItem(artist: SimilarArtistID3, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = artist.name ?: "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = stringResource(id = R.string.artist_page_title_similar_artists_section),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            TempusImage(
                coverArtId = artist.coverArtId,
                imageType = TempusImageType.Artist,
                modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.small)
            )
        },
        trailingContent = {
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
        }
    )
}

@Composable
private fun getCategoryTitle(category: String): String {
    return when (category) {
        "album" -> stringResource(id = R.string.artist_page_title_album_section)
        "ep" -> stringResource(id = R.string.artist_page_title_ep_section)
        "single" -> stringResource(id = R.string.artist_page_title_single_section)
        "compilation" -> stringResource(id = R.string.artist_page_title_compilation_section)
        "soundtrack" -> stringResource(id = R.string.artist_page_title_soundtrack_section)
        "live" -> stringResource(id = R.string.artist_page_title_live_section)
        "remix" -> stringResource(id = R.string.artist_page_title_remix_section)
        "appears_on" -> stringResource(id = R.string.artist_page_title_appears_on_section)
        else -> category.replaceFirstChar { it.uppercase() }
    }
}

private fun SimilarArtistID3.toArtist(): ArtistID3 = ArtistID3().also {
    it.id = id
    it.name = name
    it.coverArtId = coverArtId
}
