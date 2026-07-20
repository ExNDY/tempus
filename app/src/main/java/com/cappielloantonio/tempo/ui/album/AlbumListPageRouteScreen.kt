package com.cappielloantonio.tempo.ui.album
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getAlbumListPageViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.navigation.BottomMenuConfig
import com.cappielloantonio.tempo.navigation.DefaultScreenNameExtension.defaultScreenNameWithParams
import com.cappielloantonio.tempo.navigation.Screen
import com.cappielloantonio.tempo.navigation.ScreenNameExtension.screenNameWithParams
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.AlbumListPageArgs
object AlbumListPageRouteScreen : Screen.DefaultScreen {
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
            ?: error("Missing type for AlbumListPageRouteScreen")
        val viewModel = getViewModel {
            getAlbumListPageViewModel().apply { onStart(AlbumListPageArgs(type = type)) }
        }
        val uiState by viewModel.uiState.collectAsState()
        AlbumListPageScreen(
            uiState = uiState,
            title = when (type) {
                Constants.ALBUM_STARRED -> stringResource(R.string.album_list_page_starred)
                Constants.ALBUM_RECENTLY_ADDED -> stringResource(R.string.album_list_page_recently_added)
                Constants.ALBUM_RECENTLY_PLAYED -> stringResource(R.string.album_list_page_recently_played)
                Constants.ALBUM_MOST_PLAYED -> stringResource(R.string.album_list_page_most_played)
                Constants.ALBUM_NEW_RELEASES -> stringResource(R.string.album_list_page_new_releases)
                Constants.ALBUM_DOWNLOADED -> stringResource(R.string.album_list_page_downloaded)
                else -> stringResource(R.string.album_list_page_title)
            },
            onAlbumClick = { album ->
                album.id?.takeIf { it.isNotBlank() }?.let { albumId ->
                    navController.navigate(AlbumPageRouteScreen.route(albumId))
                }
            },
            onAlbumLongClick = { album ->
                navController.navigate(AlbumBottomSheetRouteScreen.route(album))
            },
            onNavigateBack = { navController.navigateUp() },
        )
    }
}
