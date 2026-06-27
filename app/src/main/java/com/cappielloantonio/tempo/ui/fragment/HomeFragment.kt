package com.cappielloantonio.tempo.ui.fragment

import android.content.Intent
import android.net.Uri
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
import androidx.navigation.fragment.findNavController
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getHomeViewModel
import com.cappielloantonio.tempo.di.getPodcastViewModel
import com.cappielloantonio.tempo.di.getRadioViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.dialog.HomeRearrangementDialog
import com.cappielloantonio.tempo.ui.dialog.PodcastChannelEditorDialog
import com.cappielloantonio.tempo.ui.dialog.RadioEditorDialog
import com.cappielloantonio.tempo.ui.home.HomeScreen
import com.cappielloantonio.tempo.ui.home.HomeTabMusicScreen
import com.cappielloantonio.tempo.ui.home.HomeTabPodcastScreen
import com.cappielloantonio.tempo.ui.home.HomeTabRadioScreen
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.viewmodel.PlaybackViewModel
import com.cappielloantonio.tempo.interfaces.PodcastCallback
import com.cappielloantonio.tempo.interfaces.RadioCallback
import java.util.ArrayList

@UnstableApi
class HomeFragment : Fragment() {

    private lateinit var activity: MainActivity
    private lateinit var playbackViewModel: PlaybackViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity() as MainActivity
        playbackViewModel = ViewModelProvider(requireActivity())[PlaybackViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    val homeViewModel = getViewModel { getHomeViewModel().apply { onStart() } }
                    val podcastViewModel = getViewModel { getPodcastViewModel().apply { onStart() } }
                    val radioViewModel = getViewModel { getRadioViewModel().apply { onStart() } }
                    val musicUiState by homeViewModel.musicUiState.collectAsState()
                    val podcastUiState by podcastViewModel.uiState.collectAsState()
                    val radioUiState by radioViewModel.uiState.collectAsState()
                    val currentSongId by playbackViewModel.currentSongId.collectAsState()
                    val isPlaying by playbackViewModel.isPlaying.collectAsState()

                    HomeScreen(
                        onSettingsClick = {
                            findNavController().navigate(R.id.action_homeFragment_to_settingsFragment)
                        },
                        onEditHomeClick = {
                            HomeRearrangementDialog().show(activity.supportFragmentManager, null)
                        },
                        musicTab = {
                            HomeTabMusicScreen(
                                uiState = musicUiState,
                                currentSongId = currentSongId,
                                isPlaying = isPlaying,
                                onSectorRefresh = { id -> homeViewModel.refreshSector(id) },
                                onMediaClick = { song, list ->
                                    val position = list.indexOf(song)
                                    MediaManager.startQueue(activity.mediaBrowserListenableFuture, ArrayList(list), position)
                                    activity.setBottomSheetInPeek(true)
                                },
                                onAlbumClick = { album ->
                                    val bundle = Bundle().apply { putSerializable(Constants.ALBUM_OBJECT, album) }
                                    findNavController().navigate(R.id.albumPageFragment, bundle)
                                },
                                onArtistClick = { artist ->
                                    val bundle = Bundle().apply { putSerializable(Constants.ARTIST_OBJECT, artist) }
                                    findNavController().navigate(R.id.artistPageFragment, bundle)
                                },
                                onPlaylistClick = { playlist ->
                                    val bundle = Bundle().apply { 
                                        putSerializable(Constants.PLAYLIST_OBJECT, playlist)
                                    }
                                    findNavController().navigate(R.id.playlistPageFragment, bundle)
                                },
                                onShareClick = { share ->
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(share.url ?: ""))
                                    startActivity(intent)
                                },
                                onYearClick = { year ->
                                    val bundle = Bundle().apply {
                                        putInt("year_object", year)
                                        putString(Constants.MEDIA_BY_YEAR, Constants.MEDIA_BY_YEAR)
                                    }
                                    findNavController().navigate(R.id.songListPageFragment, bundle)
                                },
                                onSeeAllClick = { id -> navigateToSeeAll(id) }
                            )
                        },
                        podcastTab = if (Preferences.isPodcastSectionVisible()) {
                            {
                                HomeTabPodcastScreen(
                                    uiState = podcastUiState,
                                    onHideSectionClick = { Preferences.setPodcastSectionHidden() },
                                    onAddChannelClick = {
                                        PodcastChannelEditorDialog(object : PodcastCallback {
                                            override fun onDismiss() { podcastViewModel.refresh() }
                                        }).show(activity.supportFragmentManager, null)
                                    },
                                    onRefreshClick = { podcastViewModel.refresh() },
                                    onSeeAllChannelsClick = {
                                        findNavController().navigate(R.id.action_homeFragment_to_podcastChannelCatalogueFragment)
                                    },
                                    onChannelClick = { channel ->
                                        val bundle = Bundle().apply { putSerializable(Constants.PODCAST_CHANNEL_OBJECT, channel) }
                                        findNavController().navigate(R.id.podcastChannelPageFragment, bundle)
                                    },
                                    onEpisodeClick = { episode ->
                                        MediaManager.startPodcast(activity.mediaBrowserListenableFuture, episode)
                                        activity.setBottomSheetInPeek(true)
                                    },
                                    onEpisodeLongClick = { episode ->
                                        val bundle = Bundle().apply { putSerializable(Constants.PODCAST_OBJECT, episode) }
                                        findNavController().navigate(R.id.podcastEpisodeBottomSheetDialog, bundle)
                                    }
                                )
                            }
                        } else null,
                        radioTab = if (Preferences.isRadioSectionVisible()) {
                            {
                                HomeTabRadioScreen(
                                    uiState = radioUiState,
                                    onHideSectionClick = { Preferences.setRadioSectionHidden() },
                                    onAddStationClick = {
                                        RadioEditorDialog(object : RadioCallback {
                                            override fun onDismiss() { radioViewModel.refresh() }
                                        }).show(activity.supportFragmentManager, null)
                                    },
                                    onRefreshClick = { radioViewModel.refresh() },
                                    onStationClick = { station ->
                                        MediaManager.startRadio(activity.mediaBrowserListenableFuture, station)
                                        activity.setBottomSheetInPeek(true)
                                    },
                                    onStationLongClick = { station ->
                                        val bundle = Bundle().apply { putSerializable(Constants.INTERNET_RADIO_STATION_OBJECT, station) }
                                        RadioEditorDialog(object : RadioCallback {
                                            override fun onDismiss() { radioViewModel.refresh() }
                                        }).apply { arguments = bundle }.show(activity.supportFragmentManager, null)
                                    }
                                )
                            }
                        } else null
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        activity.toggleBottomNavigationBarVisibilityOnOrientationChange()
        activity.setBottomSheetVisibility(true)
    }

    private fun navigateToSeeAll(sectorId: String) {
        val bundle = Bundle()
        when (sectorId) {
            Constants.HOME_SECTOR_STARRED_TRACKS -> {
                bundle.putString(Constants.MEDIA_STARRED, Constants.MEDIA_STARRED)
                findNavController().navigate(R.id.action_homeFragment_to_songListPageFragment, bundle)
            }
            Constants.HOME_SECTOR_STARRED_ALBUMS -> {
                bundle.putString(Constants.ALBUM_STARRED, Constants.ALBUM_STARRED)
                findNavController().navigate(R.id.action_homeFragment_to_albumListPageFragment, bundle)
            }
            Constants.HOME_SECTOR_STARRED_ARTISTS -> {
                bundle.putString(Constants.ARTIST_STARRED, Constants.ARTIST_STARRED)
                findNavController().navigate(R.id.action_homeFragment_to_artistListPageFragment, bundle)
            }
            Constants.HOME_SECTOR_RECENTLY_ADDED -> {
                bundle.putString(Constants.ALBUM_RECENTLY_ADDED, Constants.ALBUM_RECENTLY_ADDED)
                findNavController().navigate(R.id.action_homeFragment_to_albumListPageFragment, bundle)
            }
            Constants.HOME_SECTOR_PINNED_PLAYLISTS -> {
                bundle.putString(Constants.PLAYLIST_ALL, Constants.PLAYLIST_ALL)
                findNavController().navigate(R.id.action_homeFragment_to_playlistCatalogueFragment, bundle)
            }
            Constants.HOME_SECTOR_LAST_PLAYED -> {
                bundle.putString(Constants.ALBUM_RECENTLY_PLAYED, Constants.ALBUM_RECENTLY_PLAYED)
                findNavController().navigate(R.id.action_homeFragment_to_albumListPageFragment, bundle)
            }
            Constants.HOME_SECTOR_MOST_PLAYED -> {
                bundle.putString(Constants.ALBUM_MOST_PLAYED, Constants.ALBUM_MOST_PLAYED)
                findNavController().navigate(R.id.action_homeFragment_to_albumListPageFragment, bundle)
            }
        }
    }
}
