package com.cappielloantonio.tempo.ui.fragment

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.FragmentPlayerBottomSheetBinding
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.dialog.PlaybackSpeedDialog
import com.cappielloantonio.tempo.ui.dialog.PlaylistChooserDialog
import com.cappielloantonio.tempo.ui.dialog.SleepTimerDialog
import com.cappielloantonio.tempo.ui.dialog.TrackInfoDialog
import com.cappielloantonio.tempo.ui.player.PlayerHeader
import com.cappielloantonio.tempo.ui.player.PlayerScreen
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.*
import com.cappielloantonio.tempo.viewmodel.PlaybackViewModel
import com.cappielloantonio.tempo.viewmodel.PlayerBottomSheetViewModel
import com.cappielloantonio.tempo.viewmodel.RatingViewModel
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

@UnstableApi
class PlayerBottomSheetFragment : Fragment() {

    private val playerBottomSheetViewModel: PlayerBottomSheetViewModel by viewModel()
    private val ratingViewModel: RatingViewModel by viewModel()
    private lateinit var playbackViewModel: PlaybackViewModel
    private var mediaBrowserFuture: ListenableFuture<MediaBrowser>? = null
    private var mediaBrowser: MediaBrowser? = null
    private var browserListener: Player.Listener? = null
    private var progressUpdateJob: Job? = null
    private var _binding: FragmentPlayerBottomSheetBinding? = null
    private val binding get() = _binding!!
    private var requestedVerticalPage by mutableIntStateOf(0)
    private var verticalPageRequestId by mutableIntStateOf(0)
    private var requestedHorizontalPage by mutableIntStateOf(0)
    private var horizontalPageRequestId by mutableIntStateOf(0)
    private var isVerticalPagerDraggable by mutableStateOf(true)
    private var playerProgressMs by mutableLongStateOf(0L)
    private var playerDurationMs by mutableLongStateOf(0L)
    private var shuffleModeEnabled by mutableStateOf(false)
    private var repeatMode by mutableIntStateOf(Player.REPEAT_MODE_OFF)
    private var playbackState by mutableIntStateOf(Player.STATE_IDLE)
    private var isPlayPauseEnabled by mutableStateOf(false)
    private var isPreviousEnabled by mutableStateOf(false)
    private var isNextEnabled by mutableStateOf(false)
    private var isSeekControlsEnabled by mutableStateOf(false)
    private var lastSyncedMediaId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        playbackViewModel = ViewModelProvider(requireActivity())[PlaybackViewModel::class.java]
        _binding = FragmentPlayerBottomSheetBinding.inflate(inflater, container, false)

        setupBody()
        setupHeader()

        return binding.root
    }

    private fun setupBody() {
        binding.playerBodyLayout.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    val uiState by playerBottomSheetViewModel.uiState.collectAsState()
                    val currentSongId by playbackViewModel.currentSongId.collectAsState()
                    val isPlaying by playbackViewModel.isPlaying.collectAsState()

                    LaunchedEffect(playerBottomSheetViewModel) {
                        playerBottomSheetViewModel.actions.collect { action ->
                            when (action) {
                                is PlayerBottomSheetViewModel.Action.RequestDownload -> {
                                    downloadSongs(listOf(action.media))
                                }
                            }
                        }
                    }

                    PlayerScreen(
                        uiState = uiState,
                        isPlaying = isPlaying,
                        playbackState = playbackState,
                        progress = playerProgressMs,
                        duration = playerDurationMs,
                        shuffleModeEnabled = shuffleModeEnabled,
                        repeatMode = repeatMode,
                        currentSongId = currentSongId,
                        isPlayPauseEnabled = isPlayPauseEnabled,
                        isPreviousEnabled = isPreviousEnabled,
                        isNextEnabled = isNextEnabled,
                        onPlayPauseClick = { mediaBrowserFuture?.get()?.let { if (it.isPlaying) it.pause() else it.play() } },
                        onPreviousClick = { mediaBrowserFuture?.get()?.seekToPrevious() },
                        onNextClick = { mediaBrowserFuture?.get()?.seekToNext() },
                        onShuffleClick = { mediaBrowserFuture?.get()?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } },
                        onRepeatClick = { mediaBrowserFuture?.get()?.let { it.repeatMode = (it.repeatMode + 1) % 3 } },
                        onSeek = { seekToPosition(it) },
                        onFavoriteClick = { playerBottomSheetViewModel.setFavorite(uiState.currentSong) },
                        onRatingChange = { rating -> 
                            uiState.currentSong?.let { 
                                it.userRating = rating
                                ratingViewModel.setSong(it)
                                ratingViewModel.rate(rating)
                                playerBottomSheetViewModel.refreshMediaInfo(it)
                            }
                        },
                        onPlaybackSpeedClick = { showPlaybackSpeedDialog() },
                        onSleepTimerClick = { showSleepTimerDialog() },
                        onEqualizerClick = { navigateToEqualizer() },
                        onTrackInfoClick = { showTrackInfoDialog() },
                        onTitleClick = { navigateToAlbum(uiState.currentAlbum) },
                        onArtistClick = { navigateToArtist(uiState.currentArtist) },
                        onQueueSongClick = { index -> mediaBrowserFuture?.get()?.seekTo(index, 0) },
                        onQueueRemoveClick = { index -> 
                            mediaBrowserFuture?.get()?.let {
                                val mediaList = ArrayList<Child>(uiState.queue)
                                MediaManager.remove(mediaBrowserFuture, mediaList, index)
                            }
                        },
                        onQueueShuffleClick = { 
                            mediaBrowserFuture?.get()?.let { browser ->
                                val start = browser.currentMediaItemIndex + 1
                                val end = browser.mediaItemCount - 1
                                if (start < end) {
                                    val mediaList = ArrayList<Child>(uiState.queue)
                                    MediaManager.shuffle(mediaBrowserFuture, mediaList, start, end)
                                }
                            }
                        },
                        onQueueClearClick = { 
                            mediaBrowserFuture?.get()?.let { browser ->
                                val start = browser.currentMediaItemIndex + 1
                                val end = browser.mediaItemCount
                                val mediaList = ArrayList<Child>(uiState.queue)
                                MediaManager.removeRange(mediaBrowserFuture, mediaList, start, end)
                            }
                        },
                        onQueueSaveToPlaylistClick = { 
                            val songs = ArrayList<Child>(uiState.queue)
                            if (songs.isNotEmpty()) {
                                val bundle = Bundle().apply { putSerializable(Constants.TRACKS_OBJECT, songs) }
                                PlaylistChooserDialog().apply { arguments = bundle }.show(parentFragmentManager, null)
                            }
                        },
                        onQueueDownloadAllClick = { 
                            val songs = ArrayList<Child>(uiState.queue)
                            downloadSongs(songs)
                        },
                        onQueueLoadQueueClick = { 
                            lifecycleScope.launch {
                                val playQueue = playerBottomSheetViewModel.getPlayQueue().asFlow().first()
                                val entries = playQueue?.entries
                                if (playQueue != null && entries != null && entries.isNotEmpty()) {
                                    val index = entries.indexOfFirst { entry -> entry.id == playQueue.current }.coerceAtLeast(0)
                                    MediaManager.startQueue(mediaBrowserFuture, ArrayList(entries), index)
                                } else {
                                    android.widget.Toast.makeText(
                                        requireContext(),
                                        getString(R.string.player_queue_not_found),
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        onDownloadClick = { uiState.currentSong?.let { downloadSongs(listOf(it)) } },
                        onAddToPlaylistClick = { 
                            uiState.currentSong?.let { song ->
                                val bundle = Bundle().apply { putSerializable(Constants.TRACKS_OBJECT, arrayListOf(song)) }
                                PlaylistChooserDialog().apply { arguments = bundle }.show(parentFragmentManager, null)
                            }
                        },
                        onInstantMixClick = { 
                            uiState.currentSong?.let { song ->
                                lifecycleScope.launch {
                                    val media = playerBottomSheetViewModel.getMediaInstantMix(song).first()
                                    if (media.isNotEmpty()) {
                                        MediaManager.enqueue(mediaBrowserFuture, ArrayList(media), true)
                                    }
                                }
                            }
                        },
                        onSaveQueueClick = {
                            if (playerBottomSheetViewModel.savePlayQueue()) {
                                android.widget.Toast.makeText(
                                    requireContext(),
                                    getString(R.string.player_queue_save_queue_success),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onLyricsLineClick = { position -> mediaBrowserFuture?.get()?.seekTo(position) },
                        onLyricsSyncToggle = { playerBottomSheetViewModel.changeSyncLyricsState() },
                        onLyricsDownloadClick = {
                            val messageRes = if (playerBottomSheetViewModel.downloadCurrentLyrics()) {
                                R.string.player_lyrics_download_success
                            } else {
                                R.string.player_lyrics_download_failure
                            }
                            android.widget.Toast.makeText(
                                requireContext(),
                                getString(messageRes),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        onChipClick = { type, id -> (requireActivity() as MainActivity).openAssetLink(AssetLinkUtil.buildAssetLink(type, id)!!) },
                        onChipLongClick = { type, id -> AssetLinkUtil.copyToClipboard(requireContext(), AssetLinkUtil.buildAssetLink(type, id)!!) },
                        isSyncEnabled = Preferences.isSyncronizationEnabled(),
                        requestedVerticalPage = requestedVerticalPage,
                        verticalPageRequestId = verticalPageRequestId,
                        requestedHorizontalPage = requestedHorizontalPage,
                        horizontalPageRequestId = horizontalPageRequestId,
                        isVerticalPagerDraggable = isVerticalPagerDraggable,
                    )
                }
            }
        }
    }

    private fun setupHeader() {
        binding.playerHeaderLayout.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    val uiState by playerBottomSheetViewModel.uiState.collectAsState()
                    val isPlaying by playbackViewModel.isPlaying.collectAsState()
                    val progress = if (playerDurationMs > 0L) {
                        playerProgressMs.toFloat() / playerDurationMs.toFloat()
                    } else {
                        0f
                    }

                    PlayerHeader(
                        currentSong = uiState.currentSong,
                        description = uiState.description,
                        isPlaying = isPlaying,
                        progress = progress,
                        isNextEnabled = isNextEnabled,
                        isSeekControlsEnabled = isSeekControlsEnabled,
                        onHeaderClick = { (requireActivity() as MainActivity).expandBottomSheet() },
                        onPlayPauseClick = { mediaBrowserFuture?.get()?.let { if (it.isPlaying) it.pause() else it.play() } },
                        onNextClick = {
                            if (isNextEnabled) {
                                mediaBrowserFuture?.get()?.seekToNext()
                            }
                        },
                        onSeekBackClick = {
                            if (isSeekControlsEnabled) {
                                mediaBrowserFuture?.get()?.seekBack()
                            }
                        },
                        onSeekForwardClick = {
                            if (isSeekControlsEnabled) {
                                mediaBrowserFuture?.get()?.seekForward()
                            }
                        }
                    )
                }
            }
        }
    }

    private fun bindMediaBrowser(browser: MediaBrowser) {
        unbindMediaBrowser()

        mediaBrowser = browser
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                dispatchPlayerEvent(player, events)
            }
        }

        browserListener = listener
        browser.addListener(listener)
        dispatchInitialPlayerState(browser)
    }

    private fun unbindMediaBrowser() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null

        browserListener?.let { listener ->
            mediaBrowser?.removeListener(listener)
        }

        browserListener = null
        mediaBrowser = null
    }

    private fun syncFromPlayer(player: Player, forceMetadataSync: Boolean) {
        lastSyncedMediaId = PlayerBottomSheetBrowserSync.syncFromPlayer(
            player = player,
            playbackViewModel = playbackViewModel,
            playerBottomSheetViewModel = playerBottomSheetViewModel,
            lastSyncedMediaId = lastSyncedMediaId,
            forceMetadataSync = forceMetadataSync
        )
    }

    private fun dispatchPlayerEvent(player: Player, events: Player.Events) {
        val lifecycleOwner = viewLifecycleOwnerLiveData.value ?: return
        lifecycleOwner.lifecycleScope.launch {
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
                repeatMode = player.repeatMode
                Preferences.setRepeatMode(player.repeatMode)
            }

            if (events.contains(Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)) {
                shuffleModeEnabled = player.shuffleModeEnabled
                Preferences.setShuffleModeEnabled(player.shuffleModeEnabled)
            }

            if (events.contains(Player.EVENT_POSITION_DISCONTINUITY) ||
                events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) ||
                events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED)
            ) {
                updateTransportState(player)
            }
        }
    }

    private fun dispatchInitialPlayerState(player: Player) {
        val lifecycleOwner = viewLifecycleOwnerLiveData.value ?: return
        lifecycleOwner.lifecycleScope.launch {
            syncFromPlayer(player, forceMetadataSync = true)
            repeatMode = player.repeatMode
            shuffleModeEnabled = player.shuffleModeEnabled
            Preferences.setRepeatMode(player.repeatMode)
            Preferences.setShuffleModeEnabled(player.shuffleModeEnabled)
            updateTransportState(player)
        }
    }

    private fun updateTransportState(player: Player) {
        playbackState = player.playbackState
        playerProgressMs = player.currentPosition.coerceAtLeast(0L)
        playerDurationMs = player.duration.takeIf { it > 0L } ?: 0L
        isPlayPauseEnabled =
            player.currentMediaItem != null &&
                player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)
        isPreviousEnabled =
            player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM) &&
                player.hasPreviousMediaItem()
        isNextEnabled =
            player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM) &&
                player.hasNextMediaItem()
        isSeekControlsEnabled =
            (player.isCommandAvailable(Player.COMMAND_SEEK_BACK) ||
                player.isCommandAvailable(Player.COMMAND_SEEK_FORWARD)) &&
                (player.isCurrentMediaItemSeekable || player.duration > 0L)

        val shouldTrackProgress = player.playbackState == Player.STATE_READY && player.playWhenReady
        if (shouldTrackProgress) {
            startProgressUpdates()
        } else {
            progressUpdateJob?.cancel()
            progressUpdateJob = null
        }
    }

    private fun startProgressUpdates() {
        if (progressUpdateJob?.isActive == true) return

        progressUpdateJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                val browser = mediaBrowser ?: break
                playerProgressMs = browser.currentPosition.coerceAtLeast(0L)
                playerDurationMs = browser.duration.takeIf { it > 0L } ?: 0L
                delay(1000)
            }
        }
    }

    private fun seekToPosition(positionMs: Long) {
        playerProgressMs = positionMs.coerceAtLeast(0L)
        mediaBrowserFuture?.get()?.seekTo(positionMs)
    }

    private fun showPlaybackSpeedDialog() {
        PlaybackSpeedDialog().apply {
            setPlaybackSpeedListener { speed ->
                mediaBrowserFuture?.get()?.setPlaybackParameters(androidx.media3.common.PlaybackParameters(speed))
            }
        }.show(parentFragmentManager, null)
    }

    private fun showSleepTimerDialog() {
        SleepTimerDialog().show(parentFragmentManager, null)
    }

    private fun navigateToEqualizer() {
        (requireActivity() as MainActivity).navController.navigate(R.id.equalizerFragment)
        (requireActivity() as MainActivity).collapseBottomSheetDelayed()
    }

    private fun showTrackInfoDialog() {
        mediaBrowserFuture?.get()?.mediaMetadata?.let {
            TrackInfoDialog(it).show(parentFragmentManager, null)
        }
    }

    private fun navigateToAlbum(album: com.cappielloantonio.tempo.subsonic.models.AlbumID3?) {
        album?.let {
            val bundle = Bundle().apply { putSerializable(Constants.ALBUM_OBJECT, it) }
            (requireActivity() as MainActivity).navController.navigate(R.id.albumPageFragment, bundle)
            (requireActivity() as MainActivity).collapseBottomSheetDelayed()
        }
    }

    private fun navigateToArtist(artist: com.cappielloantonio.tempo.subsonic.models.ArtistID3?) {
        artist?.let {
            val bundle = Bundle().apply { putSerializable(Constants.ARTIST_OBJECT, it) }
            (requireActivity() as MainActivity).navController.navigate(R.id.artistPageFragment, bundle)
            (requireActivity() as MainActivity).collapseBottomSheetDelayed()
        }
    }

    private fun downloadSongs(songs: List<Child>) {
        if (Preferences.getDownloadDirectoryUri() == null) {
            DownloadUtil.getDownloadTracker(requireContext()).download(
                MappingUtil.mapDownloads(songs),
                songs.map { Download(it) }
            )
        } else {
            songs.forEach { ExternalAudioWriter.downloadToUserDirectory(requireContext(), it) }
        }
    }

    override fun onStart() {
        super.onStart()
        mediaBrowserFuture = MediaBrowser.Builder(
            requireContext(),
            SessionToken(requireContext(), ComponentName(requireContext(), MediaService::class.java))
        ).buildAsync().also { browserFuture ->
            browserFuture.addListener({
                val browser = try {
                    browserFuture.get()
                } catch (_: Exception) {
                    null
                }
                browser?.let { bindMediaBrowser(it) }
            }, MoreExecutors.directExecutor())
        }
    }

    override fun onStop() {
        unbindMediaBrowser()
        mediaBrowserFuture?.let { MediaController.releaseFuture(it) }
        mediaBrowserFuture = null
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun getPlayerHeader(): View {
        return binding.playerHeaderLayout
    }

    fun goBackToFirstPage() {
        requestedVerticalPage = 0
        verticalPageRequestId += 1
        requestedHorizontalPage = 0
        horizontalPageRequestId += 1
    }

    fun goToQueuePage() {
        requestedVerticalPage = 1
        verticalPageRequestId += 1
    }

    fun setPlayerControllerVerticalPagerDraggableState(isDraggable: Boolean) {
        isVerticalPagerDraggable = isDraggable
    }
}
