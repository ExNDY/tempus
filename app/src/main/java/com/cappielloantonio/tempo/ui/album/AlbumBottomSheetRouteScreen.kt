package com.cappielloantonio.tempo.ui.album

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
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.artist.ArtistPageRouteScreen
import com.cappielloantonio.tempo.ui.dialog.DialogRouteScreen
import com.cappielloantonio.tempo.ui.dialog.PlaylistChooserRouteDialog
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.requireActivity
import com.cappielloantonio.tempo.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

object AlbumBottomSheetRouteScreen : Screen.BottomSheetScreen {
    private const val ALBUM_ID = "albumId"

    override val screenName: String = "albumActions?$ALBUM_ID={$ALBUM_ID}"

    override val navArgs = listOf(
        navArgument(ALBUM_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
    )

    fun route(album: AlbumID3): String = route(album.id.orEmpty())

    internal fun route(albumId: String): String =
        "albumActions?$ALBUM_ID=${encodeNavRouteValue(albumId)}"

    @Composable
    override fun Content(navController: NavController, args: Bundle?, onClose: suspend () -> Unit) {
        val albumId = args?.getString(ALBUM_ID).orEmpty()
        val activity = LocalContext.current.requireActivity<MainActivity>()
        val homeViewModel = ViewModelProvider(activity)[HomeViewModel::class.java]
        val scope = rememberCoroutineScope()

        TempusTheme {
            AlbumBottomSheetRoute(
                albumId = albumId,
                onDismiss = { scope.launch { onClose() } },
                onOpenPlaylistChooser = { songs ->
                    scope.launch {
                        onClose()
                        navController.navigate(
                            DialogRouteScreen.route { _, onCloseDialog ->
                                PlaylistChooserRouteDialog(
                                    tracks = ArrayList(songs),
                                    onDismiss = onCloseDialog,
                                    onPlaylistsChanged = {},
                                )
                            },
                        )
                    }
                },
                onNavigateToArtist = { artist ->
                    scope.launch {
                        onClose()
                        artist.id?.takeIf { it.isNotBlank() }?.let { artistId ->
                            navController.navigate(ArtistPageRouteScreen.route(artistId))
                        }
                    }
                },
                onPlayNext = { songs ->
                    MediaManager.enqueue(activity.mediaBrowserListenableFuture, songs, true)
                    activity.setBottomSheetInPeek(true)
                },
                onAddToQueue = { songs ->
                    MediaManager.enqueue(activity.mediaBrowserListenableFuture, songs, false)
                    activity.setBottomSheetInPeek(true)
                },
                onShufflePlay = { songs ->
                    MediaManager.startQueue(activity.mediaBrowserListenableFuture, songs, 0)
                    activity.setBottomSheetInPeek(true)
                },
                onStartInstantMix = { songs ->
                    MediaManager.startQueue(activity.mediaBrowserListenableFuture, songs, 0)
                    activity.setBottomSheetInPeek(true)
                },
                onRefreshShares = {
                    homeViewModel.refreshShares()
                },
            )
        }
    }
}
