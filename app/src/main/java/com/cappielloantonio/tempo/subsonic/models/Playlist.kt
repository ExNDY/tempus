package com.cappielloantonio.tempo.subsonic.models

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.Date

@Keep
@Entity(tableName = "playlist")
open class Playlist(
    @PrimaryKey
    @ColumnInfo(name = "id")
    open var id: String = "",

    @ColumnInfo(name = "name")
    var name: String? = null,

    @ColumnInfo(name = "duration")
    var duration: Long = 0,

    @SerializedName("coverArt")
    @ColumnInfo(name = "coverArt")
    var coverArtId: String? = null,

    var comment: String? = null,

    var owner: String? = null,

    @SerializedName("public")
    var isUniversal: Boolean? = null,

    @ColumnInfo(name = "songCount", defaultValue = "0")
    var songCount: Int = 0,

    var created: Date? = null,

    var changed: Date? = null,

    @ColumnInfo(name = "allowedUsers")
    var allowedUsers: List<String>? = null,

    @ColumnInfo(name = "isPinned", defaultValue = "0")
    var isPinned: Boolean = false,
) : Serializable
