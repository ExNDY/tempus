package com.cappielloantonio.tempo.viewmodel

import com.cappielloantonio.tempo.playback.PlaybackStateStore
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.FavoriteRepository
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Test
import org.mockito.kotlin.mock

class ArtistPageViewModelTest {

    @Test
    fun setFavoriteOffline_replacesArtistInstanceInStateFlow() {
        val viewModel = ArtistPageViewModel(
            artistRepository = mock<ArtistRepository>(),
            albumRepository = mock<AlbumRepository>(),
            favoriteRepository = mock<FavoriteRepository>(),
            playbackStateStore = PlaybackStateStore(),
        )
        val initialArtist = ArtistID3(
            id = "artist-id",
            name = "Artist",
            coverArtId = "cover-art",
            albumCount = 3,
            starred = null,
        )

        viewModel.setPrivateArtist(initialArtist)
        viewModel.invokeSetFavoriteOffline(initialArtist)

        val updatedArtist = viewModel.privateArtistState().value
        assertNotNull(updatedArtist?.starred)
        assertNotSame(initialArtist, updatedArtist)
    }

    @Suppress("UNCHECKED_CAST")
    private fun ArtistPageViewModel.privateArtistState(): MutableStateFlow<ArtistID3?> {
        val field = ArtistPageViewModel::class.java.getDeclaredField("_artist")
        field.isAccessible = true
        return field.get(this) as MutableStateFlow<ArtistID3?>
    }

    private fun ArtistPageViewModel.setPrivateArtist(artist: ArtistID3) {
        privateArtistState().value = artist
    }

    private fun ArtistPageViewModel.invokeSetFavoriteOffline(artist: ArtistID3) {
        val method = ArtistPageViewModel::class.java.getDeclaredMethod("setFavoriteOffline", ArtistID3::class.java)
        method.isAccessible = true
        method.invoke(this, artist)
    }
}
