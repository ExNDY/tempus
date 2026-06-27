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
import com.cappielloantonio.tempo.di.getLibraryViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.interfaces.PlaylistCallback
import com.cappielloantonio.tempo.ui.dialog.PlaylistEditorDialog
import com.cappielloantonio.tempo.ui.home.LibraryScreen
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Constants

@UnstableApi
class LibraryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    val viewModel = getViewModel { getLibraryViewModel().apply { onStart() } }
                    val uiState by viewModel.uiState.collectAsState()

                    LibraryScreen(
                        uiState = uiState,
                        onNavigateBack = { findNavController().navigateUp() },
                        onRefreshAll = { viewModel.refreshAll() },
                        onMusicFolderClick = { folder ->
                            findNavController().navigate(
                                R.id.indexFragment,
                                Bundle().apply { putSerializable(Constants.MUSIC_FOLDER_OBJECT, folder) },
                            )
                        },
                        onAlbumClick = { album ->
                            findNavController().navigate(
                                R.id.albumPageFragment,
                                Bundle().apply { putSerializable(Constants.ALBUM_OBJECT, album) },
                            )
                        },
                        onAlbumLongClick = { album ->
                            findNavController().navigate(
                                R.id.albumBottomSheetDialog,
                                Bundle().apply { putSerializable(Constants.ALBUM_OBJECT, album) },
                            )
                        },
                        onArtistClick = { artist ->
                            findNavController().navigate(
                                R.id.artistPageFragment,
                                Bundle().apply { putSerializable(Constants.ARTIST_OBJECT, artist) },
                            )
                        },
                        onArtistLongClick = { artist ->
                            findNavController().navigate(
                                R.id.artistBottomSheetDialog,
                                Bundle().apply { putSerializable(Constants.ARTIST_OBJECT, artist) },
                            )
                        },
                        onGenreClick = { genre ->
                            findNavController().navigate(
                                R.id.songListPageFragment,
                                Bundle().apply {
                                    putSerializable(Constants.GENRE_OBJECT, genre)
                                    putString(Constants.MEDIA_BY_GENRE, Constants.MEDIA_BY_GENRE)
                                },
                            )
                        },
                        onPlaylistClick = { playlist ->
                            findNavController().navigate(
                                R.id.playlistPageFragment,
                                Bundle().apply { putSerializable(Constants.PLAYLIST_OBJECT, playlist) },
                            )
                        },
                        onPlaylistLongClick = { playlist ->
                            PlaylistEditorDialog(object : PlaylistCallback {
                                override fun onDismiss() {
                                    viewModel.refreshPlaylistSample()
                                }
                            }).apply {
                                arguments = Bundle().apply {
                                    putSerializable(Constants.PLAYLIST_OBJECT, playlist)
                                }
                            }.show(parentFragmentManager, null)
                        },
                        onSeeAllAlbumsClick = {
                            findNavController().navigate(R.id.action_libraryFragment_to_albumCatalogueFragment)
                        },
                        onSeeAllArtistsClick = {
                            findNavController().navigate(R.id.action_libraryFragment_to_artistCatalogueFragment)
                        },
                        onSeeAllGenresClick = {
                            findNavController().navigate(R.id.action_libraryFragment_to_genreCatalogueFragment)
                        },
                        onSeeAllPlaylistsClick = {
                            findNavController().navigate(
                                R.id.action_libraryFragment_to_playlistCatalogueFragment,
                                Bundle().apply { putString(Constants.PLAYLIST_ALL, Constants.PLAYLIST_ALL) },
                            )
                        },
                        onRefreshAlbums = { viewModel.refreshAlbumSample() },
                        onRefreshArtists = { viewModel.refreshArtistSample() },
                        onRefreshGenres = { viewModel.refreshGenreSample() },
                        onRefreshPlaylists = { viewModel.refreshPlaylistSample() },
                    )
                }
            }
        }
    }
}
