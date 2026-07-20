package com.cappielloantonio.tempo.ui.song

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.navigation.Screen
import com.cappielloantonio.tempo.navigation.encodeNavRouteValue
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.album.AlbumPageRouteScreen
import com.cappielloantonio.tempo.ui.artist.ArtistPageRouteScreen
import com.cappielloantonio.tempo.ui.dialog.DialogRouteScreen
import com.cappielloantonio.tempo.ui.dialog.PlaylistChooserRouteDialog
import com.cappielloantonio.tempo.ui.dialog.RatingRouteDialog
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.AssetLinkUtil
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.requireActivity
import com.cappielloantonio.tempo.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

object SongBottomSheetRouteScreen : Screen.BottomSheetScreen {
    private const val SONG_ID = "songId"
    private const val PLAYLIST_ID = "playlistId"
    private const val ITEM_POSITION = "itemPosition"

    override val screenName: String =
        "songActions?$SONG_ID={$SONG_ID}&$PLAYLIST_ID={$PLAYLIST_ID}&$ITEM_POSITION={$ITEM_POSITION}"

    override val navArgs = listOf(
        navArgument(SONG_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(PLAYLIST_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(ITEM_POSITION) {
            type = NavType.IntType
            defaultValue = -1
        },
    )

    fun route(
        song: Child,
        playlistId: String? = null,
        itemPosition: Int = -1,
    ): String = route(song.id, playlistId, itemPosition)

    internal fun route(
        songId: String,
        playlistId: String? = null,
        itemPosition: Int = -1,
    ): String = buildString {
        append("songActions?")
        append(SONG_ID).append('=').append(encodeNavRouteValue(songId))
        playlistId?.let {
            append('&').append(PLAYLIST_ID).append('=').append(encodeNavRouteValue(it))
        }
        append('&').append(ITEM_POSITION).append('=').append(itemPosition)
    }

    @Composable
    override fun Content(navController: NavController, args: Bundle?, onClose: suspend () -> Unit) {
        val songId = args?.getString(SONG_ID).orEmpty()
        val playlistId = args?.getString(PLAYLIST_ID)
        val itemPosition = args?.getInt(ITEM_POSITION, -1) ?: -1
        val activity = LocalContext.current.requireActivity<MainActivity>()
        val homeViewModel = ViewModelProvider(activity)[HomeViewModel::class.java]
        val scope = rememberCoroutineScope()

        TempusTheme {
            SongBottomSheetRoute(
                songId = songId,
                playlistId = playlistId,
                itemPosition = itemPosition,
                onDismiss = { scope.launch { onClose() } },
                onOpenRatingDialog = { media ->
                    scope.launch {
                        onClose()
                        navController.navigate(
                            DialogRouteScreen.route { _, onCloseDialog ->
                                RatingRouteDialog(
                                    song = media,
                                    onDismiss = onCloseDialog,
                                )
                            },
                        )
                    }
                },
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
                onNavigateToAlbum = { album ->
                    scope.launch {
                        onClose()
                        album.id?.takeIf { it.isNotBlank() }?.let { albumId ->
                            navController.navigate(AlbumPageRouteScreen.route(albumId))
                        }
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
                onPlayNext = { media ->
                    MediaManager.enqueue(activity.mediaBrowserListenableFuture, media, true)
                    activity.setBottomSheetInPeek(true)
                },
                onAddToQueue = { media ->
                    MediaManager.enqueue(activity.mediaBrowserListenableFuture, media, false)
                    activity.setBottomSheetInPeek(true)
                },
                onStartInstantMix = { media ->
                    MediaManager.startQueue(activity.mediaBrowserListenableFuture, media, 0)
                    activity.setBottomSheetInPeek(true)
                },
                onOpenAssetLink = { assetLink, collapsePlayer ->
                    activity.openAssetLink(assetLink, collapsePlayer)
                },
                onCopyAssetLink = { assetLink ->
                    AssetLinkUtil.copyToClipboard(activity, assetLink)
                    android.widget.Toast.makeText(
                        activity,
                        activity.getString(R.string.asset_link_copied_toast, assetLink.id),
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                },
                onRefreshShares = {
                    homeViewModel.refreshShares()
                },
            )
        }
    }
}
