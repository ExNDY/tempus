package com.cappielloantonio.tempo.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.ui.search.SearchScreen
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.SearchViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

@UnstableApi
class SearchFragment : Fragment() {

    private val viewModel: SearchViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                TempusTheme {
                    val uiState by viewModel.uiState.collectAsState()

                    SearchScreen(
                        uiState = uiState,
                        onQueryChange = { query ->
                            if (query.length >= 3) viewModel.search(query)
                        },
                        onSearch = { query ->
                            viewModel.search(query)
                        },
                        onRecentSearchDelete = { search ->
                            viewModel.deleteRecentSearch(search)
                        },
                        onArtistClick = { artist ->
                            val bundle = Bundle().apply { putSerializable(Constants.ARTIST_OBJECT, artist) }
                            findNavController().navigate(R.id.artistPageFragment, bundle)
                        },
                        onAlbumClick = { album ->
                            val bundle = Bundle().apply { putSerializable(Constants.ALBUM_OBJECT, album) }
                            findNavController().navigate(R.id.albumPageFragment, bundle)
                        },
                        onSongClick = { song ->
                            val bundle = Bundle().apply { putSerializable(Constants.TRACK_OBJECT, song) }
                            findNavController().navigate(R.id.songBottomSheetDialog, bundle)
                        },
                        onNavigateBack = { findNavController().navigateUp() }
                    )
                }
            }
        }
    }
}
