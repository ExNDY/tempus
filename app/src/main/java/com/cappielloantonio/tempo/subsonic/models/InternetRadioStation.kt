package com.cappielloantonio.tempo.subsonic.models

import androidx.annotation.Keep
import java.io.Serializable
import com.google.gson.annotations.SerializedName

@Keep
class InternetRadioStation(
    var id: String? = null,
    var name: String? = null,
    var streamUrl: String? = null,
    @SerializedName("homePageUrl", alternate = ["homepageUrl"])
    var homePageUrl: String? = null,
    var coverArt: String? = null,
    var source: String? = null,
) : Serializable
