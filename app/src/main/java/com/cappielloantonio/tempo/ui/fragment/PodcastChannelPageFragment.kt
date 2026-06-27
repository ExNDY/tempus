package com.cappielloantonio.tempo.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getPodcastChannelPageViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.subsonic.models.PodcastChannel
import com.cappielloantonio.tempo.subsonic.models.PodcastStatus
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.podcast.PodcastChannelPageScreen
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.PodcastChannelPageArgs

@UnstableApi
class PodcastChannelPageFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val bundle = arguments ?: return ComposeView(requireContext())
        val channel = BundleCompat.getSerializable(
            bundle,
            Constants.PODCAST_CHANNEL_OBJECT,
            PodcastChannel::class.java,
        ) ?: return ComposeView(requireContext())
        val activity = requireActivity() as MainActivity

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    val viewModel = getViewModel {
                        getPodcastChannelPageViewModel().apply { onStart(PodcastChannelPageArgs(channel)) }
                    }
                    val uiState by viewModel.uiState.collectAsState()

                    DisposableEffect(viewModel) {
                        parentFragmentManager.setFragmentResultListener(
                            Constants.REQUEST_REFRESH_PODCASTS,
                            viewLifecycleOwner,
                        ) { _, _ ->
                            viewModel.refresh()
                        }

                        onDispose {
                            parentFragmentManager.clearFragmentResultListener(
                                Constants.REQUEST_REFRESH_PODCASTS
                            )
                        }
                    }

                    PodcastChannelPageScreen(
                        uiState = uiState,
                        onNavigateBack = { findNavController().navigateUp() },
                        onRefresh = viewModel::refresh,
                        onEpisodeClick = { episode ->
                            if (episode.status == PodcastStatus.COMPLETED) {
                                MediaManager.startPodcast(activity.mediaBrowserListenableFuture, episode)
                                activity.setBottomSheetInPeek(true)
                            }
                        },
                        onEpisodeLongClick = { episode ->
                            findNavController().navigate(
                                R.id.podcastEpisodeBottomSheetDialog,
                                Bundle().apply { putSerializable(Constants.PODCAST_OBJECT, episode) },
                            )
                        },
                        onEpisodeDownloadClick = viewModel::requestPodcastEpisodeDownload,
                    )
                }
            }
        }
    }
}
