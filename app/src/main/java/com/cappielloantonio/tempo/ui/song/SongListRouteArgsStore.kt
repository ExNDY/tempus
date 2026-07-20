package com.cappielloantonio.tempo.ui.song

import android.os.Bundle
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object SongListRouteArgsStore {
    private val routeArgs = ConcurrentHashMap<String, Bundle>()

    fun put(bundle: Bundle): String {
        val token = UUID.randomUUID().toString()
        routeArgs[token] = Bundle(bundle)
        return token
    }

    fun consume(token: String?): Bundle? {
        if (token.isNullOrBlank()) return null
        return routeArgs.remove(token)
    }
}
