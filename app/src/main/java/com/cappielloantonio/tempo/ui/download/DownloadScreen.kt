package com.cappielloantonio.tempo.ui.download

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.model.DownloadStack
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.components.TempusImage
import com.cappielloantonio.tempo.ui.components.TempusImageType
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.viewmodel.DownloadUiState

private data class DownloadListItem(
    val id: String,
    val viewType: String,
    val title: String,
    val subtitle: String,
    val preTitle: String? = null,
    val coverArtId: String? = null,
    val song: Child? = null,
    val groupValue: String? = null,
    val groupedSongs: List<Child> = emptyList(),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
    uiState: DownloadUiState,
    onNavigateBack: () -> Unit,
    onSearchClick: () -> Unit,
    onGroupTypeSelected: (String) -> Unit,
    onRefreshClick: () -> Unit,
    onSetDirectoryClick: () -> Unit,
    onShuffleClick: (List<Child>) -> Unit,
    onTrackClick: (List<Child>, Int) -> Unit,
    onTrackLongClick: (Child, Int) -> Unit,
    onGroupClick: (String, String) -> Unit,
    onGroupLongClick: (List<Child>, String, String) -> Unit,
) {
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    val currentStack = uiState.viewStack.lastOrNull() ?: DownloadStack(
        id = Constants.DOWNLOAD_TYPE_TRACK,
        view = null,
    )
    val filteredSongs = remember(uiState.songs, currentStack) {
        filterSongs(currentStack.id, currentStack.view, uiState.songs)
    }
    val currentViewType = remember(currentStack) {
        resolveCurrentViewType(currentStack)
    }
    val items = remember(filteredSongs, currentViewType) {
        buildItems(
            songs = filteredSongs,
            viewType = currentViewType,
            itemCountFormatter = { count -> "$count" },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.download_title_section)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DownloadMenuItem(R.string.menu_group_by_track) {
                            menuExpanded = false
                            onGroupTypeSelected(Constants.DOWNLOAD_TYPE_TRACK)
                        }
                        DownloadMenuItem(R.string.menu_group_by_album) {
                            menuExpanded = false
                            onGroupTypeSelected(Constants.DOWNLOAD_TYPE_ALBUM)
                        }
                        DownloadMenuItem(R.string.menu_group_by_artist) {
                            menuExpanded = false
                            onGroupTypeSelected(Constants.DOWNLOAD_TYPE_ARTIST)
                        }
                        DownloadMenuItem(R.string.menu_group_by_genre) {
                            menuExpanded = false
                            onGroupTypeSelected(Constants.DOWNLOAD_TYPE_GENRE)
                        }
                        DownloadMenuItem(R.string.menu_group_by_year) {
                            menuExpanded = false
                            onGroupTypeSelected(Constants.DOWNLOAD_TYPE_YEAR)
                        }
                        DownloadMenuItem(R.string.menu_group_by_playlist) {
                            menuExpanded = false
                            onGroupTypeSelected(Constants.DOWNLOAD_TYPE_PLAYLIST)
                        }
                        DownloadMenuItem(R.string.download_directory_set) {
                            menuExpanded = false
                            onSetDirectoryClick()
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.isLoading && uiState.songs.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.songs.isEmpty() -> {
                DownloadEmptyState(
                    modifier = Modifier.padding(padding),
                    onSetDirectoryClick = onSetDirectoryClick,
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ElevatedAssistChip(
                            onClick = { onShuffleClick(filteredSongs) },
                            label = { Text(text = stringResource(id = R.string.download_shuffle_all_subtitle)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = null,
                                )
                            },
                        )
                        ElevatedAssistChip(
                            onClick = onRefreshClick,
                            label = { Text(text = stringResource(id = R.string.download_refresh_button_content_description)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                )
                            },
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp),
                    ) {
                        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                            DownloadRow(
                                item = item.copy(
                                    subtitle = if (item.viewType == Constants.DOWNLOAD_TYPE_TRACK) {
                                        item.subtitle
                                    } else {
                                        stringResource(
                                            id = R.string.download_item_single_subtitle_formatter,
                                            item.subtitle.toIntOrNull() ?: 0,
                                        )
                                    },
                                ),
                                onClick = {
                                    if (item.viewType == Constants.DOWNLOAD_TYPE_TRACK) {
                                        item.song?.let { onTrackClick(filteredSongs, index) }
                                    } else if (!item.groupValue.isNullOrEmpty()) {
                                        onGroupClick(item.viewType, item.groupValue)
                                    }
                                },
                                onLongClick = {
                                    if (item.viewType == Constants.DOWNLOAD_TYPE_TRACK) {
                                        item.song?.let { onTrackLongClick(it, index) }
                                    } else if (item.groupedSongs.isNotEmpty()) {
                                        onGroupLongClick(item.groupedSongs, item.title, item.subtitle)
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

@Composable
private fun DownloadMenuItem(
    textRes: Int,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(text = stringResource(id = textRes)) },
        onClick = onClick,
    )
}

@Composable
private fun DownloadEmptyState(
    modifier: Modifier = Modifier,
    onSetDirectoryClick: () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.download_info_empty_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.download_info_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onSetDirectoryClick) {
                Text(text = stringResource(id = R.string.download_directory_set))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadRow(
    item: DownloadListItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        ),
        overlineContent = item.preTitle?.takeIf { it.isNotBlank() }?.let { preTitle ->
            {
                Text(
                    text = preTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        headlineContent = {
            Text(
                text = item.title,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = item.subtitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            if (item.coverArtId != null) {
                TempusImage(
                    coverArtId = item.coverArtId,
                    imageType = TempusImageType.Song,
                    modifier = Modifier.size(56.dp),
                )
            }
        },
        trailingContent = {
            IconButton(onClick = onLongClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                )
            }
        },
    )
}

private fun resolveCurrentViewType(level: DownloadStack): String {
    if (level.view == null) {
        return level.id
    }

    return when (level.id) {
        Constants.DOWNLOAD_TYPE_ARTIST -> Constants.DOWNLOAD_TYPE_ALBUM
        else -> Constants.DOWNLOAD_TYPE_TRACK
    }
}

private fun filterSongs(
    filterKey: String,
    filterValue: String?,
    songs: List<Child>,
): List<Child> {
    if (filterValue == null) {
        return songs
    }

    return when (filterKey) {
        Constants.DOWNLOAD_TYPE_TRACK -> songs.filter { it.id == filterValue }
        Constants.DOWNLOAD_TYPE_ALBUM -> songs.filter { it.albumId == filterValue }
        Constants.DOWNLOAD_TYPE_ARTIST -> songs.filter { it.artistId == filterValue }
        Constants.DOWNLOAD_TYPE_GENRE -> songs.filter { it.genre == filterValue }
        Constants.DOWNLOAD_TYPE_YEAR -> songs.filter { it.year?.toString() == filterValue }
        Constants.DOWNLOAD_TYPE_PLAYLIST -> songs.filter {
            it is Download && it.playlistId == filterValue
        }

        else -> songs
    }
}

private fun buildItems(
    songs: List<Child>,
    viewType: String,
    itemCountFormatter: (Int) -> String,
): List<DownloadListItem> {
    return when (viewType) {
        Constants.DOWNLOAD_TYPE_TRACK -> songs
            .distinctBy { it.id.orEmpty() }
            .map { song ->
                DownloadListItem(
                    id = song.id.orEmpty(),
                    viewType = Constants.DOWNLOAD_TYPE_TRACK,
                    title = song.title.orEmpty(),
                    subtitle = buildTrackSubtitle(song),
                    preTitle = song.album,
                    coverArtId = song.coverArtId,
                    song = song,
                )
            }

        Constants.DOWNLOAD_TYPE_ALBUM -> songs
            .filter { !it.albumId.isNullOrEmpty() }
            .groupBy { it.albumId.orEmpty() }
            .mapNotNull { (albumId, albumSongs) ->
                albumSongs.firstOrNull()?.let { song ->
                    DownloadListItem(
                        id = "album:$albumId",
                        viewType = Constants.DOWNLOAD_TYPE_ALBUM,
                        title = song.album.orEmpty(),
                        subtitle = itemCountFormatter(albumSongs.size),
                        preTitle = song.artist,
                        coverArtId = song.coverArtId,
                        groupValue = albumId,
                        groupedSongs = albumSongs,
                    )
                }
            }

        Constants.DOWNLOAD_TYPE_ARTIST -> songs
            .filter { !it.artistId.isNullOrEmpty() }
            .groupBy { it.artistId.orEmpty() }
            .mapNotNull { (artistId, artistSongs) ->
                artistSongs.firstOrNull()?.let { song ->
                    DownloadListItem(
                        id = "artist:$artistId",
                        viewType = Constants.DOWNLOAD_TYPE_ARTIST,
                        title = song.artist.orEmpty(),
                        subtitle = itemCountFormatter(artistSongs.size),
                        coverArtId = song.coverArtId,
                        groupValue = artistId,
                        groupedSongs = artistSongs,
                    )
                }
            }

        Constants.DOWNLOAD_TYPE_GENRE -> songs
            .filter { !it.genre.isNullOrEmpty() }
            .groupBy { it.genre.orEmpty() }
            .map { (genre, genreSongs) ->
                DownloadListItem(
                    id = "genre:$genre",
                    viewType = Constants.DOWNLOAD_TYPE_GENRE,
                    title = genre,
                    subtitle = itemCountFormatter(genreSongs.size),
                    groupValue = genre,
                    groupedSongs = genreSongs,
                )
            }

        Constants.DOWNLOAD_TYPE_YEAR -> songs
            .filter { it.year != null }
            .groupBy { it.year.toString() }
            .map { (year, yearSongs) ->
                DownloadListItem(
                    id = "year:$year",
                    viewType = Constants.DOWNLOAD_TYPE_YEAR,
                    title = year,
                    subtitle = itemCountFormatter(yearSongs.size),
                    groupValue = year,
                    groupedSongs = yearSongs,
                )
            }

        Constants.DOWNLOAD_TYPE_PLAYLIST -> songs
            .filterIsInstance<Download>()
            .filter { !it.playlistId.isNullOrEmpty() }
            .groupBy { it.playlistId.orEmpty() }
            .mapNotNull { (playlistId, playlistSongs) ->
                playlistSongs.firstOrNull()?.let { song ->
                    DownloadListItem(
                        id = "playlist:$playlistId",
                        viewType = Constants.DOWNLOAD_TYPE_PLAYLIST,
                        title = song.playlistName.orEmpty(),
                        subtitle = itemCountFormatter(playlistSongs.size),
                        coverArtId = song.coverArtId,
                        groupValue = playlistId,
                        groupedSongs = playlistSongs,
                    )
                }
            }

        else -> emptyList()
    }
}

private fun buildTrackSubtitle(song: Child): String {
    return buildString {
        append(song.artist.orEmpty())

        val duration = song.duration?.let { MusicUtil.getReadableDurationString(it, false) }
        if (!duration.isNullOrBlank()) {
            if (isNotEmpty()) append("  •  ")
            append(duration)
        }

        val quality = MusicUtil.getReadableAudioQualityString(song)
        if (!quality.isNullOrBlank()) {
            if (isNotEmpty()) append("  •  ")
            append(quality)
        }
    }
}
