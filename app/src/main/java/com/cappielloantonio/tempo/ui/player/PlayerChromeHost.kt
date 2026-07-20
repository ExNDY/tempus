package com.cappielloantonio.tempo.ui.player
import android.content.ComponentName
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.asFlow
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getPlayerBottomSheetViewModel
import com.cappielloantonio.tempo.di.getRatingViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.playback.PlaybackState
import com.cappielloantonio.tempo.playback.PlaybackStateStore
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.dialog.DialogRouteScreen
import com.cappielloantonio.tempo.ui.dialog.PlaybackSpeedRouteDialog
import com.cappielloantonio.tempo.ui.dialog.PlaylistChooserRouteDialog
import com.cappielloantonio.tempo.ui.dialog.SleepTimerRouteDialog
import com.cappielloantonio.tempo.ui.dialog.TrackInfoRouteDialog
import com.cappielloantonio.tempo.util.AssetLinkUtil
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.ExternalAudioWriter
import com.cappielloantonio.tempo.util.MappingUtil
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.util.requireActivity
import com.cappielloantonio.tempo.viewmodel.PlayerBottomSheetViewModel
import com.cappielloantonio.tempo.viewmodel.PlayerUiState
import com.cappielloantonio.tempo.viewmodel.RatingViewModel
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
data class PlayerTransportState(
    val playbackState: Int = Player.STATE_IDLE,
    val shuffleModeEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val isPlayPauseEnabled: Boolean = false,
    val isPreviousEnabled: Boolean = false,
    val isNextEnabled: Boolean = false,
    val isSeekControlsEnabled: Boolean = false,
)
data class PlayerChromeState(
    val uiState: PlayerUiState,
    val playbackSnapshot: PlaybackState,
    val transport: PlayerTransportState,
    val progressRefreshToken: Int,
) {
    val hasPlayback: Boolean
        get() = uiState.currentSong != null || playbackSnapshot.currentSongId != null
}
data class PlayerChromeActions(
    val onPlayPauseClick: () -> Unit,
    val onPreviousClick: () -> Unit,
    val onNextClick: () -> Unit,
    val onShuffleClick: () -> Unit,
    val onRepeatClick: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onSeekBackClick: () -> Unit,
    val onSeekForwardClick: () -> Unit,
    val onFavoriteClick: () -> Unit,
    val onRatingChange: (Int) -> Unit,
    val onPlaybackSpeedClick: () -> Unit,
    val onSleepTimerClick: () -> Unit,
    val onEqualizerClick: () -> Unit,
    val onTrackInfoClick: () -> Unit,
    val onTitleClick: () -> Unit,
    val onArtistClick: () -> Unit,
    val onQueueSongClick: (Int) -> Unit,
    val onQueueRemoveClick: (Int) -> Unit,
    val onQueueShuffleClick: () -> Unit,
    val onQueueClearClick: () -> Unit,
    val onQueueSaveToPlaylistClick: () -> Unit,
    val onQueueDownloadAllClick: () -> Unit,
    val onQueueLoadQueueClick: () -> Unit,
    val onDownloadClick: () -> Unit,
    val onAddToPlaylistClick: () -> Unit,
    val onInstantMixClick: () -> Unit,
    val onSaveQueueClick: () -> Unit,
    val onLyricsLineClick: (Long) -> Unit,
    val onLyricsSyncToggle: () -> Unit,
    val onLyricsDownloadClick: () -> Unit,
    val onChipClick: (String, String) -> Unit,
    val onChipLongClick: (String, String) -> Unit,
)
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalPlayerSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
val LocalPlayerAnimatedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }
val LocalPlayerChromeState = staticCompositionLocalOf<PlayerChromeState?> { null }
val LocalPlayerChromeActions = staticCompositionLocalOf<PlayerChromeActions?> { null }
val LocalPlayerProgressController = staticCompositionLocalOf<PlayerProgressController?> { null }
internal object PlayerSharedContentKeys {
    const val Background = "player-background"
    const val CoverArt = "player-cover-art"
}
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlayerChromeHost(
    onNavigateToRoute: (String) -> Unit,
    onChooseArtist: (List<ArtistID3>) -> Unit,
    content: @Composable (PlayerChromeState, PlayerChromeActions) -> Unit,
) {
    val activity = LocalContext.current.requireActivity<MainActivity>()
    val coroutineScope = rememberCoroutineScope()
    val playerBottomSheetViewModel: PlayerBottomSheetViewModel = getViewModel {
        getPlayerBottomSheetViewModel()
    }
    val ratingViewModel: RatingViewModel = getViewModel {
        getRatingViewModel()
    }
    val playbackStateStore = remember {
        GlobalContext.get().get<PlaybackStateStore>()
    }
    val uiState by playerBottomSheetViewModel.uiState.collectAsState()
    val playbackSnapshot by playbackStateStore.state.collectAsState()
    var mediaBrowserFuture by remember { mutableStateOf<ListenableFuture<MediaBrowser>?>(null) }
    var mediaBrowser by remember { mutableStateOf<MediaBrowser?>(null) }
    var lastSyncedMediaId by remember { mutableStateOf<String?>(null) }
    var transport by remember { mutableStateOf(PlayerTransportState()) }
    var progressRefreshToken by remember { mutableIntStateOf(0) }
    val progressController = remember(mediaBrowser) {
        MediaPlayerProgressController(mediaBrowser)
    }
    fun updateTransportState(player: Player) {
        transport = PlayerTransportState(
            playbackState = player.playbackState,
            shuffleModeEnabled = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
            isPlayPauseEnabled = player.currentMediaItem != null &&
                player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE),
            isPreviousEnabled = player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM) &&
                player.hasPreviousMediaItem(),
            isNextEnabled = player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM) &&
                player.hasNextMediaItem(),
            isSeekControlsEnabled = (
                player.isCommandAvailable(Player.COMMAND_SEEK_BACK) ||
                    player.isCommandAvailable(Player.COMMAND_SEEK_FORWARD)
                ) && (player.isCurrentMediaItemSeekable || player.duration > 0L),
        )
    }
    fun syncFromPlayer(player: Player, forceMetadataSync: Boolean) {
        lastSyncedMediaId = PlayerBrowserSync.syncFromPlayer(
            player = player,
            playbackStateStore = playbackStateStore,
            playerBottomSheetViewModel = playerBottomSheetViewModel,
            lastSyncedMediaId = lastSyncedMediaId,
            forceMetadataSync = forceMetadataSync
        )
    }
    fun dispatchPlayerEvent(player: Player, events: Player.Events) {
        val forceMetadataSync =
            events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                events.contains(Player.EVENT_MEDIA_METADATA_CHANGED) ||
                events.contains(Player.EVENT_TIMELINE_CHANGED)
        val needsPlaybackSync =
            forceMetadataSync ||
                events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) ||
                events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED)
        if (needsPlaybackSync) {
            syncFromPlayer(player, forceMetadataSync)
        }
        if (events.contains(Player.EVENT_REPEAT_MODE_CHANGED)) {
            Preferences.setRepeatMode(player.repeatMode)
        }
        if (events.contains(Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)) {
            Preferences.setShuffleModeEnabled(player.shuffleModeEnabled)
        }
        if (events.contains(Player.EVENT_POSITION_DISCONTINUITY) ||
            events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
            events.contains(Player.EVENT_TIMELINE_CHANGED) ||
            events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) ||
            events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED) ||
            events.contains(Player.EVENT_REPEAT_MODE_CHANGED) ||
            events.contains(Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)
        ) {
            updateTransportState(player)
            progressRefreshToken++
        }
    }
    fun dispatchInitialPlayerState(player: Player) {
        syncFromPlayer(player, forceMetadataSync = true)
        Preferences.setRepeatMode(player.repeatMode)
        Preferences.setShuffleModeEnabled(player.shuffleModeEnabled)
        updateTransportState(player)
        progressRefreshToken++
    }
    fun seekToPosition(positionMs: Long) {
        progressController.seekTo(positionMs)
    }
    fun downloadSongs(songs: List<Child>) {
        if (Preferences.getDownloadDirectoryUri() == null) {
            DownloadUtil.getDownloadTracker(activity).download(
                MappingUtil.mapDownloads(songs),
                songs.map { Download(it) }
            )
        } else {
            songs.forEach { ExternalAudioWriter.downloadToUserDirectory(activity, it) }
        }
    }
    DisposableEffect(Unit) {
        val browserFuture = MediaBrowser.Builder(
            activity,
            SessionToken(activity, ComponentName(activity, MediaService::class.java))
        ).buildAsync()
        mediaBrowserFuture = browserFuture
        browserFuture.addListener({
            val browser = try {
                browserFuture.get()
            } catch (_: Exception) {
                null
            }
            coroutineScope.launch {
                mediaBrowser = browser
                browser?.let(::dispatchInitialPlayerState)
            }
        }, MoreExecutors.directExecutor())
        onDispose {
            mediaBrowser = null
            mediaBrowserFuture = null
            MediaController.releaseFuture(browserFuture)
        }
    }
    DisposableEffect(mediaBrowser) {
        val browser = mediaBrowser ?: return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                coroutineScope.launch {
                    dispatchPlayerEvent(player, events)
                }
            }
        }
        browser.addListener(listener)
        onDispose {
            browser.removeListener(listener)
        }
    }
    LaunchedEffect(playerBottomSheetViewModel) {
        playerBottomSheetViewModel.actions.collect { action ->
            when (action) {
                is PlayerBottomSheetViewModel.Action.RequestDownload -> {
                    downloadSongs(listOf(action.media))
                }
            }
        }
    }
    val state = PlayerChromeState(
        uiState = uiState,
        playbackSnapshot = playbackSnapshot,
        transport = transport,
        progressRefreshToken = progressRefreshToken,
    )
    val actions = PlayerChromeActions(
        onPlayPauseClick = {
            mediaBrowser?.let { browser ->
                if (browser.isPlaying) browser.pause() else browser.play()
            }
        },
        onPreviousClick = { mediaBrowser?.seekToPrevious() },
        onNextClick = { mediaBrowser?.seekToNext() },
        onShuffleClick = {
            mediaBrowser?.let { browser ->
                browser.shuffleModeEnabled = !browser.shuffleModeEnabled
            }
        },
        onRepeatClick = {
            mediaBrowser?.let { browser ->
                browser.repeatMode = (browser.repeatMode + 1) % 3
            }
        },
        onSeek = ::seekToPosition,
        onSeekBackClick = { mediaBrowser?.seekBack() },
        onSeekForwardClick = { mediaBrowser?.seekForward() },
        onFavoriteClick = { playerBottomSheetViewModel.setFavorite(uiState.currentSong) },
        onRatingChange = { rating ->
            uiState.currentSong?.let {
                it.userRating = rating
                ratingViewModel.setSong(it)
                ratingViewModel.rate(rating)
                playerBottomSheetViewModel.refreshMediaInfo(it)
            }
        },
        onPlaybackSpeedClick = {
            onNavigateToRoute(
                DialogRouteScreen.route { _, onClose ->
                    PlaybackSpeedRouteDialog(
                        onDismiss = onClose,
                        onSpeedSelected = { speed ->
                            mediaBrowser?.setPlaybackParameters(PlaybackParameters(speed))
                        },
                    )
                },
            )
        },
        onSleepTimerClick = {
            onNavigateToRoute(
                DialogRouteScreen.route { _, onClose ->
                    SleepTimerRouteDialog(onDismiss = onClose)
                },
            )
        },
        onEqualizerClick = {
            activity.openEqualizerRoute()
            activity.collapseBottomSheetDelayed()
        },
        onTrackInfoClick = {
            mediaBrowser?.mediaMetadata?.let { mediaMetadata ->
                onNavigateToRoute(
                    DialogRouteScreen.route { _, onClose ->
                        TrackInfoRouteDialog(
                            mediaMetadata = mediaMetadata,
                            onDismiss = onClose,
                        )
                    },
                )
            }
        },
        onTitleClick = {
            uiState.currentAlbum?.let {
                activity.openAlbumRoute(it.id.orEmpty())
                activity.collapseBottomSheetDelayed()
            }
        },
        onArtistClick = {
            when (val action = playerArtistClickAction(uiState)) {
                PlayerArtistClickAction.None -> Unit
                is PlayerArtistClickAction.OpenArtist -> {
                    activity.openArtistRoute(action.artistId)
                    activity.collapseBottomSheetDelayed()
                }
                is PlayerArtistClickAction.ChooseArtist -> onChooseArtist(action.artists)
            }
        },
        onQueueSongClick = { index -> mediaBrowser?.seekTo(index, 0) },
        onQueueRemoveClick = { index ->
            val mediaList = ArrayList<Child>(uiState.queue)
            MediaManager.remove(mediaBrowserFuture, mediaList, index)
        },
        onQueueShuffleClick = {
            mediaBrowser?.let { browser ->
                val start = browser.currentMediaItemIndex + 1
                val end = browser.mediaItemCount - 1
                if (start < end) {
                    val mediaList = ArrayList<Child>(uiState.queue)
                    MediaManager.shuffle(mediaBrowserFuture, mediaList, start, end)
                }
            }
        },
        onQueueClearClick = {
            mediaBrowser?.let { browser ->
                val start = browser.currentMediaItemIndex + 1
                val end = browser.mediaItemCount
                val mediaList = ArrayList<Child>(uiState.queue)
                MediaManager.removeRange(mediaBrowserFuture, mediaList, start, end)
            }
        },
        onQueueSaveToPlaylistClick = {
            val songs = ArrayList<Child>(uiState.queue)
            if (songs.isNotEmpty()) {
                onNavigateToRoute(
                    DialogRouteScreen.route { _, onClose ->
                        PlaylistChooserRouteDialog(
                            tracks = songs,
                            onDismiss = onClose,
                            onPlaylistsChanged = {},
                        )
                    },
                )
            }
        },
        onQueueDownloadAllClick = {
            val songs = ArrayList<Child>(uiState.queue)
            downloadSongs(songs)
        },
        onQueueLoadQueueClick = {
            coroutineScope.launch {
                val playQueue = playerBottomSheetViewModel.getPlayQueue().asFlow().first()
                val entries = playQueue?.entries
                if (playQueue != null && entries != null && entries.isNotEmpty()) {
                    val index = entries.indexOfFirst { entry -> entry.id == playQueue.current }
                        .coerceAtLeast(0)
                    MediaManager.startQueue(mediaBrowserFuture, ArrayList(entries), index)
                } else {
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.player_queue_not_found),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        },
        onDownloadClick = {
            uiState.currentSong?.let { downloadSongs(listOf(it)) }
        },
        onAddToPlaylistClick = {
            uiState.currentSong?.let { song ->
                onNavigateToRoute(
                    DialogRouteScreen.route { _, onClose ->
                        PlaylistChooserRouteDialog(
                            tracks = arrayListOf(song),
                            onDismiss = onClose,
                            onPlaylistsChanged = {},
                        )
                    },
                )
            }
        },
        onInstantMixClick = {
            uiState.currentSong?.let { song ->
                coroutineScope.launch {
                    val media = playerBottomSheetViewModel.getMediaInstantMix(song).first()
                    if (media.isNotEmpty()) {
                        MediaManager.enqueue(mediaBrowserFuture, ArrayList(media), true)
                    }
                }
            }
        },
        onSaveQueueClick = {
            if (playerBottomSheetViewModel.savePlayQueue()) {
                Toast.makeText(
                    activity,
                    activity.getString(R.string.player_queue_save_queue_success),
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        onLyricsLineClick = { position -> mediaBrowser?.seekTo(position) },
        onLyricsSyncToggle = { playerBottomSheetViewModel.changeSyncLyricsState() },
        onLyricsDownloadClick = {
            val messageRes = if (playerBottomSheetViewModel.downloadCurrentLyrics()) {
                R.string.player_lyrics_download_success
            } else {
                R.string.player_lyrics_download_failure
            }
            Toast.makeText(activity, activity.getString(messageRes), Toast.LENGTH_SHORT).show()
        },
        onChipClick = { type, id ->
            AssetLinkUtil.buildAssetLink(type, id)?.let(activity::openAssetLink)
        },
        onChipLongClick = { type, id ->
            AssetLinkUtil.buildAssetLink(type, id)?.let {
                AssetLinkUtil.copyToClipboard(activity, it)
            }
        },
    )
    SharedTransitionLayout {
        CompositionLocalProvider(
            LocalPlayerSharedTransitionScope provides this,
            LocalPlayerChromeState provides state,
            LocalPlayerChromeActions provides actions,
            LocalPlayerProgressController provides progressController,
        ) {
            content(state, actions)
        }
    }
}
