package com.cappielloantonio.tempo.subsonic.models

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Keep
class ArtistWithAlbumsID3(
    @SerializedName("album")
    var albums: List<AlbumID3>? = null,
    @SerializedName("appearsOn")
    var appearsOn: List<AlbumID3>? = null,
) : ArtistID3(), Serializable