package com.cappielloantonio.tempo.ui.download

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
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
    val coverArtId: String? = null,
    val song: Child? = null,
    val groupValue: String? = null,
    val groupedSongs: List<Child> = emptyList(),
)

private sealed interface DownloadRowItem {
    val id: String
}

private data class DownloadSectionHeader(
    override val id: String,
    val title: String,
) : DownloadRowItem

private data class DownloadEntryRow(
    override val id: String,
    val item: DownloadListItem,
) : DownloadRowItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
    uiState: DownloadUiState,
    onSearchClick: () -> Unit,
    onViewBack: () -> Unit,
    onGroupTypeSelected: (String) -> Unit,
    onRefreshClick: () -> Unit,
    onSetDirectoryClick: () -> Unit,
    onShuffleClick: (List<Child>) -> Unit,
    onTrackClick: (List<Child>, Int) -> Unit,
    onTrackLongClick: (Child, Int) -> Unit,
    onGroupClick: (String, String) -> Unit,
    onGroupLongClick: (String, String) -> Unit,
) {
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    val currentStack = uiState.viewStack.lastOrNull() ?: DownloadStack(
        id = Constants.DOWNLOAD_TYPE_TRACK,
        view = null,
    )
    val canPopViewStack = uiState.viewStack.size > 1
    val filteredSongs = remember(uiState.songs, currentStack) {
        filterDownloadedSongs(currentStack.id, currentStack.view, uiState.songs)
    }
    val currentViewType = remember(currentStack) {
        resolveCurrentViewType(currentStack)
    }
    val rows = remember(filteredSongs, currentViewType) {
        buildRows(
            songs = filteredSongs,
            viewType = currentViewType,
            itemCountFormatter = { count -> "$count" },
        )
    }
    val displayedTracks = remember(rows) {
        rows.mapNotNull { row ->
            (row as? DownloadEntryRow)?.item?.song
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.download_title_section)) },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
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
                    DownloadToolbar(
                        canPopViewStack = canPopViewStack,
                        menuExpanded = menuExpanded,
                        onMenuExpandedChange = { menuExpanded = it },
                        onViewBack = onViewBack,
                        onGroupTypeSelected = onGroupTypeSelected,
                        onRefreshClick = onRefreshClick,
                        onSetDirectoryClick = onSetDirectoryClick,
                    )

                    Text(
                        text = stringResource(id = R.string.download_shuffle_all_subtitle),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
                            .clickable { onShuffleClick(filteredSongs) },
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp),
                    ) {
                        items(rows, key = { it.id }) { row ->
                            when (row) {
                                is DownloadEntryRow -> {
                                    val displayItem = row.item.withFormattedSubtitle()
                                    DownloadRow(
                                        item = displayItem,
                                        onClick = {
                                            if (displayItem.viewType == Constants.DOWNLOAD_TYPE_TRACK) {
                                                val trackIndex = displayedTracks.indexOfFirst { it.id == displayItem.song?.id }
                                                    .takeIf { it >= 0 } ?: 0
                                                displayItem.song?.let { onTrackClick(displayedTracks, trackIndex) }
                                            } else if (!displayItem.groupValue.isNullOrEmpty()) {
                                                onGroupClick(displayItem.viewType, displayItem.groupValue)
                                            }
                                        },
                                        onLongClick = {
                                            if (displayItem.viewType == Constants.DOWNLOAD_TYPE_TRACK) {
                                                val trackIndex = displayedTracks.indexOfFirst { it.id == displayItem.song?.id }
                                                    .takeIf { it >= 0 } ?: 0
                                                displayItem.song?.let { onTrackLongClick(it, trackIndex) }
                                            } else if (displayItem.groupedSongs.isNotEmpty()) {
                                                onGroupLongClick(
                                                    displayItem.viewType,
                                                    displayItem.groupValue.orEmpty(),
                                                )
                                            }
                                        },
                                    )
                                }

                                is DownloadSectionHeader -> DownloadSectionHeaderRow(title = row.title)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadToolbar(
    canPopViewStack: Boolean,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onViewBack: () -> Unit,
    onGroupTypeSelected: (String) -> Unit,
    onRefreshClick: () -> Unit,
    onSetDirectoryClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = R.string.download_title_section),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRefreshClick) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = stringResource(id = R.string.download_refresh_button_content_description),
            )
        }
        if (canPopViewStack) {
            IconButton(onClick = onViewBack) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = null,
                )
            }
        }
        Box {
            IconButton(onClick = { onMenuExpandedChange(true) }) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { onMenuExpandedChange(false) },
            ) {
                DownloadMenuItem(R.string.menu_group_by_track) {
                    onMenuExpandedChange(false)
                    onGroupTypeSelected(Constants.DOWNLOAD_TYPE_TRACK)
                }
                DownloadMenuItem(R.string.menu_group_by_album) {
                    onMenuExpandedChange(false)
                    onGroupTypeSelected(Constants.DOWNLOAD_TYPE_ALBUM)
                }
                DownloadMenuItem(R.string.menu_group_by_artist) {
                    onMenuExpandedChange(false)
                    onGroupTypeSelected(Constants.DOWNLOAD_TYPE_ARTIST)
                }
                DownloadMenuItem(R.string.menu_group_by_genre) {
                    onMenuExpandedChange(false)
                    onGroupTypeSelected(Constants.DOWNLOAD_TYPE_GENRE)
                }
                DownloadMenuItem(R.string.menu_group_by_year) {
                    onMenuExpandedChange(false)
                    onGroupTypeSelected(Constants.DOWNLOAD_TYPE_YEAR)
                }
                DownloadMenuItem(R.string.menu_group_by_playlist) {
                    onMenuExpandedChange(false)
                    onGroupTypeSelected(Constants.DOWNLOAD_TYPE_PLAYLIST)
                }
                DownloadMenuItem(R.string.download_directory_set) {
                    onMenuExpandedChange(false)
                    onSetDirectoryClick()
                }
            }
        }
    }
}

@Composable
private fun DownloadSectionHeaderRow(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, end = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp),
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
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
            Image(
                painter = painterResource(id = R.drawable.ui_empty_list),
                contentDescription = null,
                modifier = Modifier.size(180.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
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
                    painter = painterResource(id = R.drawable.ic_more_vert),
                    contentDescription = null,
                )
            }
        },
    )
}

@Composable
private fun DownloadListItem.withFormattedSubtitle(): DownloadListItem {
    return if (viewType == Constants.DOWNLOAD_TYPE_TRACK) {
        this
    } else {
        copy(
            subtitle = stringResource(
                id = R.string.download_item_single_subtitle_formatter,
                subtitle.toIntOrNull() ?: 0,
            ),
        )
    }
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

private fun buildRows(
    songs: List<Child>,
    viewType: String,
    itemCountFormatter: (Int) -> String,
): List<DownloadRowItem> {
    val items = buildItems(songs, viewType, itemCountFormatter)

    return when (viewType) {
        Constants.DOWNLOAD_TYPE_TRACK -> buildSectionedRows(
            items = items,
            sectionKey = { item -> item.song?.album.orEmpty() },
        )

        Constants.DOWNLOAD_TYPE_ALBUM -> buildSectionedRows(
            items = items,
            sectionKey = { item -> item.groupedSongs.firstOrNull()?.artist.orEmpty() },
        )

        else -> items.map { item ->
            DownloadEntryRow(
                id = item.id,
                item = item,
            )
        }
    }
}

private fun buildSectionedRows(
    items: List<DownloadListItem>,
    sectionKey: (DownloadListItem) -> String,
): List<DownloadRowItem> {
    val rows = mutableListOf<DownloadRowItem>()
    var previousSection: String? = null

    items.forEach { item ->
        val currentSection = sectionKey(item).takeIf { it.isNotBlank() }
        if (currentSection != null && currentSection != previousSection) {
            rows += DownloadSectionHeader(
                id = "section:${item.id}",
                title = currentSection,
            )
            previousSection = currentSection
        }

        rows += DownloadEntryRow(
            id = item.id,
            item = item,
        )
    }

    return rows
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
