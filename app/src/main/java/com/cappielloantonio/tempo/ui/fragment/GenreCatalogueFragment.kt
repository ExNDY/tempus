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
import com.cappielloantonio.tempo.di.getGenreCatalogueViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.ui.components.CatalogueScreen
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Constants

@UnstableApi
class GenreCatalogueFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    val viewModel = getViewModel { getGenreCatalogueViewModel().apply { onStart() } }
                    val uiState by viewModel.uiState.collectAsState()
                    CatalogueScreen(
                        items = uiState.genres,
                        title = getString(R.string.genre_catalogue_title),
                        isLoading = uiState.isLoading,
                        onItemClick = { genre ->
                            findNavController().navigate(
                                R.id.songListPageFragment,
                                Bundle().apply {
                                    putSerializable(Constants.GENRE_OBJECT, genre)
                                    putString(Constants.MEDIA_BY_GENRE, Constants.MEDIA_BY_GENRE)
                                }
                            )
                        },
                        onNavigateBack = { findNavController().navigateUp() },
                        onSearchClick = { findNavController().navigate(R.id.searchFragment) },
                        onFilterClick = { findNavController().navigate(R.id.action_genreCatalogueFragment_to_filterFragment) },
                    )
                }
            }
        }
    }
}
