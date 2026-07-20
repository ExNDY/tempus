package com.cappielloantonio.tempo.subsonic.models

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.Date

@Keep
open class ArtistID3(
    var id: String? = null,
    var name: String? = null,
    @SerializedName("coverArt")
    var coverArtId: String? = null,
    var albumCount: Int = 0,
    var starred: Date? = null,
) : Serializable