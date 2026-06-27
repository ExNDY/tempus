package com.cappielloantonio.tempo.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.ui.dialog.PodcastChannelEditorDialog
import com.cappielloantonio.tempo.ui.home.PodcastChannelItem
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.PodcastViewModel

@UnstableApi
class PodcastChannelCatalogueFragment : Fragment() {
    private val viewModel: PodcastViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    com.cappielloantonio.tempo.ui.components.CatalogueScreen(
                        items = uiState.channels,
                        title = getString(R.string.podcast_channel_catalogue_title),
                        isLoading = uiState.isLoading,
                        onItemClick = { channel ->
                            findNavController().navigate(
                                R.id.podcastChannelPageFragment,
                                Bundle().apply { putSerializable(Constants.PODCAST_CHANNEL_OBJECT, channel) }
                            )
                        },
                        onNavigateBack = { findNavController().navigateUp() },
                        onSearchClick = { PodcastChannelEditorDialog(null).show(parentFragmentManager, null) }
                    )
                }
            }
        }
    }
}
