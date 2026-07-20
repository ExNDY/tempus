package com.cappielloantonio.tempo.ui.download

import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadGroupingTest {

    private val albumSong = Child("song-1").apply {
        albumId = "album / one"
        album = "Album One"
        artistId = "artist-1"
        artist = "Artist"
        genre = "Rock & Roll"
        year = 2024
    }
    private val otherSong = Child("song-2").apply {
        albumId = "album-2"
        artistId = "artist-2"
        genre = "Jazz"
        year = 2023
    }
    private val playlistSong = Download("song-3").apply {
        playlistId = "playlist-1"
        playlistName = "Offline Mix"
    }
    private val songs = listOf(albumSong, otherSong, playlistSong)

    @Test
    fun filtersEverySupportedGroupType() {
        assertEquals(listOf(albumSong), filterDownloadedSongs(Constants.DOWNLOAD_TYPE_ALBUM, "album / one", songs))
        assertEquals(listOf(albumSong), filterDownloadedSongs(Constants.DOWNLOAD_TYPE_ARTIST, "artist-1", songs))
        assertEquals(listOf(albumSong), filterDownloadedSongs(Constants.DOWNLOAD_TYPE_GENRE, "Rock & Roll", songs))
        assertEquals(listOf(albumSong), filterDownloadedSongs(Constants.DOWNLOAD_TYPE_YEAR, "2024", songs))
        assertEquals(listOf(playlistSong), filterDownloadedSongs(Constants.DOWNLOAD_TYPE_PLAYLIST, "playlist-1", songs))
        assertEquals(listOf(otherSong), filterDownloadedSongs(Constants.DOWNLOAD_TYPE_TRACK, "song-2", songs))
    }

    @Test
    fun unknownGroupDoesNotExposeAllDownloads() {
        assertTrue(filterDownloadedSongs("unknown", "value", songs).isEmpty())
    }

    @Test
    fun groupTitleComesFromPersistedMetadataWithValueFallback() {
        assertEquals("Album One", downloadedGroupTitle(Constants.DOWNLOAD_TYPE_ALBUM, "album-1", listOf(albumSong)))
        assertEquals("Offline Mix", downloadedGroupTitle(Constants.DOWNLOAD_TYPE_PLAYLIST, "playlist-1", listOf(playlistSong)))
        assertEquals("missing-value", downloadedGroupTitle(Constants.DOWNLOAD_TYPE_ALBUM, "missing-value", emptyList()))
    }
}

