package com.cappielloantonio.tempo.viewmodel

import com.cappielloantonio.tempo.playback.PlaybackStateStore
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.FavoriteRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Test
import org.mockito.kotlin.mock

class AlbumPageViewModelTest {

    @Test
    fun setFavoriteOffline_replacesAlbumInstanceInStateFlow() {
        val viewModel = AlbumPageViewModel(
            albumRepository = mock<AlbumRepository>(),
            artistRepository = mock<ArtistRepository>(),
            favoriteRepository = mock<FavoriteRepository>(),
            playbackStateStore = PlaybackStateStore(),
        )
        val initialAlbum = AlbumID3(
            id = "album-id",
            name = "Album",
            artist = "Artist",
            artistId = "artist-id",
            coverArtId = "cover-art",
            starred = null,
        )

        viewModel.setPrivateAlbum(initialAlbum)
        viewModel.invokeSetFavoriteOffline(initialAlbum)

        val updatedAlbum = viewModel.privateAlbumState().value
        assertNotNull(updatedAlbum?.starred)
        assertNotSame(initialAlbum, updatedAlbum)
    }

    @Suppress("UNCHECKED_CAST")
    private fun AlbumPageViewModel.privateAlbumState(): MutableStateFlow<AlbumID3?> {
        val field = AlbumPageViewModel::class.java.getDeclaredField("_album")
        field.isAccessible = true
        return field.get(this) as MutableStateFlow<AlbumID3?>
    }

    private fun AlbumPageViewModel.setPrivateAlbum(album: AlbumID3) {
        privateAlbumState().value = album
    }

    private fun AlbumPageViewModel.invokeSetFavoriteOffline(album: AlbumID3) {
        val method = AlbumPageViewModel::class.java.getDeclaredMethod("setFavoriteOffline", AlbumID3::class.java)
        method.isAccessible = true
        method.invoke(this, album)
    }
}
