package com.cappielloantonio.tempo.subsonic.models

import androidx.annotation.Keep
import java.io.Serializable

@Keep
open class DiscTitle(
    var disc: Int? = null,
    var title: String? = null,
) : Serializable