package com.cappielloantonio.tempo.ui.album
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.cappielloantonio.tempo.di.getAlbumCatalogueViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.navigation.BottomMenuConfig
import com.cappielloantonio.tempo.navigation.DefaultScreenNameExtension.defaultScreenName
import com.cappielloantonio.tempo.navigation.Screen
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.viewmodel.AlbumListUiState
object AlbumCatalogueRouteScreen : Screen.DefaultScreen {
    override val screenName: String = defaultScreenName()
    override fun bottomMenuConfig(): BottomMenuConfig = BottomMenuConfig.Hidden
    @Composable
    override fun Content(navController: NavController, args: Bundle?) {
        val viewModel = getViewModel { getAlbumCatalogueViewModel().apply { onStart() } }
        val uiState by viewModel.uiState.collectAsState()
        AlbumListPageScreen(
            uiState = AlbumListUiState(
                albums = uiState.albums,
                isLoading = uiState.isLoading,
                supportsSort = true,
            ),
            title = stringResource(R.string.album_catalogue_title),
            onAlbumClick = { album ->
                album.id?.takeIf { it.isNotBlank() }?.let { albumId ->
                    navController.navigate(AlbumPageRouteScreen.route(albumId))
                }
            },
            onAlbumLongClick = { album ->
                navController.navigate(AlbumBottomSheetRouteScreen.route(album))
            },
            onRefresh = viewModel::refresh,
            onLoadMore = viewModel::loadNextPage,
            onNavigateBack = { navController.navigateUp() },
        )
    }
}
