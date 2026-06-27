package com.cappielloantonio.tempo.ui.fragment

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.fragment.findNavController
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.di.getAlbumPageViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.album.AlbumPageScreen
import com.cappielloantonio.tempo.ui.dialog.PlaylistChooserDialog
import com.cappielloantonio.tempo.ui.dialog.RatingDialog
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.*
import com.cappielloantonio.tempo.viewmodel.PlaybackViewModel
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.ArrayList

@UnstableApi
class AlbumPageFragment : Fragment() {

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

        val album = arguments?.let { BundleCompat.getSerializable(it, Constants.ALBUM_OBJECT, AlbumID3::class.java) }
        if (album == null) {
            findNavController().navigateUp()
            return ComposeView(requireContext())
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    val albumPageViewModel = getViewModel { getAlbumPageViewModel().apply { onStart(album) } }
                    val uiState by albumPageViewModel.uiState.collectAsState()
                    val currentSongId by playbackViewModel.currentSongId.collectAsState()
                    val isPlaying by playbackViewModel.isPlaying.collectAsState()
                    val refreshEvent by ExternalAudioReader.getRefreshEvents().observeAsState()
                    var pendingDownloadedSongIds by remember(album.id) { mutableStateOf(emptySet<String>()) }
                    val downloadedSongIds = remember(
                        uiState.songs,
                        refreshEvent,
                        pendingDownloadedSongIds,
                        Preferences.getDownloadDirectoryUri(),
                    ) {
                        uiState.songs
                            .asSequence()
                            .filter(::isSongDownloaded)
                            .map { it.id }
                            .toSet() + pendingDownloadedSongIds
                    }

                    AlbumPageScreen(
                        uiState = uiState,
                        downloadedSongIds = downloadedSongIds,
                        currentSongId = currentSongId,
                        isPlaying = isPlaying,
                        onFavoriteClick = {
                            albumPageViewModel.setFavorite()
                        },
                        onPlayClick = {
                            MediaManager.startQueue(mediaBrowserListenableFuture, uiState.songs, 0)
                            activity.setBottomSheetInPeek(true)
                        },
                        onShuffleClick = {
                            val shuffled = uiState.songs.shuffled()
                            MediaManager.startQueue(mediaBrowserListenableFuture, shuffled, 0)
                            activity.setBottomSheetInPeek(true)
                        },
                        onDownloadClick = {
                            pendingDownloadedSongIds = pendingDownloadedSongIds + uiState.songs.map { it.id }
                            downloadAlbum(uiState.songs)
                        },
                        onRateClick = {
                            val bundle = Bundle().apply {
                                putSerializable(Constants.ALBUM_OBJECT, uiState.album)
                            }
                            val dialog = RatingDialog()
                            dialog.arguments = bundle
                            dialog.show(parentFragmentManager, null)
                        },
                        onAddToPlaylistClick = {
                            val bundle = Bundle().apply {
                                putSerializable(Constants.TRACKS_OBJECT, ArrayList(uiState.songs))
                            }
                            val dialog = PlaylistChooserDialog()
                            dialog.arguments = bundle
                            dialog.show(parentFragmentManager, null)
                        },
                        onArtistClick = {
                            navigateToArtist(albumPageViewModel)
                        },
                        onYearClick = { year ->
                            val bundle = Bundle().apply {
                                putInt("year_object", year)
                                putString(Constants.MEDIA_BY_YEAR, Constants.MEDIA_BY_YEAR)
                            }
                            findNavController().navigate(R.id.songListPageFragment, bundle)
                        },
                        onSongClick = { index ->
                            MediaManager.startQueue(mediaBrowserListenableFuture, uiState.songs, index)
                            activity.setBottomSheetInPeek(true)
                        },
                        onSongLongClick = { song ->
                            val bundle = Bundle().apply {
                                putSerializable(Constants.TRACK_OBJECT, song)
                            }
                            findNavController().navigate(R.id.songBottomSheetDialog, bundle)
                        },
                        onNavigateBack = {
                            findNavController().navigateUp()
                        }
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

    private fun downloadAlbum(songs: List<Child>) {
        if (Preferences.getDownloadDirectoryUri() == null) {
            DownloadUtil.getDownloadTracker(requireContext()).download(
                MappingUtil.mapDownloads(songs),
                songs.map { Download(it) }
            )
        } else {
            songs.forEach { child ->
                ExternalAudioWriter.downloadToUserDirectory(requireContext(), child)
            }
        }
    }

    private fun navigateToArtist(albumPageViewModel: com.cappielloantonio.tempo.viewmodel.AlbumPageViewModel) {
        lifecycleScope.launch {
            val artist = albumPageViewModel.getArtist().first()
            if (artist != null) {
                val bundle = Bundle().apply {
                    putSerializable(Constants.ARTIST_OBJECT, artist)
                }
                findNavController().navigate(R.id.artistPageFragment, bundle)
            } else {
                Toast.makeText(requireContext(), getString(R.string.album_error_retrieving_artist), Toast.LENGTH_SHORT).show()
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
