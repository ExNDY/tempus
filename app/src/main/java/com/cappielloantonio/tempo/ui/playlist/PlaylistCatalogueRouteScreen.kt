package com.cappielloantonio.tempo.ui.playlist
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getPlaylistCatalogueViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.navigation.BottomMenuConfig
import com.cappielloantonio.tempo.navigation.DefaultScreenNameExtension.defaultScreenNameWithOptionalParams
import com.cappielloantonio.tempo.navigation.Screen
import com.cappielloantonio.tempo.navigation.ScreenNameExtension.screenNameWithOptionalParams
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.dialog.DialogRouteScreen
import com.cappielloantonio.tempo.ui.dialog.PlaylistEditorRouteDialog
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.PlaylistCatalogueArgs
object PlaylistCatalogueRouteScreen : Screen.DefaultScreen {
    private const val TYPE = "type"
    override val navArgs = listOf(
        navArgument(TYPE) {
            type = NavType.StringType
            nullable = false
            defaultValue = Constants.PLAYLIST_ALL
        },
    )
    override val screenName: String = defaultScreenNameWithOptionalParams(TYPE)
    override fun bottomMenuConfig(): BottomMenuConfig = BottomMenuConfig.Hidden
    fun route(type: String = Constants.PLAYLIST_ALL): String {
        return screenNameWithOptionalParams(listOf(TYPE to type))
    }
    @Composable
    override fun Content(navController: NavController, args: Bundle?) {
        val type = args?.getString(TYPE) ?: Constants.PLAYLIST_ALL
        val viewModel = getViewModel {
            getPlaylistCatalogueViewModel().apply { onStart(PlaylistCatalogueArgs(type)) }
        }
        val uiState by viewModel.uiState.collectAsState()
        PlaylistCatalogueScreen(
            uiState = uiState,
            title = stringResource(R.string.playlist_catalogue_title),
            onPlaylistClick = { playlist ->
                playlist.id.takeIf { it.isNotBlank() }?.let { playlistId ->
                    navController.navigate(PlaylistPageRouteScreen.route(playlistId))
                }
            },
            onPlaylistLongClick = { playlist ->
                navController.navigate(PlaylistBottomSheetRouteScreen.route(playlist))
            },
            onCreatePlaylist = {
                navController.navigate(
                    DialogRouteScreen.route { _, onClose ->
                        PlaylistEditorRouteDialog(
                            tracks = ArrayList<Child>(),
                            playlist = null,
                            onDismiss = onClose,
                            onPlaylistsChanged = viewModel::refresh,
                        )
                    },
                )
            },
            onRefresh = viewModel::refresh,
            onNavigateBack = { navController.navigateUp() },
        )
    }
}
