package com.cappielloantonio.tempo.ui.album

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.components.TempusImage
import com.cappielloantonio.tempo.ui.components.TempusImageType
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.viewmodel.AlbumPageUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumPageScreen(
    uiState: AlbumPageUiState,
    downloadedSongIds: Set<String>,
    currentSongId: String?,
    isPlaying: Boolean,
    onFavoriteClick: () -> Unit,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onRateClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onArtistClick: () -> Unit,
    onYearClick: (Int) -> Unit,
    onSongClick: (Int) -> Unit,
    onSongLongClick: (Child) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = uiState.album?.name ?: "",
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
                            imageVector = if (uiState.album?.starred != null) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (uiState.album?.starred != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onRateClick) {
                        Icon(imageVector = Icons.Default.StarOutline, contentDescription = null)
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
                AlbumHeader(
                    uiState = uiState,
                    onArtistClick = onArtistClick,
                    onYearClick = onYearClick,
                    onPlayClick = onPlayClick,
                    onShuffleClick = onShuffleClick,
                    onDownloadClick = onDownloadClick,
                    onAddToPlaylistClick = onAddToPlaylistClick
                )
            }

            if (!uiState.album?.genre.isNullOrEmpty() || !uiState.albumInfo?.notes.isNullOrEmpty()) {
                item {
                    AlbumDetails(uiState = uiState)
                }
            }

            itemsIndexed(uiState.songs) { index, song ->
                SongItem(
                    song = song,
                    isDownloaded = song.id in downloadedSongIds,
                    isCurrent = song.id == currentSongId,
                    isPlaying = isPlaying && song.id == currentSongId,
                    onClick = { onSongClick(index) },
                    onLongClick = { onSongLongClick(song) }
                )
            }
        }
    }
}

@Composable
private fun AlbumDetails(uiState: AlbumPageUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        val genre = uiState.album?.genre
        if (!genre.isNullOrEmpty()) {
            Text(
                text = genre,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        val notes = uiState.albumInfo?.notes?.let(MusicUtil::forceReadableString).orEmpty().trim()
        if (notes.isNotEmpty()) {
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AlbumHeader(
    uiState: AlbumPageUiState,
    onArtistClick: () -> Unit,
    onYearClick: (Int) -> Unit,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TempusImage(
            coverArtId = uiState.album?.coverArtId,
            imageType = TempusImageType.Album,
            modifier = Modifier
                .size(200.dp)
                .clip(MaterialTheme.shapes.large),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = uiState.album?.artist ?: "",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onArtistClick)
        )
        if (uiState.album?.year != null) {
            val year = uiState.album.year
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { onYearClick(year) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = onPlayClick) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
            }
            IconButton(onClick = onShuffleClick) {
                Icon(imageVector = Icons.Default.Shuffle, contentDescription = null)
            }
            IconButton(onClick = onDownloadClick) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null)
            }
            IconButton(onClick = onAddToPlaylistClick) {
                Icon(imageVector = Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null)
            }
        }
    }
}

@Composable
fun SongItem(
    song: Child,
    isDownloaded: Boolean,
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
                text = "${song.track ?: ""} • ${song.artist ?: ""}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            if (isCurrent && isPlaying) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = song.track?.toString() ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(24.dp)
                )
            }
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isDownloaded) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = stringResource(id = R.string.song_list_page_downloaded),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                IconButton(onClick = onLongClick) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
                }
            }
        }
    )
}
