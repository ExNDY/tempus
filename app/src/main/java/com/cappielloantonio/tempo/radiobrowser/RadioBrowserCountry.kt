package com.cappielloantonio.tempo.radiobrowser

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
class RadioBrowserCountry {
    var name: String? = null

    @SerializedName("iso_3166_1")
    var isoCode: String? = null

    @SerializedName("stationcount")
    var stationCount: Int = 0
}
