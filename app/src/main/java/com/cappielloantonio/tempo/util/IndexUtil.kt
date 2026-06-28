package com.cappielloantonio.tempo.util

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.subsonic.models.Artist
import com.cappielloantonio.tempo.subsonic.models.Indexes

@OptIn(UnstableApi::class)
object IndexUtil {
    @JvmStatic
    fun getArtist(indexes: Indexes): List<Artist> {
        val indices = indexes.indices ?: return emptyList()
        val toReturn = ArrayList<Artist>()
        for (index in indices) {
            val artists = index.artists
            if (artists != null) {
                toReturn.addAll(artists)
            }
        }
        return toReturn
    }
}
