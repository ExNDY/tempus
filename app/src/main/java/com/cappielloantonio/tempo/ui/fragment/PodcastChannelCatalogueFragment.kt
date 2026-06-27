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
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getPodcastChannelCatalogueViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.interfaces.PodcastCallback
import com.cappielloantonio.tempo.ui.dialog.PodcastChannelEditorDialog
import com.cappielloantonio.tempo.ui.podcast.PodcastChannelCatalogueScreen
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Constants

@UnstableApi
class PodcastChannelCatalogueFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    val viewModel = getViewModel {
                        getPodcastChannelCatalogueViewModel().apply { onStart() }
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

                    val refreshPodcastCallback = object : PodcastCallback {
                        override fun onDismiss() {
                            parentFragmentManager.setFragmentResult(
                                Constants.REQUEST_REFRESH_PODCASTS,
                                Bundle.EMPTY,
                            )
                        }
                    }

                    PodcastChannelCatalogueScreen(
                        uiState = uiState,
                        title = getString(R.string.podcast_channel_catalogue_title),
                        onChannelClick = { channel ->
                            findNavController().navigate(
                                R.id.podcastChannelPageFragment,
                                Bundle().apply { putSerializable(Constants.PODCAST_CHANNEL_OBJECT, channel) },
                            )
                        },
                        onChannelLongClick = { channel ->
                            findNavController().navigate(
                                R.id.podcastChannelBottomSheetDialog,
                                Bundle().apply { putSerializable(Constants.PODCAST_CHANNEL_OBJECT, channel) },
                            )
                        },
                        onCreateChannel = {
                            PodcastChannelEditorDialog(refreshPodcastCallback).show(parentFragmentManager, null)
                        },
                        onRefresh = viewModel::refresh,
                        onNavigateBack = { findNavController().navigateUp() },
                    )
                }
            }
        }
    }
}
