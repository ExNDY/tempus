package com.cappielloantonio.tempo.subsonic.models

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Keep
class PodcastChannel(
    @SerializedName("episode")
    var episodes: List<PodcastEpisode>? = null,
    var id: String? = null,
    var url: String? = null,
    var title: String? = null,
    var description: String? = null,
    @SerializedName("coverArt")
    var coverArtId: String? = null,
    var originalImageUrl: String? = null,
    var status: String? = null,
    var errorMessage: String? = null,
) : Serializable