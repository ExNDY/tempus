package com.cappielloantonio.tempo.ui.fragment

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.fragment.findNavController
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getPlaylistPageViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.dialog.PlaylistEditorDialog
import com.cappielloantonio.tempo.ui.playlist.PlaylistPageScreen
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.ExternalAudioReader
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.viewmodel.PlaybackViewModel
import com.google.common.util.concurrent.ListenableFuture
import com.cappielloantonio.tempo.interfaces.PlaylistCallback
import com.cappielloantonio.tempo.subsonic.models.Child
import java.util.ArrayList

@UnstableApi
class PlaylistPageFragment : Fragment() {

    private lateinit var activity: MainActivity
    private lateinit var playbackViewModel: PlaybackViewModel
    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity() as MainActivity
        playbackViewModel = ViewModelProvider(requireActivity())[PlaybackViewModel::class.java]

        val playlist = arguments?.let {
            BundleCompat.getSerializable(it, Constants.PLAYLIST_OBJECT, Playlist::class.java)
        }
        if (playlist == null) {
            findNavController().navigateUp()
            return ComposeView(requireContext())
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    val viewModel = getViewModel { getPlaylistPageViewModel().apply { onStart(playlist) } }
                    val uiState by viewModel.uiState.collectAsState()
                    val currentSongId by playbackViewModel.currentSongId.collectAsState()
                    val isPlaying by playbackViewModel.isPlaying.collectAsState()
                    val refreshEvent by ExternalAudioReader.getRefreshEvents().observeAsState()
                    var searchQuery by remember { mutableStateOf("") }
                    val downloadedSongIds = remember(
                        uiState.songs,
                        refreshEvent,
                        Preferences.getDownloadDirectoryUri(),
                    ) {
                        uiState.songs
                            .asSequence()
                            .filter(::isSongDownloaded)
                            .map { it.id }
                            .toSet()
                    }

                    LaunchedEffect(uiState.playlist, uiState.isLoading) {
                        if (!uiState.isLoading && uiState.playlist == null) {
                            findNavController().navigateUp()
                        }
                    }

                    PlaylistPageScreen(
                        uiState = uiState,
                        downloadedSongIds = downloadedSongIds,
                        searchQuery = searchQuery,
                        currentSongId = currentSongId,
                        isPlaying = isPlaying,
                        onSearchQueryChange = { searchQuery = it },
                        onSongClick = { index ->
                            MediaManager.startQueue(mediaBrowserListenableFuture, ArrayList(uiState.songs), index)
                            activity.setBottomSheetInPeek(true)
                        },
                        onSongLongClick = { song, index ->
                            val bundle = Bundle().apply {
                                putSerializable(Constants.TRACK_OBJECT, song)
                                putString(Constants.PLAYLIST_ID, uiState.playlist?.id)
                                putInt(Constants.ITEM_POSITION, index)
                            }
                            findNavController().navigate(R.id.songBottomSheetDialog, bundle)
                        },
                        onPlayAllClick = {
                            MediaManager.startQueue(mediaBrowserListenableFuture, ArrayList(uiState.songs), 0)
                            activity.setBottomSheetInPeek(true)
                        },
                        onShuffleAllClick = {
                            val shuffled = uiState.songs.shuffled()
                            MediaManager.startQueue(mediaBrowserListenableFuture, ArrayList(shuffled), 0)
                            activity.setBottomSheetInPeek(true)
                        },
                        onPinClick = {
                            viewModel.togglePinned()
                        },
                        onEditClick = {
                            uiState.playlist?.let { targetPlaylist ->
                                PlaylistEditorDialog(buildPlaylistCallback()).apply {
                                    arguments = Bundle().apply { putSerializable(Constants.PLAYLIST_OBJECT, targetPlaylist) }
                                }.show(parentFragmentManager, null)
                            }
                        },
                        onDeleteClick = {
                            uiState.playlist?.let { targetPlaylist ->
                                PlaylistEditorDialog(buildPlaylistCallback()).apply {
                                    arguments = Bundle().apply { putSerializable(Constants.PLAYLIST_OBJECT, targetPlaylist) }
                                }.show(parentFragmentManager, null)
                            }
                        },
                        onNavigateBack = { findNavController().navigateUp() }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        initializeMediaBrowser()
    }

    override fun onStop() {
        mediaBrowserListenableFuture?.let { MediaBrowser.releaseFuture(it) }
        super.onStop()
    }

    private fun initializeMediaBrowser() {
        mediaBrowserListenableFuture = MediaBrowser.Builder(
            requireContext(),
            SessionToken(requireContext(), ComponentName(requireContext(), MediaService::class.java))
        ).buildAsync()
    }

    private fun buildPlaylistCallback(): PlaylistCallback {
        return object : PlaylistCallback {
            override fun onDismiss() {
                parentFragmentManager.setFragmentResult(
                    Constants.REQUEST_REFRESH_HOME_PLAYLISTS,
                    Bundle.EMPTY
                )
            }
        }
    }

    private fun isSongDownloaded(song: Child): Boolean {
        return if (Preferences.getDownloadDirectoryUri() == null) {
            DownloadUtil.getDownloadTracker(requireContext()).isDownloaded(song.id)
        } else {
            ExternalAudioReader.getUri(song) != null
        }
    }
}
