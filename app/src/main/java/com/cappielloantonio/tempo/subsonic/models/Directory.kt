package com.cappielloantonio.tempo.subsonic.models

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.Date

@Keep
class Directory(
    @SerializedName("child")
    var children: List<Child>? = null,
    var id: String? = null,
    @SerializedName("parent")
    var parentId: String? = null,
    var name: String? = null,
    var starred: Date? = null,
    var userRating: Int? = null,
    var averageRating: Double? = null,
    var playCount: Long? = null,
) : Serializable