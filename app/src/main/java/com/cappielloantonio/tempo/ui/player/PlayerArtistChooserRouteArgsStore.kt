package com.cappielloantonio.tempo.ui.player

import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object PlayerArtistChooserRouteArgsStore {
    private val routeArgs = ConcurrentHashMap<String, List<ArtistID3>>()

    fun put(artists: List<ArtistID3>): String {
        val token = UUID.randomUUID().toString()
        routeArgs[token] = artists
        return token
    }

    fun get(token: String?): List<ArtistID3> {
        if (token.isNullOrBlank()) return emptyList()
        return routeArgs[token].orEmpty()
    }

    fun remove(token: String?) {
        if (!token.isNullOrBlank()) {
            routeArgs.remove(token)
        }
    }
}
