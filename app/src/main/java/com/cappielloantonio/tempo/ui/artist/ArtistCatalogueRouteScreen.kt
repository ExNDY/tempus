package com.cappielloantonio.tempo.ui.artist
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.cappielloantonio.tempo.di.getArtistCatalogueViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.navigation.BottomMenuConfig
import com.cappielloantonio.tempo.navigation.DefaultScreenNameExtension.defaultScreenName
import com.cappielloantonio.tempo.navigation.Screen
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.viewmodel.ArtistListUiState
object ArtistCatalogueRouteScreen : Screen.DefaultScreen {
    override val screenName: String = defaultScreenName()
    override fun bottomMenuConfig(): BottomMenuConfig = BottomMenuConfig.Hidden
    @Composable
    override fun Content(navController: NavController, args: Bundle?) {
        val viewModel = getViewModel { getArtistCatalogueViewModel().apply { onStart() } }
        val uiState by viewModel.uiState.collectAsState()
        ArtistListPageScreen(
            uiState = ArtistListUiState(
                artists = uiState.artists,
                downloadedArtistIds = uiState.downloadedArtistIds,
                isLoading = uiState.isLoading,
                supportsSort = true,
            ),
            title = stringResource(R.string.artist_catalogue_title),
            onArtistClick = { artist ->
                artist.id?.takeIf { it.isNotBlank() }?.let { artistId ->
                    navController.navigate(ArtistPageRouteScreen.route(artistId))
                }
            },
            onArtistLongClick = { artist ->
                navController.navigate(ArtistBottomSheetRouteScreen.route(artist))
            },
            onRefresh = viewModel::refresh,
            onNavigateBack = { navController.navigateUp() },
        )
    }
}
