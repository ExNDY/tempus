package com.cappielloantonio.tempo.ui.album
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getAlbumPageViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.navigation.BottomMenuConfig
import com.cappielloantonio.tempo.navigation.DefaultScreenNameExtension.defaultScreenNameWithParams
import com.cappielloantonio.tempo.navigation.Screen
import com.cappielloantonio.tempo.navigation.ScreenNameExtension.screenNameWithParams
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.util.requireActivity
import com.cappielloantonio.tempo.ui.dialog.DialogRouteScreen
import com.cappielloantonio.tempo.ui.dialog.PlaylistChooserRouteDialog
import com.cappielloantonio.tempo.ui.dialog.RatingRouteDialog
import com.cappielloantonio.tempo.ui.artist.ArtistPageRouteScreen
import com.cappielloantonio.tempo.ui.song.SongBottomSheetRouteScreen
import com.cappielloantonio.tempo.ui.song.SongListPageRouteScreen
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.ExternalAudioReader
import com.cappielloantonio.tempo.util.ExternalAudioWriter
import com.cappielloantonio.tempo.util.MappingUtil
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.viewmodel.SongListPageArgs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
object AlbumPageRouteScreen : Screen.DefaultScreen {
    private const val ALBUM_ID = "albumId"
    override val navArgs = listOf(
        navArgument(ALBUM_ID) {
            type = NavType.StringType
            nullable = false
        },
    )
    override val screenName: String = defaultScreenNameWithParams(ALBUM_ID)
    override fun bottomMenuConfig(): BottomMenuConfig = BottomMenuConfig.Hidden
    fun route(albumId: String): String {
        return screenNameWithParams(Uri.encode(albumId))
    }
    @Composable
    override fun Content(navController: NavController, args: Bundle?) {
        val activity = LocalContext.current.requireActivity<MainActivity>()
        val context = LocalContext.current
        val albumId = args?.getString(ALBUM_ID)
            ?.takeIf { it.isNotBlank() }
            ?: error("Missing albumId for AlbumPageRouteScreen")
        val albumPageViewModel = getViewModel { getAlbumPageViewModel().apply { onStart(albumId) } }
        val uiState by albumPageViewModel.uiState.collectAsState()
        val refreshEvent by ExternalAudioReader.getRefreshEvents().observeAsState()
        val coroutineScope = rememberCoroutineScope()
        var pendingDownloadedSongIds by remember(albumId) { mutableStateOf(emptySet<String>()) }
        val downloadedSongIds = remember(
            uiState.songs,
            refreshEvent,
            pendingDownloadedSongIds,
            Preferences.getDownloadDirectoryUri(),
        ) {
            uiState.songs
                .asSequence()
                .filter { song -> isSongDownloaded(activity, song) }
                .map { it.id }
                .toSet() + pendingDownloadedSongIds
        }
        AlbumPageScreen(
            uiState = uiState,
            downloadedSongIds = downloadedSongIds,
            currentSongId = uiState.currentSongId,
            isPlaying = uiState.isPlaying,
            onFavoriteClick = albumPageViewModel::setFavorite,
            onPlayClick = {
                MediaManager.startQueue(activity.mediaBrowserListenableFuture, uiState.songs, 0)
                activity.setBottomSheetInPeek(true)
            },
            onInstantMixClick = {
                Toast.makeText(context, R.string.bottom_sheet_generating_instant_mix, Toast.LENGTH_SHORT).show()
                coroutineScope.launch {
                    val media = albumPageViewModel.getAlbumInstantMix().first().toMutableList()
                    if (media.isNotEmpty()) {
                        MusicUtil.ratingFilter(media)
                        MediaManager.startQueue(activity.mediaBrowserListenableFuture, media, 0)
                        activity.setBottomSheetInPeek(true)
                    }
                }
            },
            onShuffleClick = {
                val shuffled = uiState.songs.shuffled()
                MediaManager.startQueue(activity.mediaBrowserListenableFuture, shuffled, 0)
                activity.setBottomSheetInPeek(true)
            },
            onDownloadClick = {
                pendingDownloadedSongIds = pendingDownloadedSongIds + uiState.songs.map { it.id }
                downloadAlbum(activity, uiState.songs)
            },
            onRateClick = {
                val album = uiState.album ?: return@AlbumPageScreen
                navController.navigate(
                    DialogRouteScreen.route { _, onClose ->
                        RatingRouteDialog(
                            album = album,
                            onDismiss = onClose,
                        )
                    },
                )
            },
            onAddToPlaylistClick = {
                navController.navigate(
                    DialogRouteScreen.route { _, onClose ->
                        PlaylistChooserRouteDialog(
                            tracks = ArrayList(uiState.songs),
                            onDismiss = onClose,
                            onPlaylistsChanged = {},
                        )
                    },
                )
            },
            onArtistClick = {
                coroutineScope.launch {
                    val artist = albumPageViewModel.getArtist().first()
                    val artistId = artist?.id
                    if (!artistId.isNullOrBlank()) {
                        navController.navigate(ArtistPageRouteScreen.route(artistId))
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.album_error_retrieving_artist),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            },
            onYearClick = { year ->
                navController.navigate(
                    SongListPageRouteScreen.route(
                        SongListPageArgs(
                            type = Constants.MEDIA_BY_YEAR,
                            year = year,
                        ),
                    ),
                )
            },
            onSongClick = { index ->
                MediaManager.startQueue(activity.mediaBrowserListenableFuture, uiState.songs, index)
                activity.setBottomSheetInPeek(true)
            },
            onSongLongClick = { song ->
                navController.navigate(SongBottomSheetRouteScreen.route(song))
            },
            onNavigateBack = { navController.navigateUp() },
        )
    }
    private fun downloadAlbum(activity: MainActivity, songs: List<Child>) {
        if (Preferences.getDownloadDirectoryUri() == null) {
            DownloadUtil.getDownloadTracker(activity).download(
                MappingUtil.mapDownloads(songs),
                songs.map { Download(it) },
            )
        } else {
            songs.forEach { child ->
                ExternalAudioWriter.downloadToUserDirectory(activity, child)
            }
        }
    }
    private fun isSongDownloaded(activity: MainActivity, song: Child): Boolean {
        return if (Preferences.getDownloadDirectoryUri() == null) {
            DownloadUtil.getDownloadTracker(activity).isDownloaded(song.id)
        } else {
            ExternalAudioReader.getUri(song) != null
        }
    }
}
