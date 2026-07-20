package com.cappielloantonio.tempo.ui.search

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import com.cappielloantonio.tempo.di.getSearchViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.navigation.BottomMenuConfig
import com.cappielloantonio.tempo.navigation.DefaultScreenNameExtension.defaultScreenName
import com.cappielloantonio.tempo.navigation.Screen
import com.cappielloantonio.tempo.ui.album.AlbumBottomSheetRouteScreen
import com.cappielloantonio.tempo.ui.album.AlbumPageRouteScreen
import com.cappielloantonio.tempo.ui.artist.ArtistBottomSheetRouteScreen
import com.cappielloantonio.tempo.ui.artist.ArtistPageRouteScreen
import com.cappielloantonio.tempo.ui.song.SongBottomSheetRouteScreen

object SearchRouteScreen : Screen.DefaultScreen {

    override val screenName: String = defaultScreenName()

    override fun bottomMenuConfig(): BottomMenuConfig = BottomMenuConfig.Hidden

    @Composable
    override fun Content(navController: NavController, args: Bundle?) {
        val viewModel = getViewModel { getSearchViewModel() }
        val uiState by viewModel.uiState.collectAsState()

        SearchScreen(
            uiState = uiState,
            onQueryChange = { query ->
                viewModel.search(query, saveToRecents = false)
            },
            onSearch = { query ->
                viewModel.search(query, saveToRecents = true)
            },
            onRecentSearchDelete = { search ->
                viewModel.deleteRecentSearch(search)
            },
            onArtistClick = { artist ->
                artist.id?.takeIf { it.isNotBlank() }?.let { artistId ->
                    navController.navigate(ArtistPageRouteScreen.route(artistId))
                }
            },
            onArtistLongClick = { artist ->
                navController.navigate(ArtistBottomSheetRouteScreen.route(artist))
            },
            onAlbumClick = { album ->
                album.id?.takeIf { it.isNotBlank() }?.let { albumId ->
                    navController.navigate(AlbumPageRouteScreen.route(albumId))
                }
            },
            onAlbumLongClick = { album ->
                navController.navigate(AlbumBottomSheetRouteScreen.route(album))
            },
            onSongClick = { song ->
                navController.navigate(SongBottomSheetRouteScreen.route(song))
            },
            onSongLongClick = { song ->
                navController.navigate(SongBottomSheetRouteScreen.route(song))
            },
            onNavigateBack = { navController.navigateUp() },
        )
    }
}
