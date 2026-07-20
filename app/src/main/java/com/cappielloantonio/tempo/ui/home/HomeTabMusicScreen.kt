package com.cappielloantonio.tempo.ui.home

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.subsonic.models.Share
import com.cappielloantonio.tempo.ui.components.TempusImage
import com.cappielloantonio.tempo.ui.components.TempusImageType
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.HomeMusicUiState

@Composable
fun HomeTabMusicScreen(
    uiState: HomeMusicUiState,
    topContent: (@Composable () -> Unit)? = null,
    onSectorRefresh: (String) -> Unit,
    onMediaClick: (Child, List<Child>) -> Unit,
    onAlbumClick: (AlbumID3) -> Unit,
    onArtistClick: (ArtistID3) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onShareClick: (Share) -> Unit,
    onYearClick: (Int) -> Unit,
    onSeeAllClick: (String) -> Unit,
) {
    if (uiState.isLoading && uiState.sectorConfig.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        topContent?.let { content ->
            item {
                content()
            }
        }

        items(uiState.sectorConfig.filter { it.isVisible }) { sector ->
            when (sector.id) {
                Constants.HOME_SECTOR_DISCOVERY -> DiscoverySection(
                    songs = uiState.discoverSongs,
                    onRefresh = { onSectorRefresh(sector.id) },
                    onMediaClick = { onMediaClick(it, uiState.discoverSongs) }
                )
                Constants.HOME_SECTOR_MADE_FOR_YOU -> MadeForYouSection(
                    songs = uiState.similarTracks,
                    onMediaClick = { onMediaClick(it, uiState.similarTracks) }
                )
                Constants.HOME_SECTOR_BEST_OF -> CarouselSection(
                    title = stringResource(id = R.string.home_title_best_of),
                    subtitle = stringResource(id = R.string.home_subtitle_best_of),
                    items = uiState.bestOfArtists,
                    onItemClick = { onArtistClick(it as ArtistID3) }
                )
                Constants.HOME_SECTOR_TOP_SONGS -> TopSongsSection(
                    songs = uiState.topSongs,
                    onMediaClick = { onMediaClick(it, uiState.topSongs) },
                    onSeeAll = { onSeeAllClick(sector.id) }
                )
                Constants.HOME_SECTOR_STARRED_TRACKS -> CarouselSection(
                    title = stringResource(id = R.string.home_title_starred_tracks),
                    items = uiState.starredTracks,
                    onSeeAll = { onSeeAllClick(sector.id) },
                    onItemClick = { onMediaClick(it as Child, uiState.starredTracks) }
                )
                Constants.HOME_SECTOR_STARRED_ALBUMS -> CarouselSection(
                    title = stringResource(id = R.string.home_title_starred_albums),
                    items = uiState.starredAlbums,
                    onSeeAll = { onSeeAllClick(sector.id) },
                    onItemClick = { onAlbumClick(it as AlbumID3) }
                )
                Constants.HOME_SECTOR_STARRED_ARTISTS -> CarouselSection(
                    title = stringResource(id = R.string.home_title_starred_artists),
                    items = uiState.starredArtists,
                    onSeeAll = { onSeeAllClick(sector.id) },
                    onItemClick = { onArtistClick(it as ArtistID3) }
                )
                Constants.HOME_SECTOR_NEW_RELEASES -> CarouselSection(
                    title = stringResource(id = R.string.home_title_new_releases),
                    items = uiState.newReleases,
                    onItemClick = { onAlbumClick(it as AlbumID3) }
                )
                Constants.HOME_SECTOR_FLASHBACK -> FlashbackSection(
                    years = uiState.flashbackYears,
                    onYearClick = onYearClick
                )
                Constants.HOME_SECTOR_MOST_PLAYED -> CarouselSection(
                    title = stringResource(id = R.string.home_title_most_played),
                    items = uiState.mostPlayedAlbums,
                    onSeeAll = { onSeeAllClick(sector.id) },
                    onItemClick = { onAlbumClick(it as AlbumID3) }
                )
                Constants.HOME_SECTOR_LAST_PLAYED -> CarouselSection(
                    title = stringResource(id = R.string.home_title_last_played),
                    items = uiState.recentlyPlayedAlbums,
                    onSeeAll = { onSeeAllClick(sector.id) },
                    onItemClick = { onAlbumClick(it as AlbumID3) }
                )
                Constants.HOME_SECTOR_RECENTLY_ADDED -> CarouselSection(
                    title = stringResource(id = R.string.home_title_recently_added),
                    items = uiState.recentlyAddedAlbums,
                    onSeeAll = { onSeeAllClick(sector.id) },
                    onItemClick = { onAlbumClick(it as AlbumID3) }
                )
                Constants.HOME_SECTOR_PINNED_PLAYLISTS -> CarouselSection(
                    title = stringResource(id = R.string.home_title_pinned_playlists),
                    items = uiState.pinnedPlaylists,
                    onSeeAll = { onSeeAllClick(sector.id) },
                    onItemClick = { onPlaylistClick(it as Playlist) }
                )
                Constants.HOME_SECTOR_SHARED -> CarouselSection(
                    title = stringResource(id = R.string.home_title_shares),
                    items = uiState.shares,
                    onItemClick = { onShareClick(it as Share) }
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    onSeeAll: (() -> Unit)? = null,
    onAction: (() -> Unit)? = null,
    actionIcon: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row {
                if (onAction != null && actionIcon != null) {
                    IconButton(onClick = onAction) {
                        actionIcon()
                    }
                }
                if (onSeeAll != null) {
                    IconButton(onClick = onSeeAll) {
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoverySection(
    songs: List<Child>,
    onRefresh: () -> Unit,
    onMediaClick: (Child) -> Unit,
) {
    Column {
        SectionHeader(
            title = stringResource(id = R.string.home_title_discovery),
            onAction = onRefresh,
            actionIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(songs) { song ->
                DiscoveryItem(song = song, onClick = { onMediaClick(song) })
            }
        }
    }
}

@Composable
fun DiscoveryItem(song: Child, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            TempusImage(
                coverArtId = song.coverArtId,
                imageType = TempusImageType.Song,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = song.title ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MadeForYouSection(
    songs: List<Child>,
    onMediaClick: (Child) -> Unit,
) {
    Column {
        SectionHeader(
            title = stringResource(id = R.string.home_title_made_for_you),
            subtitle = stringResource(id = R.string.home_subtitle_made_for_you)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(songs) { song ->
                MadeForYouItem(song = song, onClick = { onMediaClick(song) })
            }
        }
    }
}

@Composable
fun MadeForYouItem(song: Child, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
    ) {
        TempusImage(
            coverArtId = song.coverArtId,
            imageType = TempusImageType.Song,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CarouselSection(
    title: String,
    items: List<Any>,
    onItemClick: (Any) -> Unit,
    subtitle: String? = null,
    onSeeAll: (() -> Unit)? = null,
) {
    if (items.isEmpty()) return

    Column {
        SectionHeader(title = title, subtitle = subtitle, onSeeAll = onSeeAll)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items) { item ->
                when (item) {
                    is AlbumID3 -> AlbumCarouselItem(album = item, onClick = { onItemClick(item) })
                    is ArtistID3 -> ArtistCarouselItem(
                        artist = item,
                        onClick = { onItemClick(item) })
                    is Child -> SongCarouselItem(song = item, onClick = { onItemClick(item) })
                    is Playlist -> PlaylistCarouselItem(
                        playlist = item,
                        onClick = { onItemClick(item) })
                    is Share -> ShareCarouselItem(share = item, onClick = { onItemClick(item) })
                }
            }
        }
    }
}

@Composable
fun AlbumCarouselItem(album: AlbumID3, onClick: () -> Unit) {
    Column(modifier = Modifier
        .width(140.dp)
        .clickable(onClick = onClick)) {
        TempusImage(
            coverArtId = album.coverArtId,
            imageType = TempusImageType.Album,
            modifier = Modifier
                .size(140.dp)
                .clip(MaterialTheme.shapes.medium)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = album.name ?: "",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = album.artist ?: "",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ArtistCarouselItem(artist: ArtistID3, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TempusImage(
            coverArtId = artist.coverArtId,
            imageType = TempusImageType.Artist,
            modifier = Modifier
                .clip(CircleShape)
                .size(80.dp)
                .clip(MaterialTheme.shapes.extraLarge)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = artist.name.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SongCarouselItem(song: Child, onClick: () -> Unit) {
    Column(modifier = Modifier
        .width(140.dp)
        .clickable(onClick = onClick)) {
        TempusImage(
            coverArtId = song.coverArtId,
            imageType = TempusImageType.Song,
            modifier = Modifier
                .size(140.dp)
                .clip(MaterialTheme.shapes.medium)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = song.title ?: "",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist ?: "",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PlaylistCarouselItem(playlist: Playlist, onClick: () -> Unit) {
    Column(modifier = Modifier
        .width(140.dp)
        .clickable(onClick = onClick)) {
        TempusImage(
            coverArtId = playlist.coverArtId,
            imageType = TempusImageType.Playlist,
            modifier = Modifier
                .size(140.dp)
                .clip(MaterialTheme.shapes.medium)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = playlist.name ?: "",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ShareCarouselItem(share: Share, onClick: () -> Unit) {
    Column(modifier = Modifier
        .width(140.dp)
        .clickable(onClick = onClick)) {
        TempusImage(
            coverArtId = share.entries?.firstOrNull()?.coverArtId,
            imageType = TempusImageType.Unknown,
            modifier = Modifier
                .size(140.dp)
                .clip(MaterialTheme.shapes.medium)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = share.description ?: "",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FlashbackSection(years: List<Int>, onYearClick: (Int) -> Unit) {
    Column {
        SectionHeader(title = stringResource(id = R.string.home_title_flashback))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(years) { year ->
                FlashbackItem(year = year, onClick = { onYearClick(year) })
            }
        }
    }
}

@Composable
fun FlashbackItem(year: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = year.toString(),
                maxLines = 1,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun TopSongsSection(
    songs: List<Child>,
    onMediaClick: (Child) -> Unit,
    onSeeAll: () -> Unit
) {
    Column {
        SectionHeader(
            title = stringResource(id = R.string.home_title_top_songs),
            onSeeAll = onSeeAll
        )
        songs.take(5).forEach { song ->
            ListItem(
                modifier = Modifier.clickable { onMediaClick(song) },
                headlineContent = { Text(song.title ?: "") },
                supportingContent = { Text(song.artist ?: "") },
                leadingContent = {
                    TempusImage(
                        coverArtId = song.coverArtId,
                        imageType = TempusImageType.Song,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(MaterialTheme.shapes.small)
                    )
                },
                colors = ListItemDefaults.colors().copy(
                    containerColor = Color.Transparent
                ),
            )
        }
    }
}
