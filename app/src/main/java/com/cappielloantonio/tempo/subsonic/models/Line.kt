package com.cappielloantonio.tempo.subsonic.models

import androidx.annotation.Keep

@Keep
class Line {
    var start: Long? = null
    lateinit var value: String
}
