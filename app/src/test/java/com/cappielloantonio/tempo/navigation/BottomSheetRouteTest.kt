package com.cappielloantonio.tempo.navigation

import com.cappielloantonio.tempo.ui.album.AlbumBottomSheetRouteScreen
import com.cappielloantonio.tempo.ui.artist.ArtistBottomSheetRouteScreen
import com.cappielloantonio.tempo.ui.download.DownloadedBottomSheetRouteScreen
import com.cappielloantonio.tempo.ui.playlist.PlaylistBottomSheetRouteScreen
import com.cappielloantonio.tempo.ui.song.SongBottomSheetRouteScreen
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BottomSheetRouteTest {

    @Test
    fun routeValues_encodeReservedCharactersAndUnicode() {
        val value = "исполнитель / альбом?x=1&y=2"
        val encoded = encodeNavRouteValue(value)

        assertFalse(encoded.contains(" "))
        assertFalse(encoded.contains("/"))
        assertFalse(encoded.contains("?"))
        assertFalse(encoded.contains("&"))
        assertEquals(value, URLDecoder.decode(encoded, StandardCharsets.UTF_8.name()))
    }

    @Test
    fun entityRoutes_useStableNamesAndEncodedIds() {
        val id = "id /?& Юникод"
        val encoded = encodeNavRouteValue(id)

        assertEquals("albumActions?albumId=$encoded", AlbumBottomSheetRouteScreen.route(id))
        assertEquals("artistActions?artistId=$encoded", ArtistBottomSheetRouteScreen.route(id))
        assertEquals("playlistActions?playlistId=$encoded", PlaylistBottomSheetRouteScreen.route(id))
        assertEquals(
            "songActions?songId=$encoded&itemPosition=-1",
            SongBottomSheetRouteScreen.route(id),
        )
    }

    @Test
    fun songRoute_encodesOptionalPlaylistAndPreservesPosition() {
        val songId = "song/1"
        val playlistId = "playlist?2&owner=me"

        assertEquals(
            "songActions?songId=${encodeNavRouteValue(songId)}" +
                "&playlistId=${encodeNavRouteValue(playlistId)}&itemPosition=17",
            SongBottomSheetRouteScreen.route(songId, playlistId, 17),
        )
    }

    @Test
    fun downloadedRoute_encodesBothGroupValues() {
        val type = "genre&type"
        val value = "Drum / Bass?"

        assertEquals(
            "downloadedActions?groupType=${encodeNavRouteValue(type)}" +
                "&groupValue=${encodeNavRouteValue(value)}",
            DownloadedBottomSheetRouteScreen.route(type, value),
        )
    }
}

