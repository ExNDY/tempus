package com.cappielloantonio.tempo.ui.state

import android.content.Context
import dev.icerock.moko.resources.desc.StringDesc
import dev.icerock.moko.resources.desc.desc

data class UiText(val desc: StringDesc) {
    fun resolve(context: Context): String = desc.toString(context)

    companion object {
        fun raw(value: String): UiText = UiText(value.desc())
    }
}
