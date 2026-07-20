package com.cappielloantonio.tempo.ui.playlist
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getPlaylistPageViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.navigation.BottomMenuConfig
import com.cappielloantonio.tempo.navigation.DefaultScreenNameExtension.defaultScreenNameWithParams
import com.cappielloantonio.tempo.navigation.Screen
import com.cappielloantonio.tempo.navigation.ScreenNameExtension.screenNameWithParams
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.util.requireActivity
import com.cappielloantonio.tempo.ui.dialog.DialogRouteScreen
import com.cappielloantonio.tempo.ui.dialog.PlaylistEditorRouteDialog
import com.cappielloantonio.tempo.ui.song.SongBottomSheetRouteScreen
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.ExternalAudioReader
import com.cappielloantonio.tempo.util.Preferences
import java.util.ArrayList
object PlaylistPageRouteScreen : Screen.DefaultScreen {
    private const val PLAYLIST_ID = "playlistId"
    override val navArgs = listOf(
        navArgument(PLAYLIST_ID) {
            type = NavType.StringType
            nullable = false
        },
    )
    override val screenName: String = defaultScreenNameWithParams(PLAYLIST_ID)
    override fun bottomMenuConfig(): BottomMenuConfig = BottomMenuConfig.Hidden
    fun route(playlistId: String): String {
        return screenNameWithParams(Uri.encode(playlistId))
    }
    @Composable
    override fun Content(navController: NavController, args: Bundle?) {
        val activity = LocalContext.current.requireActivity<MainActivity>()
        val playlistId = args?.getString(PLAYLIST_ID)
            ?.takeIf { it.isNotBlank() }
            ?: error("Missing playlistId for PlaylistPageRouteScreen")
        val viewModel = getViewModel { getPlaylistPageViewModel().apply { onStart(playlistId) } }
        val uiState by viewModel.uiState.collectAsState()
        val refreshEvent by ExternalAudioReader.getRefreshEvents().observeAsState()
        var searchQuery by remember { mutableStateOf("") }
        val downloadedSongIds = remember(
            uiState.songs,
            refreshEvent,
            Preferences.getDownloadDirectoryUri(),
        ) {
            uiState.songs
                .asSequence()
                .filter { song -> isSongDownloaded(activity, song) }
                .map { it.id }
                .toSet()
        }
        LaunchedEffect(uiState.playlist, uiState.isLoading) {
            if (!uiState.isLoading && uiState.playlist == null) {
                navController.navigateUp()
            }
        }
        PlaylistPageScreen(
            uiState = uiState,
            downloadedSongIds = downloadedSongIds,
            searchQuery = searchQuery,
            currentSongId = uiState.currentSongId,
            isPlaying = uiState.isPlaying,
            onSearchQueryChange = { searchQuery = it },
            onSongClick = { index ->
                MediaManager.startQueue(activity.mediaBrowserListenableFuture, ArrayList(uiState.songs), index)
                activity.setBottomSheetInPeek(true)
            },
            onSongLongClick = { song, index ->
                navController.navigate(
                    SongBottomSheetRouteScreen.route(
                        song = song,
                        playlistId = uiState.playlist?.id,
                        itemPosition = index,
                    ),
                )
            },
            onPlayAllClick = {
                MediaManager.startQueue(activity.mediaBrowserListenableFuture, ArrayList(uiState.songs), 0)
                activity.setBottomSheetInPeek(true)
            },
            onShuffleAllClick = {
                val shuffled = uiState.songs.shuffled()
                MediaManager.startQueue(activity.mediaBrowserListenableFuture, ArrayList(shuffled), 0)
                activity.setBottomSheetInPeek(true)
            },
            onPinClick = viewModel::togglePinned,
            onEditClick = {
                uiState.playlist?.let { targetPlaylist ->
                    navController.navigate(
                        DialogRouteScreen.route { _, onClose ->
                            PlaylistEditorRouteDialog(
                                tracks = null,
                                playlist = targetPlaylist,
                                onDismiss = onClose,
                            onPlaylistsChanged = viewModel::refreshCurrent,
                            )
                        },
                    )
                }
            },
            onDeleteClick = {
                uiState.playlist?.let { targetPlaylist ->
                    navController.navigate(
                        DialogRouteScreen.route { _, onClose ->
                            PlaylistEditorRouteDialog(
                                tracks = null,
                                playlist = targetPlaylist,
                                onDismiss = onClose,
                            onPlaylistsChanged = viewModel::refreshCurrent,
                            )
                        },
                    )
                }
            },
            onNavigateBack = { navController.navigateUp() },
        )
    }
    private fun isSongDownloaded(activity: MainActivity, song: Child): Boolean {
        return if (Preferences.getDownloadDirectoryUri() == null) {
            DownloadUtil.getDownloadTracker(activity).isDownloaded(song.id)
        } else {
            ExternalAudioReader.getUri(song) != null
        }
    }
}
