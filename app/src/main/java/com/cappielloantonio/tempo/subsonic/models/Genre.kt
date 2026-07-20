package com.cappielloantonio.tempo.subsonic.models

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Keep
class Genre(
    @SerializedName("value")
    var genre: String? = null,
    var songCount: Int = 0,
    var albumCount: Int = 0,
) : Serializable