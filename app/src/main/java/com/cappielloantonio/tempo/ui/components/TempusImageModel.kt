package com.cappielloantonio.tempo.ui.components

import androidx.annotation.DrawableRes
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.util.Util

enum class TempusImageType(
    @DrawableRes val placeholderRes: Int,
) {
    Unknown(R.drawable.ui_splash_screen),
    Album(R.drawable.ic_placeholder_album),
    Artist(R.drawable.ic_placeholder_artist),
    Folder(R.drawable.ic_placeholder_folder),
    Directory(R.drawable.ic_placeholder_directory),
    Playlist(R.drawable.ic_placeholder_playlist),
    Podcast(R.drawable.ic_placeholder_podcast),
    Radio(R.drawable.ic_placeholder_radio),
    Song(R.drawable.ic_placeholder_song),
}

data class TempusImageModel(
    val url: String?,
    val memoryCacheKey: String?,
    val diskCacheKey: String?,
) {
    companion object {
        fun from(
            coverArtId: String?,
            type: TempusImageType,
        ): TempusImageModel {
            if (coverArtId.isNullOrBlank() || Preferences.isDataSavingMode()) {
                return TempusImageModel(
                    url = null,
                    memoryCacheKey = null,
                    diskCacheKey = null,
                )
            }

            val imageSize = Preferences.getImageSize()
            val serverId = Preferences.getServerId().orEmpty()
            val cacheKey = "$serverId:${type.name}:$coverArtId:$imageSize"

            return TempusImageModel(
                url = createCoverArtUrl(coverArtId, imageSize),
                memoryCacheKey = cacheKey,
                diskCacheKey = cacheKey,
            )
        }

        fun createCoverArtUrl(
            coverArtId: String,
            size: Int = Preferences.getImageSize(),
        ): String {
            val params = App.getSubsonicClientInstance(false).params
            val uri = StringBuilder()

            uri.append(App.getSubsonicClientInstance(false).url)
            uri.append("getCoverArt")

            if (params["u"] != null) uri.append("?u=").append(Util.encode(params["u"]))
            if (params["p"] != null) uri.append("&p=").append(params["p"])
            if (params["s"] != null) uri.append("&s=").append(params["s"])
            if (params["t"] != null) uri.append("&t=").append(params["t"])
            if (params["v"] != null) uri.append("&v=").append(params["v"])
            if (params["c"] != null) uri.append("&c=").append(params["c"])
            if (size != -1) uri.append("&size=").append(size)

            uri.append("&id=").append(coverArtId)

            return uri.toString()
        }
    }
}
