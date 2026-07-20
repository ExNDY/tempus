package com.cappielloantonio.tempo.ui.home
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getLibraryViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.navigation.BottomBarItems
import com.cappielloantonio.tempo.navigation.BottomMenuConfig
import com.cappielloantonio.tempo.navigation.DefaultScreenNameExtension.defaultScreenName
import com.cappielloantonio.tempo.navigation.Screen
import com.cappielloantonio.tempo.ui.album.AlbumBottomSheetRouteScreen
import com.cappielloantonio.tempo.ui.album.AlbumCatalogueRouteScreen
import com.cappielloantonio.tempo.ui.album.AlbumPageRouteScreen
import com.cappielloantonio.tempo.ui.artist.ArtistBottomSheetRouteScreen
import com.cappielloantonio.tempo.ui.artist.ArtistCatalogueRouteScreen
import com.cappielloantonio.tempo.ui.artist.ArtistPageRouteScreen
import com.cappielloantonio.tempo.ui.folder.IndexRouteScreen
import com.cappielloantonio.tempo.ui.genre.GenreCatalogueRouteScreen
import com.cappielloantonio.tempo.ui.playlist.PlaylistBottomSheetRouteScreen
import com.cappielloantonio.tempo.ui.playlist.PlaylistCatalogueRouteScreen
import com.cappielloantonio.tempo.ui.playlist.PlaylistPageRouteScreen
import com.cappielloantonio.tempo.ui.song.SongListPageRouteScreen
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.SongListPageArgs
object LibraryRouteScreen : Screen.DefaultScreen {
    override val screenName: String = defaultScreenName()
    override fun bottomMenuConfig(): BottomMenuConfig {
        return BottomMenuConfig.Visible(BottomBarItems.library)
    }
    @Composable
    override fun Content(navController: NavController, args: Bundle?) {
        val viewModel = getViewModel { getLibraryViewModel().apply { onStart() } }
        val uiState by viewModel.uiState.collectAsState()
        LibraryScreen(
            uiState = uiState,
            onNavigateBack = { navController.navigateUp() },
            onRefreshAll = { viewModel.refreshAll() },
            onMusicFolderClick = { folder ->
                navController.navigate(IndexRouteScreen.route(folder.id, folder.name))
            },
            onAlbumClick = { album ->
                album.id?.takeIf { it.isNotBlank() }?.let { albumId ->
                    navController.navigate(AlbumPageRouteScreen.route(albumId))
                }
            },
            onAlbumLongClick = { album ->
                navController.navigate(AlbumBottomSheetRouteScreen.route(album))
            },
            onArtistClick = { artist ->
                artist.id?.takeIf { it.isNotBlank() }?.let { artistId ->
                    navController.navigate(ArtistPageRouteScreen.route(artistId))
                }
            },
            onArtistLongClick = { artist ->
                navController.navigate(ArtistBottomSheetRouteScreen.route(artist))
            },
            onGenreClick = { genre ->
                navController.navigate(
                    SongListPageRouteScreen.route(
                        SongListPageArgs(
                            type = Constants.MEDIA_BY_GENRE,
                            genre = genre,
                        ),
                    ),
                )
            },
            onPlaylistClick = { playlist ->
                playlist.id.takeIf { it.isNotBlank() }?.let { playlistId ->
                    navController.navigate(PlaylistPageRouteScreen.route(playlistId))
                }
            },
            onPlaylistLongClick = { playlist ->
                navController.navigate(PlaylistBottomSheetRouteScreen.route(playlist))
            },
            onSeeAllAlbumsClick = {
                navController.navigate(AlbumCatalogueRouteScreen.screenName)
            },
            onSeeAllArtistsClick = {
                navController.navigate(ArtistCatalogueRouteScreen.screenName)
            },
            onSeeAllGenresClick = {
                navController.navigate(GenreCatalogueRouteScreen.screenName)
            },
            onSeeAllPlaylistsClick = {
                navController.navigate(PlaylistCatalogueRouteScreen.route(Constants.PLAYLIST_ALL))
            },
            onRefreshAlbums = { viewModel.refreshAlbumSample() },
            onRefreshArtists = { viewModel.refreshArtistSample() },
            onRefreshGenres = { viewModel.refreshGenreSample() },
            onRefreshPlaylists = { viewModel.refreshPlaylistSample() },
        )
    }
}
