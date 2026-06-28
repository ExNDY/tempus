package com.cappielloantonio.tempo.radiobrowser

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
class RadioBrowserStation {
    @SerializedName("stationuuid")
    var stationUuid: String? = null
    var name: String? = null
    var url: String? = null

    @SerializedName("url_resolved")
    var urlResolved: String? = null
    var homepage: String? = null
    var favicon: String? = null
    var tags: String? = null
    var country: String? = null

    @SerializedName("countrycode")
    var countryCode: String? = null
    var language: String? = null
    var codec: String? = null
    var bitrate: Int = 0

    @SerializedName("clickcount")
    var clickCount: Int = 0

    @SerializedName("votes")
    var votes: Int = 0
}
