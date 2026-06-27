package com.cappielloantonio.tempo.ui.fragment

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.fragment.findNavController
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getArtistPageViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.artist.ArtistPageScreen
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.viewmodel.PlaybackViewModel
import com.google.common.util.concurrent.ListenableFuture
import java.util.ArrayList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@UnstableApi
class ArtistPageFragment : Fragment() {

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

        val artist = arguments?.getSerializable(Constants.ARTIST_OBJECT) as? ArtistID3
        if (artist == null) {
            findNavController().navigateUp()
            return ComposeView(requireContext())
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    val artistPageViewModel = getViewModel { getArtistPageViewModel().apply { onStart(artist) } }
                    val uiState by artistPageViewModel.uiState.collectAsState()
                    val currentSongId by playbackViewModel.currentSongId.collectAsState()
                    val isPlaying by playbackViewModel.isPlaying.collectAsState()
                    var isBiographyVisible by remember { mutableStateOf(Preferences.getArtistDisplayBiography()) }

                    ArtistPageScreen(
                        uiState = uiState,
                        currentSongId = currentSongId,
                        isPlaying = isPlaying,
                        isBiographyVisible = isBiographyVisible,
                        onFavoriteClick = {
                            artistPageViewModel.setFavorite()
                        },
                        onToggleBiographyVisibility = {
                            isBiographyVisible = !isBiographyVisible
                            Preferences.setArtistDisplayBiography(isBiographyVisible)
                        },
                        onBiographyMoreClick = uiState.artistInfo?.lastFmUrl?.let { url ->
                            {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        },
                        onShuffleClick = {
                            val shuffled = artistPageViewModel.getShuffledArtistSongs()
                            if (shuffled.isNotEmpty()) {
                                MediaManager.startQueue(mediaBrowserListenableFuture, ArrayList(shuffled), 0)
                                activity.setBottomSheetInPeek(true)
                            }
                        },
                        onRadioClick = {
                            lifecycleScope.launch {
                                val mixSongs = artistPageViewModel.getArtistInstantMix().first()
                                if (mixSongs.isNotEmpty()) {
                                    MediaManager.startQueue(mediaBrowserListenableFuture, ArrayList(mixSongs), 0)
                                    activity.setBottomSheetInPeek(true)
                                }
                            }
                        },
                        onSeeAllTopSongsClick = {
                            val currentArtist = uiState.artist ?: return@ArtistPageScreen
                            val bundle = Bundle().apply {
                                putSerializable(Constants.ARTIST_OBJECT, currentArtist)
                                putString(Constants.MEDIA_BY_ARTIST, Constants.MEDIA_BY_ARTIST)
                            }
                            findNavController().navigate(R.id.songListPageFragment, bundle)
                        },
                        onAlbumClick = { album ->
                            val bundle = Bundle().apply { putSerializable(Constants.ALBUM_OBJECT, album) }
                            findNavController().navigate(R.id.albumPageFragment, bundle)
                        },
                        onSongClick = { index ->
                            MediaManager.startQueue(mediaBrowserListenableFuture, ArrayList(uiState.topSongs), index)
                            activity.setBottomSheetInPeek(true)
                        },
                        onSongLongClick = { song ->
                            val bundle = Bundle().apply { putSerializable(Constants.TRACK_OBJECT, song) }
                            findNavController().navigate(R.id.songBottomSheetDialog, bundle)
                        },
                        onSimilarArtistClick = { similarArtist ->
                            val bundle = Bundle().apply { putSerializable(Constants.ARTIST_OBJECT, similarArtist) }
                            findNavController().navigate(R.id.artistPageFragment, bundle)
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
}
