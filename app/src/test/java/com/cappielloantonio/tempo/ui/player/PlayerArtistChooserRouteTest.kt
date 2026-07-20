package com.cappielloantonio.tempo.ui.player

import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerArtistChooserRouteTest {

    @Test
    fun routeStoresArtistsUntilRemoved() {
        val artists = listOf(
            ArtistID3(id = "artist-1", name = "First"),
            ArtistID3(id = "artist-2", name = "Second"),
        )

        val route = PlayerArtistChooserRouteScreen.route(artists)
        val token = URLDecoder.decode(route.substringAfterLast('/'), StandardCharsets.UTF_8.name())

        assertTrue(route.startsWith("playerArtistChooser/"))
        assertEquals(artists, PlayerArtistChooserRouteArgsStore.get(token))
        assertEquals(artists, PlayerArtistChooserRouteArgsStore.get(token))

        PlayerArtistChooserRouteArgsStore.remove(token)
        assertTrue(PlayerArtistChooserRouteArgsStore.get(token).isEmpty())
    }
}
