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
import com.cappielloantonio.tempo.di.getPlaylistCatalogueViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.interfaces.PlaylistCallback
import com.cappielloantonio.tempo.ui.dialog.PlaylistEditorDialog
import com.cappielloantonio.tempo.ui.playlist.PlaylistCatalogueScreen
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.PlaylistCatalogueArgs

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

                    DisposableEffect(viewModel) {
                        parentFragmentManager.setFragmentResultListener(
                            Constants.REQUEST_REFRESH_HOME_PLAYLISTS,
                            viewLifecycleOwner,
                        ) { _, _ ->
                            viewModel.refresh()
                        }

                        onDispose {
                            parentFragmentManager.clearFragmentResultListener(
                                Constants.REQUEST_REFRESH_HOME_PLAYLISTS
                            )
                        }
                    }

                    val refreshPlaylistCallback = object : PlaylistCallback {
                        override fun onDismiss() {
                            parentFragmentManager.setFragmentResult(
                                Constants.REQUEST_REFRESH_HOME_PLAYLISTS,
                                Bundle.EMPTY,
                            )
                        }
                    }

                    PlaylistCatalogueScreen(
                        uiState = uiState,
                        title = getString(R.string.playlist_catalogue_title),
                        onPlaylistClick = { playlist ->
                            findNavController().navigate(
                                R.id.playlistPageFragment,
                                Bundle().apply { putSerializable(Constants.PLAYLIST_OBJECT, playlist) }
                            )
                        },
                        onPlaylistLongClick = { playlist ->
                            findNavController().navigate(
                                R.id.playlistBottomSheetDialog,
                                Bundle().apply { putSerializable(Constants.PLAYLIST_OBJECT, playlist) },
                            )
                        },
                        onCreatePlaylist = {
                            PlaylistEditorDialog(refreshPlaylistCallback).apply {
                                arguments = Bundle().apply {
                                    putSerializable(Constants.TRACKS_OBJECT, ArrayList<Child>())
                                }
                            }.show(parentFragmentManager, null)
                        },
                        onRefresh = viewModel::refresh,
                        onNavigateBack = { findNavController().navigateUp() },
                    )
                }
            }
        }
    }
}
