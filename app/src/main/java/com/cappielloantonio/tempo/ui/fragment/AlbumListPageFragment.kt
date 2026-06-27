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
import com.cappielloantonio.tempo.di.getAlbumListPageViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.ui.album.AlbumListPageScreen
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.AlbumListPageArgs
import java.util.ArrayList

@UnstableApi
class AlbumListPageFragment : Fragment() {

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
                    val viewModel = getViewModel { getAlbumListPageViewModel().apply { onStart(args) } }
                    val uiState by viewModel.uiState.collectAsState()

                    AlbumListPageScreen(
                        uiState = uiState,
                        title = resolveTitle(args),
                        onAlbumClick = { album ->
                            val bundle = Bundle().apply { putSerializable(Constants.ALBUM_OBJECT, album) }
                            findNavController().navigate(R.id.albumPageFragment, bundle)
                        },
                        onAlbumLongClick = { album ->
                            val bundle = Bundle().apply { putSerializable(Constants.ALBUM_OBJECT, album) }
                            findNavController().navigate(R.id.albumBottomSheetDialog, bundle)
                        },
                        onNavigateBack = { findNavController().navigateUp() },
                    )
                }
            }
        }
    }

    private fun resolveTitle(args: AlbumListPageArgs): String {
        return when (args.type) {
            Constants.ALBUM_STARRED -> getString(R.string.album_list_page_starred)
            Constants.ALBUM_RECENTLY_ADDED -> getString(R.string.album_list_page_recently_added)
            Constants.ALBUM_RECENTLY_PLAYED -> getString(R.string.album_list_page_recently_played)
            Constants.ALBUM_MOST_PLAYED -> getString(R.string.album_list_page_most_played)
            Constants.ALBUM_NEW_RELEASES -> getString(R.string.album_list_page_new_releases)
            Constants.ALBUM_DOWNLOADED -> getString(R.string.album_list_page_downloaded)
            Constants.ALBUM_FROM_ARTIST -> args.artist?.name ?: getString(R.string.album_list_page_title)
            else -> args.listTitle ?: getString(R.string.album_list_page_title)
        }
    }

    private fun resolveArgs(bundle: Bundle?): AlbumListPageArgs? {
        bundle ?: return null

        val artist = bundle.getSerializable(Constants.ARTIST_OBJECT) as? ArtistID3
        val customAlbums = (bundle.getSerializable(Constants.ALBUMS_OBJECT) as? ArrayList<*>)?.filterIsInstance<AlbumID3>()

        val type = when {
            bundle.getString(Constants.ALBUM_RECENTLY_PLAYED) != null -> Constants.ALBUM_RECENTLY_PLAYED
            bundle.getString(Constants.ALBUM_MOST_PLAYED) != null -> Constants.ALBUM_MOST_PLAYED
            bundle.getString(Constants.ALBUM_RECENTLY_ADDED) != null -> Constants.ALBUM_RECENTLY_ADDED
            bundle.getString(Constants.ALBUM_STARRED) != null -> Constants.ALBUM_STARRED
            bundle.getString(Constants.ALBUM_NEW_RELEASES) != null -> Constants.ALBUM_NEW_RELEASES
            bundle.getString(Constants.ALBUM_DOWNLOADED) != null -> Constants.ALBUM_DOWNLOADED
            artist != null -> Constants.ALBUM_FROM_ARTIST
            !customAlbums.isNullOrEmpty() -> Constants.ALBUM_LIST_TITLE
            else -> return null
        }

        return AlbumListPageArgs(
            type = type,
            artist = artist,
            albums = customAlbums ?: emptyList(),
            listTitle = bundle.getString(Constants.ALBUM_LIST_TITLE),
        )
    }
}
