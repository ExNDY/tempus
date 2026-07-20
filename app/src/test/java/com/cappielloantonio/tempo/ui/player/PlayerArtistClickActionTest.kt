package com.cappielloantonio.tempo.ui.player

import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.viewmodel.PlayerUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerArtistClickActionTest {

    @Test
    fun playerArtistClickAction_returnsNoneWithoutArtistData() {
        assertSame(
            PlayerArtistClickAction.None,
            playerArtistClickAction(PlayerUiState(currentSong = Child("song-1"))),
        )
    }

    @Test
    fun playerArtistClickAction_opensSingleResolvedArtist() {
        val song = Child("song-1").apply {
            artistId = "artist-1"
            artist = "Single Artist"
        }

        val action = playerArtistClickAction(PlayerUiState(currentSong = song))

        assertEquals(PlayerArtistClickAction.OpenArtist("artist-1"), action)
    }

    @Test
    fun playerArtistClickAction_choosesBetweenMultipleArtists() {
        val artists = listOf(
            ArtistID3(id = "artist-1", name = "First"),
            ArtistID3(id = "artist-2", name = "Second"),
        )
        val song = Child("song-1").apply {
            this.artists = artists
        }

        val action = playerArtistClickAction(PlayerUiState(currentSong = song))

        assertTrue(action is PlayerArtistClickAction.ChooseArtist)
        assertEquals(artists, (action as PlayerArtistClickAction.ChooseArtist).artists)
    }
}
