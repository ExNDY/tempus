package com.cappielloantonio.tempo.ui.artist
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getArtistListPageViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.navigation.BottomMenuConfig
import com.cappielloantonio.tempo.navigation.DefaultScreenNameExtension.defaultScreenNameWithParams
import com.cappielloantonio.tempo.navigation.Screen
import com.cappielloantonio.tempo.navigation.ScreenNameExtension.screenNameWithParams
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.ArtistListPageArgs
import com.cappielloantonio.tempo.viewmodel.ArtistListUiState
object ArtistListPageRouteScreen : Screen.DefaultScreen {
    private const val TYPE = "type"
    override val navArgs = listOf(
        navArgument(TYPE) {
            type = NavType.StringType
            nullable = false
        },
    )
    override val screenName: String = defaultScreenNameWithParams(TYPE)
    override fun bottomMenuConfig(): BottomMenuConfig = BottomMenuConfig.Hidden
    fun route(type: String): String {
        return screenNameWithParams(type)
    }
    @Composable
    override fun Content(navController: NavController, args: Bundle?) {
        val type = args?.getString(TYPE)
            ?: error("Missing type for ArtistListPageRouteScreen")
        val viewModel = getViewModel {
            getArtistListPageViewModel().apply { onStart(ArtistListPageArgs(type = type)) }
        }
        val uiState by viewModel.uiState.collectAsState()
        ArtistListPageScreen(
            uiState = ArtistListUiState(
                artists = uiState.artists,
                downloadedArtistIds = uiState.downloadedArtistIds,
                isLoading = uiState.isLoading,
                supportsSort = uiState.supportsSort,
            ),
            title = when (type) {
                Constants.ARTIST_STARRED -> stringResource(R.string.artist_list_page_starred)
                Constants.ARTIST_DOWNLOADED -> stringResource(R.string.artist_list_page_downloaded)
                else -> stringResource(R.string.artist_list_page_title)
            },
            onArtistClick = { artist ->
                artist.id?.takeIf { it.isNotBlank() }?.let { artistId ->
                    navController.navigate(ArtistPageRouteScreen.route(artistId))
                }
            },
            onArtistLongClick = { artist ->
                navController.navigate(ArtistBottomSheetRouteScreen.route(artist))
            },
            onNavigateBack = { navController.navigateUp() },
        )
    }
}
