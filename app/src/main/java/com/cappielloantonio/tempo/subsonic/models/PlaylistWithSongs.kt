package com.cappielloantonio.tempo.subsonic.models

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Keep
class PlaylistWithSongs(
    @SerializedName("_id")
    override var id: String,
    @SerializedName("entry")
    var entries: List<Child>? = null,
) : Playlist(id), Serializable