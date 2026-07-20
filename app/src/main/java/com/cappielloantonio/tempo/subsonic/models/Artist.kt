package com.cappielloantonio.tempo.subsonic.models

import androidx.annotation.Keep
import java.io.Serializable
import java.util.Date

@Keep
class Artist(
    var id: String? = null,
    var name: String? = null,
    var starred: Date? = null,
    var userRating: Int? = null,
    var averageRating: Double? = null,
) : Serializable