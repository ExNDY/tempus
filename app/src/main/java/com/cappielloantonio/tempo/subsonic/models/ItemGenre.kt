package com.cappielloantonio.tempo.subsonic.models

import androidx.annotation.Keep
import java.io.Serializable

@Keep
open class ItemGenre(
    var name: String? = null,
) : Serializable