package com.cappielloantonio.tempo.util

import android.os.Bundle
import androidx.core.os.BundleCompat
import java.io.Serializable

inline fun <reified T : Serializable> Bundle.serializable(key: String): T? {
    classLoader = T::class.java.classLoader
    return BundleCompat.getSerializable(this, key, T::class.java)
}

inline fun <reified T : Serializable> Bundle.serializableArrayList(key: String): ArrayList<T> {
    classLoader = T::class.java.classLoader
    val rawList = BundleCompat.getSerializable(this, key, ArrayList::class.java) ?: return arrayListOf()
    return ArrayList(rawList.filterIsInstance<T>())
}
