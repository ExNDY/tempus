package com.cappielloantonio.tempo.ui.song
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.BundleCompat
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.cappielloantonio.tempo.di.getSongListPageViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.navigation.BottomMenuConfig
import com.cappielloantonio.tempo.navigation.DefaultScreenNameExtension.defaultScreenNameWithOptionalParams
import com.cappielloantonio.tempo.navigation.Screen
import com.cappielloantonio.tempo.navigation.ScreenNameExtension.screenNameWithOptionalParams
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Genre
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.util.requireActivity
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.ExternalAudioReader
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.viewmodel.SongListPageArgs
import java.util.ArrayList
object SongListPageRouteScreen : Screen.DefaultScreen {
    private const val TYPE = "type"
    private const val ARGS_TOKEN = "argsToken"
    private const val FILTERS = "filters_list"
    private const val FILTER_NAMES = "filter_name_list"
    private const val YEAR = "year_object"
    override val navArgs = listOf(
        navArgument(TYPE) {
            type = NavType.StringType
            nullable = false
        },
        navArgument(ARGS_TOKEN) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
    )
    override val screenName: String = defaultScreenNameWithOptionalParams(
        params = listOf(TYPE),
        optionalParams = listOf(ARGS_TOKEN),
    )
    override fun bottomMenuConfig(): BottomMenuConfig = BottomMenuConfig.Hidden
    fun route(args: SongListPageArgs): String {
        val token = SongListRouteArgsStore.put(
            Bundle().apply {
                putString(TYPE, args.type)
                putSerializable(Constants.ARTIST_OBJECT, args.artist)
                putSerializable(Constants.GENRE_OBJECT, args.genre)
                putSerializable(Constants.ALBUM_OBJECT, args.album)
                putStringArrayList(FILTERS, ArrayList(args.filters))
                putStringArrayList(FILTER_NAMES, ArrayList(args.filterNames))
                putInt(YEAR, args.year)
            },
        )
        return screenNameWithOptionalParams(
            params = listOf(args.type),
            optionalParams = listOf(ARGS_TOKEN to token),
        )
    }
    @Composable
    override fun Content(navController: NavController, args: Bundle?) {
        val activity = LocalContext.current.requireActivity<MainActivity>()
        val routeArgs = requireNotNull(args) { "Missing arguments for SongListPageRouteScreen" }
        val type = routeArgs.getString(TYPE) ?: error("Missing type for SongListPageRouteScreen")
        val storedArgs = remember(routeArgs.getString(ARGS_TOKEN)) {
            SongListRouteArgsStore.consume(routeArgs.getString(ARGS_TOKEN))
        }
        val screenArgs = resolveArgs(type, storedArgs)
            ?: error("Missing route args for SongListPageRouteScreen")
        val viewModel = getViewModel { getSongListPageViewModel().apply { onStart(screenArgs) } }
        val uiState by viewModel.uiState.collectAsState()
        val refreshEvent by ExternalAudioReader.getRefreshEvents().observeAsState()
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
        SongListPageScreen(
            uiState = uiState,
            downloadedSongIds = downloadedSongIds,
            currentSongId = uiState.currentSongId,
            isPlaying = uiState.isPlaying,
            onSongClick = { songs, index ->
                val targetSongId = songs.getOrNull(index)?.id
                if (targetSongId != null && targetSongId == uiState.currentSongId) {
                    activity.mediaBrowserListenableFuture?.get()?.let { browser ->
                        if (browser.isPlaying) browser.pause() else browser.play()
                    }
                } else {
                    MediaManager.startQueue(activity.mediaBrowserListenableFuture, ArrayList(songs), index)
                    activity.setBottomSheetInPeek(true)
                }
            },
            onSongLongClick = { song, index ->
                navController.navigate(SongBottomSheetRouteScreen.route(song, itemPosition = index))
            },
            onPlayAllClick = { songs ->
                if (songs.isNotEmpty()) {
                    MediaManager.startQueue(activity.mediaBrowserListenableFuture, ArrayList(songs), 0)
                    activity.setBottomSheetInPeek(true)
                }
            },
            onShuffleAllClick = { songs ->
                if (songs.isNotEmpty()) {
                    val shuffled = songs.shuffled()
                    MediaManager.startQueue(activity.mediaBrowserListenableFuture, ArrayList(shuffled), 0)
                    activity.setBottomSheetInPeek(true)
                }
            },
            onNavigateBack = { navController.navigateUp() },
        )
    }
    private fun resolveArgs(type: String, bundle: Bundle?): SongListPageArgs? {
        return when (type) {
            Constants.MEDIA_BY_GENRE,
            Constants.MEDIA_BY_ARTIST,
            Constants.MEDIA_BY_GENRES,
            Constants.MEDIA_BY_YEAR,
            Constants.MEDIA_FROM_ALBUM -> {
                bundle ?: return null
                SongListPageArgs(
                    type = type,
                    artist = BundleCompat.getSerializable(bundle, Constants.ARTIST_OBJECT, ArtistID3::class.java),
                    genre = BundleCompat.getSerializable(bundle, Constants.GENRE_OBJECT, Genre::class.java),
                    album = BundleCompat.getSerializable(bundle, Constants.ALBUM_OBJECT, AlbumID3::class.java),
                    filters = bundle.getStringArrayList(FILTERS) ?: emptyList(),
                    filterNames = bundle.getStringArrayList(FILTER_NAMES) ?: emptyList(),
                    year = bundle.getInt(YEAR),
                )
            }
            Constants.MEDIA_RECENTLY_PLAYED,
            Constants.MEDIA_MOST_PLAYED,
            Constants.MEDIA_RECENTLY_ADDED,
            Constants.MEDIA_DOWNLOADED,
            Constants.MEDIA_STARRED -> SongListPageArgs(type = type)
            else -> null
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
