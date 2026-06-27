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
import com.cappielloantonio.tempo.di.getArtistCatalogueViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.ui.artist.ArtistListPageScreen
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.viewmodel.ArtistListUiState

@UnstableApi
class ArtistCatalogueFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    val viewModel = getViewModel { getArtistCatalogueViewModel().apply { onStart() } }
                    val uiState by viewModel.uiState.collectAsState()
                    ArtistListPageScreen(
                        uiState = ArtistListUiState(
                            artists = uiState.artists,
                            isLoading = uiState.isLoading,
                            supportsSort = true,
                        ),
                        title = getString(R.string.artist_catalogue_title),
                        onArtistClick = { artist ->
                            findNavController().navigate(
                                R.id.artistPageFragment,
                                Bundle().apply { putSerializable(com.cappielloantonio.tempo.util.Constants.ARTIST_OBJECT, artist) }
                            )
                        },
                        onArtistLongClick = { artist ->
                            findNavController().navigate(
                                R.id.artistBottomSheetDialog,
                                Bundle().apply { putSerializable(com.cappielloantonio.tempo.util.Constants.ARTIST_OBJECT, artist) }
                            )
                        },
                        onNavigateBack = { findNavController().navigateUp() },
                    )
                }
            }
        }
    }
}
