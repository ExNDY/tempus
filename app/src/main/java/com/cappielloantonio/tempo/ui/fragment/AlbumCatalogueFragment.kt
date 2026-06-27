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
import com.cappielloantonio.tempo.di.getAlbumCatalogueViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.ui.album.AlbumListPageScreen
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.viewmodel.AlbumListUiState

@UnstableApi
class AlbumCatalogueFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    val viewModel = getViewModel { getAlbumCatalogueViewModel().apply { onStart() } }
                    val uiState by viewModel.uiState.collectAsState()
                    AlbumListPageScreen(
                        uiState = AlbumListUiState(
                            albums = uiState.albums,
                            isLoading = uiState.isLoading,
                            supportsSort = true,
                        ),
                        title = getString(R.string.album_catalogue_title),
                        onAlbumClick = { album ->
                            findNavController().navigate(
                                R.id.albumPageFragment,
                                Bundle().apply { putSerializable(com.cappielloantonio.tempo.util.Constants.ALBUM_OBJECT, album) }
                            )
                        },
                        onAlbumLongClick = { album ->
                            findNavController().navigate(
                                R.id.albumBottomSheetDialog,
                                Bundle().apply { putSerializable(com.cappielloantonio.tempo.util.Constants.ALBUM_OBJECT, album) }
                            )
                        },
                        onRefresh = viewModel::refresh,
                        onLoadMore = viewModel::loadNextPage,
                        onNavigateBack = { findNavController().navigateUp() },
                    )
                }
            }
        }
    }
}
