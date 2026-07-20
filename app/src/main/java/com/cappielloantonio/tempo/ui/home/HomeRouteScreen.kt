package com.cappielloantonio.tempo.ui.home
import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.cappielloantonio.tempo.di.getHomeViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.navigation.BottomBarItems
import com.cappielloantonio.tempo.navigation.BottomMenuConfig
import com.cappielloantonio.tempo.navigation.DefaultScreenNameExtension.defaultScreenName
import com.cappielloantonio.tempo.navigation.Screen
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.util.requireActivity
import com.cappielloantonio.tempo.ui.album.AlbumListPageRouteScreen
import com.cappielloantonio.tempo.ui.album.AlbumPageRouteScreen
import com.cappielloantonio.tempo.ui.artist.ArtistListPageRouteScreen
import com.cappielloantonio.tempo.ui.artist.ArtistPageRouteScreen
import com.cappielloantonio.tempo.ui.dialog.BottomSheetRouteScreen
import com.cappielloantonio.tempo.ui.settings.SettingsRouteScreen
import com.cappielloantonio.tempo.ui.playlist.PlaylistCatalogueRouteScreen
import com.cappielloantonio.tempo.ui.playlist.PlaylistPageRouteScreen
import com.cappielloantonio.tempo.ui.song.SongListPageRouteScreen
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.SongListPageArgs
import java.util.ArrayList
import androidx.core.net.toUri
import kotlinx.coroutines.launch

object HomeRouteScreen : Screen.DefaultScreen {
    override val screenName: String = defaultScreenName()
    override fun bottomMenuConfig(): BottomMenuConfig {
        return BottomMenuConfig.Visible(BottomBarItems.home)
    }
    @Composable
    override fun Content(navController: NavController, args: Bundle?) {
        val activity = LocalContext.current.requireActivity<MainActivity>()
        val scope = rememberCoroutineScope()
        val routeHomeViewModel = getViewModel { getHomeViewModel().apply { onStart() } }
        val musicUiState by routeHomeViewModel.musicUiState.collectAsState()
        HomeScreen(
            onSettingsClick = {
                navController.navigate(SettingsRouteScreen.screenName)
            },
            onEditHomeClick = {
                navController.navigate(
                    BottomSheetRouteScreen.route { _, onClose ->
                        HomeRearrangementRouteSheet(
                            onDismiss = { scope.launch { onClose() } },
                            onHomeLayoutChanged = routeHomeViewModel::refreshHomeLayout,
                        )
                    },
                )
            },
            musicTab = { topContent ->
                HomeTabMusicScreen(
                    uiState = musicUiState,
                    topContent = topContent,
                    onSectorRefresh = { id -> routeHomeViewModel.refreshSector(id) },
                    onMediaClick = { song, list ->
                        val position = list.indexOf(song)
                        MediaManager.startQueue(activity.mediaBrowserListenableFuture, ArrayList(list), position)
                        activity.setBottomSheetInPeek(true)
                    },
                    onAlbumClick = { album ->
                        album.id?.takeIf { it.isNotBlank() }?.let { albumId ->
                            navController.navigate(AlbumPageRouteScreen.route(albumId))
                        }
                    },
                    onArtistClick = { artist ->
                        artist.id?.takeIf { it.isNotBlank() }?.let { artistId ->
                            navController.navigate(ArtistPageRouteScreen.route(artistId))
                        }
                    },
                    onPlaylistClick = { playlist ->
                        playlist.id.takeIf { it.isNotBlank() }?.let { playlistId ->
                            navController.navigate(PlaylistPageRouteScreen.route(playlistId))
                        }
                    },
                    onShareClick = { share ->
                        activity.startActivity(Intent(Intent.ACTION_VIEW, (share.url ?: "").toUri()))
                    },
                    onYearClick = { year ->
                        navController.navigate(
                            SongListPageRouteScreen.route(
                                SongListPageArgs(
                                    type = Constants.MEDIA_BY_YEAR,
                                    year = year,
                                ),
                            ),
                        )
                    },
                    onSeeAllClick = { sectorId ->
                        navigateToSeeAll(navController, sectorId)
                    },
                )
            },
        )
    }
    private fun navigateToSeeAll(
        navController: NavController,
        sectorId: String,
    ) {
        when (sectorId) {
            Constants.HOME_SECTOR_STARRED_TRACKS -> {
                navController.navigate(SongListPageRouteScreen.route(SongListPageArgs(type = Constants.MEDIA_STARRED)))
                return
            }
            Constants.HOME_SECTOR_STARRED_ALBUMS -> {
                navController.navigate(AlbumListPageRouteScreen.route(Constants.ALBUM_STARRED))
                return
            }
            Constants.HOME_SECTOR_STARRED_ARTISTS -> {
                navController.navigate(ArtistListPageRouteScreen.route(Constants.ARTIST_STARRED))
                return
            }
            Constants.HOME_SECTOR_RECENTLY_ADDED -> {
                navController.navigate(AlbumListPageRouteScreen.route(Constants.ALBUM_RECENTLY_ADDED))
                return
            }
            Constants.HOME_SECTOR_PINNED_PLAYLISTS -> {
                navController.navigate(PlaylistCatalogueRouteScreen.route(Constants.PLAYLIST_ALL))
                return
            }
            Constants.HOME_SECTOR_LAST_PLAYED -> {
                navController.navigate(AlbumListPageRouteScreen.route(Constants.ALBUM_RECENTLY_PLAYED))
                return
            }
            Constants.HOME_SECTOR_MOST_PLAYED -> {
                navController.navigate(AlbumListPageRouteScreen.route(Constants.ALBUM_MOST_PLAYED))
                return
            }
            else -> return
        }
    }
}
