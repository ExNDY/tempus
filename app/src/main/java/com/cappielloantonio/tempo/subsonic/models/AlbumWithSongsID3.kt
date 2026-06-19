package com.cappielloantonio.tempo.subsonic.models

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Keep
class AlbumWithSongsID3(
    @SerializedName("song")
    var songs: List<Child>? = null,
) : AlbumID3(), Serializable