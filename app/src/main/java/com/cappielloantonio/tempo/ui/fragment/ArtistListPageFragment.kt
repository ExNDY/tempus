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
import com.cappielloantonio.tempo.di.getArtistListPageViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.ui.artist.ArtistListPageScreen
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.ArtistListPageArgs

@UnstableApi
class ArtistListPageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val args = resolveArgs(arguments)
        if (args == null) {
            findNavController().navigateUp()
            return ComposeView(requireContext())
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    val viewModel = getViewModel { getArtistListPageViewModel().apply { onStart(args) } }
                    val uiState by viewModel.uiState.collectAsState()

                    ArtistListPageScreen(
                        uiState = uiState,
                        title = resolveTitle(args.type),
                        onArtistClick = { artist ->
                            val bundle = Bundle().apply { putSerializable(Constants.ARTIST_OBJECT, artist) }
                            findNavController().navigate(R.id.artistPageFragment, bundle)
                        },
                        onArtistLongClick = { artist ->
                            val bundle = Bundle().apply { putSerializable(Constants.ARTIST_OBJECT, artist) }
                            findNavController().navigate(R.id.artistBottomSheetDialog, bundle)
                        },
                        onNavigateBack = { findNavController().navigateUp() },
                    )
                }
            }
        }
    }

    private fun resolveArgs(bundle: Bundle?): ArtistListPageArgs? {
        val type = when {
            bundle?.getString(Constants.ARTIST_STARRED) != null -> Constants.ARTIST_STARRED
            bundle?.getString(Constants.ARTIST_DOWNLOADED) != null -> Constants.ARTIST_DOWNLOADED
            bundle != null -> Constants.ARTIST_ORDER_BY_NAME
            else -> return null
        }

        return ArtistListPageArgs(type = type)
    }

    private fun resolveTitle(type: String): String {
        return when (type) {
            Constants.ARTIST_STARRED -> getString(R.string.artist_list_page_starred)
            Constants.ARTIST_DOWNLOADED -> getString(R.string.artist_list_page_downloaded)
            else -> getString(R.string.artist_list_page_title)
        }
    }
}
