package com.cappielloantonio.tempo.ui.fragment

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.fragment.findNavController
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getSongListPageViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Genre
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.song.SongListPageScreen
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.PlaybackViewModel
import com.cappielloantonio.tempo.viewmodel.SongListPageArgs
import com.google.common.util.concurrent.ListenableFuture
import java.util.ArrayList

@UnstableApi
class SongListPageFragment : Fragment() {

    private lateinit var activity: MainActivity
    private lateinit var playbackViewModel: PlaybackViewModel
    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        activity = requireActivity() as MainActivity
        playbackViewModel = ViewModelProvider(requireActivity())[PlaybackViewModel::class.java]

        val args = resolveArgs(arguments)
        if (args == null) {
            findNavController().navigateUp()
            return ComposeView(requireContext())
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    val viewModel = getViewModel { getSongListPageViewModel().apply { onStart(args) } }
                    val uiState by viewModel.uiState.collectAsState()
                    val currentSongId by playbackViewModel.currentSongId.collectAsState()
                    val isPlaying by playbackViewModel.isPlaying.collectAsState()

                    SongListPageScreen(
                        uiState = uiState,
                        currentSongId = currentSongId,
                        isPlaying = isPlaying,
                        onSongClick = { songs, index ->
                            MediaManager.startQueue(mediaBrowserListenableFuture, ArrayList(songs), index)
                            activity.setBottomSheetInPeek(true)
                        },
                        onSongLongClick = { song, index ->
                            val bundle = Bundle().apply {
                                putSerializable(Constants.TRACK_OBJECT, song)
                                putInt(Constants.ITEM_POSITION, index)
                            }
                            findNavController().navigate(R.id.songBottomSheetDialog, bundle)
                        },
                        onPlayAllClick = { songs ->
                            if (songs.isNotEmpty()) {
                                MediaManager.startQueue(mediaBrowserListenableFuture, ArrayList(songs), 0)
                                activity.setBottomSheetInPeek(true)
                            }
                        },
                        onShuffleAllClick = { songs ->
                            if (songs.isNotEmpty()) {
                                val shuffled = songs.shuffled()
                                MediaManager.startQueue(mediaBrowserListenableFuture, ArrayList(shuffled), 0)
                                activity.setBottomSheetInPeek(true)
                            }
                        },
                        onNavigateBack = { findNavController().navigateUp() },
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
            SessionToken(requireContext(), ComponentName(requireContext(), MediaService::class.java)),
        ).buildAsync()
    }

    private fun resolveArgs(bundle: Bundle?): SongListPageArgs? {
        bundle ?: return null

        val type = listOf(
            Constants.MEDIA_RECENTLY_PLAYED,
            Constants.MEDIA_MOST_PLAYED,
            Constants.MEDIA_RECENTLY_ADDED,
            Constants.MEDIA_DOWNLOADED,
            Constants.MEDIA_STARRED,
            Constants.MEDIA_BY_ARTIST,
            Constants.MEDIA_BY_GENRE,
            Constants.MEDIA_BY_GENRES,
            Constants.MEDIA_BY_YEAR,
            Constants.MEDIA_FROM_ALBUM,
        ).firstOrNull { bundle.getString(it) != null || (it == Constants.MEDIA_FROM_ALBUM && bundle.getSerializable(Constants.ALBUM_OBJECT) != null) }
            ?: return null

        return SongListPageArgs(
            type = type,
            artist = bundle.getSerializable(Constants.ARTIST_OBJECT) as? ArtistID3,
            genre = bundle.getSerializable(Constants.GENRE_OBJECT) as? Genre,
            album = bundle.getSerializable(Constants.ALBUM_OBJECT) as? AlbumID3,
            filters = bundle.getStringArrayList("filters_list") ?: emptyList(),
            filterNames = bundle.getStringArrayList("filter_name_list") ?: emptyList(),
            year = bundle.getInt("year_object"),
        )
    }
}
