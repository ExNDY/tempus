package com.cappielloantonio.tempo.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

inline fun <reified T : Activity> Context.requireActivity(): T {
    return findActivity() as? T
        ?: error("Unable to find ${T::class.java.simpleName} from ${javaClass.name}")
}
