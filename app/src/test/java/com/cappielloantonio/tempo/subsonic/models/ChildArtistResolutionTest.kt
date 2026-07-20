package com.cappielloantonio.tempo.subsonic.models

import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChildArtistResolutionTest {

    @Test
    fun resolvedArtists_usesOpenSubsonicArtistsInOrder() {
        val song = Child("song-1").apply {
            artists = listOf(
                ArtistID3(id = "artist-1", name = "First"),
                ArtistID3(id = "", name = "Ignored"),
                ArtistID3(id = "artist-2", name = "Second"),
                ArtistID3(id = "artist-1", name = "Duplicate"),
            )
            artistId = "legacy"
            artist = "Legacy"
        }

        val resolved = song.resolvedArtists()

        assertEquals(listOf("artist-1", "artist-2"), resolved.map { it.id })
        assertEquals(listOf("First", "Second"), resolved.map { it.name })
    }

    @Test
    fun resolvedArtists_fallsBackToLegacyArtistFields() {
        val starred = Date(1000)
        val song = Child("song-1").apply {
            artistId = "legacy-artist"
            artist = "Legacy Artist"
        }
        val fallback = ArtistID3(
            id = "legacy-artist",
            name = "Fetched Artist",
            coverArtId = "cover-id",
            albumCount = 7,
            starred = starred,
        )

        val resolved = song.resolvedArtists(fallback)

        assertEquals(1, resolved.size)
        assertEquals("legacy-artist", resolved.first().id)
        assertEquals("Legacy Artist", resolved.first().name)
        assertEquals("cover-id", resolved.first().coverArtId)
        assertEquals(7, resolved.first().albumCount)
        assertEquals(starred, resolved.first().starred)
    }

    @Test
    fun resolvedArtists_usesFallbackArtistWhenSongHasNoArtistId() {
        val fallback = ArtistID3(id = "fallback-artist", name = "Fallback Artist")

        val resolved = Child("song-1").resolvedArtists(fallback)

        assertEquals(1, resolved.size)
        assertEquals("fallback-artist", resolved.first().id)
    }

    @Test
    fun resolvedArtists_returnsEmptyWhenNoArtistDataExists() {
        assertTrue(Child("song-1").resolvedArtists().isEmpty())
    }
}
