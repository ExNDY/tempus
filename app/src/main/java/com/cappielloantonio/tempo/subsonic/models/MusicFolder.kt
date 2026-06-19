package com.cappielloantonio.tempo.subsonic.models

import androidx.annotation.Keep
import java.io.Serializable

@Keep
class MusicFolder(
    var id: String? = null,
    var name: String? = null,
) : Serializable