package com.cappielloantonio.tempo.ui.playlist

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.cappielloantonio.tempo.navigation.Screen
import com.cappielloantonio.tempo.navigation.encodeNavRouteValue
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.dialog.DialogRouteScreen
import com.cappielloantonio.tempo.ui.dialog.PlaylistEditorRouteDialog
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.requireActivity
import com.cappielloantonio.tempo.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

object PlaylistBottomSheetRouteScreen : Screen.BottomSheetScreen {
    private const val PLAYLIST_ID = "playlistId"

    override val screenName: String = "playlistActions?$PLAYLIST_ID={$PLAYLIST_ID}"

    override val navArgs = listOf(
        navArgument(PLAYLIST_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
    )

    fun route(playlist: Playlist): String = route(playlist.id)

    internal fun route(playlistId: String): String =
        "playlistActions?$PLAYLIST_ID=${encodeNavRouteValue(playlistId)}"

    @Composable
    override fun Content(navController: NavController, args: Bundle?, onClose: suspend () -> Unit) {
        val playlistId = args?.getString(PLAYLIST_ID).orEmpty()
        val activity = LocalContext.current.requireActivity<MainActivity>()
        val homeViewModel = ViewModelProvider(activity)[HomeViewModel::class.java]
        val scope = rememberCoroutineScope()

        TempusTheme {
            PlaylistBottomSheetRoute(
                playlistId = playlistId,
                onDismiss = { scope.launch { onClose() } },
                onPlay = { songs ->
                    MediaManager.startQueue(activity.mediaBrowserListenableFuture, ArrayList(songs), 0)
                    activity.setBottomSheetInPeek(true)
                },
                onAddToQueue = { songs ->
                    MediaManager.enqueue(activity.mediaBrowserListenableFuture, songs, false)
                    activity.setBottomSheetInPeek(true)
                },
                onShufflePlay = { songs ->
                    MediaManager.startQueue(activity.mediaBrowserListenableFuture, ArrayList(songs), 0)
                    activity.setBottomSheetInPeek(true)
                },
                onOpenEditor = { targetPlaylist ->
                    scope.launch {
                        onClose()
                        navController.navigate(
                            DialogRouteScreen.route { _, onCloseDialog ->
                                PlaylistEditorRouteDialog(
                                    tracks = null,
                                    playlist = targetPlaylist,
                                    onDismiss = onCloseDialog,
                                    onPlaylistsChanged = {},
                                )
                            },
                        )
                    }
                },
                onDeletePlaylist = { targetPlaylist ->
                    scope.launch {
                        onClose()
                        navController.navigate(
                            DialogRouteScreen.route { _, onCloseDialog ->
                                PlaylistEditorRouteDialog(
                                    tracks = null,
                                    playlist = targetPlaylist,
                                    onDismiss = onCloseDialog,
                                    onPlaylistsChanged = {},
                                )
                            },
                        )
                    }
                },
                onRefreshAfterMutation = {
                    homeViewModel.refreshShares()
                    homeViewModel.refreshSector(Constants.HOME_SECTOR_PINNED_PLAYLISTS)
                },
            )
        }
    }
}
