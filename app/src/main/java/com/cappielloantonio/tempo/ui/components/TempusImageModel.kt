package com.cappielloantonio.tempo.ui.components

import androidx.annotation.DrawableRes
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.util.Util

enum class TempusImageType(
    @param:DrawableRes val placeholderRes: Int,
) {
    Unknown(placeholderRes = R.drawable.ui_splash_screen),
    Album(placeholderRes = R.drawable.ic_placeholder_album),
    Artist(placeholderRes = R.drawable.ic_placeholder_artist),
    Folder(placeholderRes = R.drawable.ic_placeholder_folder),
    Directory(placeholderRes = R.drawable.ic_placeholder_directory),
    Playlist(placeholderRes = R.drawable.ic_placeholder_playlist),
    Song(placeholderRes = R.drawable.ic_placeholder_song),
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
            val subSonicInstance = App.getSubsonicClientInstance(false)
            val params = subSonicInstance.params
            val uri = StringBuilder()

            uri.append(subSonicInstance.url)
            uri.append("getCoverArt")

            params["u"]?.let { uri.append("?u=").append(Util.encode(it)) }
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
