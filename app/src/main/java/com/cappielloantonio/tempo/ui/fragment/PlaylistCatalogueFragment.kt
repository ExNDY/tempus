package com.cappielloantonio.tempo.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getPlaylistCatalogueViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.interfaces.PodcastCallback
import com.cappielloantonio.tempo.ui.components.CatalogueScreen
import com.cappielloantonio.tempo.ui.dialog.PlaylistEditorDialog
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.PlaylistCatalogueArgs
import com.cappielloantonio.tempo.subsonic.models.Child

@UnstableApi
class PlaylistCatalogueFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val type = when {
            arguments?.getString(Constants.PLAYLIST_DOWNLOADED) != null -> Constants.PLAYLIST_DOWNLOADED
            else -> Constants.PLAYLIST_ALL
        }
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    val viewModel = getViewModel { getPlaylistCatalogueViewModel().apply { onStart(PlaylistCatalogueArgs(type)) } }
                    val uiState by viewModel.uiState.collectAsState()
                    CatalogueScreen(
                        items = uiState.playlists,
                        title = getString(R.string.playlist_catalogue_title),
                        isLoading = uiState.isLoading,
                        onItemClick = { playlist ->
                            findNavController().navigate(
                                R.id.playlistPageFragment,
                                Bundle().apply { putSerializable(Constants.PLAYLIST_OBJECT, playlist) }
                            )
                        },
                        onNavigateBack = { findNavController().navigateUp() },
                        onSearchClick = {
                            PlaylistEditorDialog(null).apply {
                                arguments = Bundle().apply {
                                    putSerializable(Constants.TRACKS_OBJECT, ArrayList<Child>())
                                }
                            }.show(parentFragmentManager, null)
                        },
                    )
                }
            }
        }
    }
}
