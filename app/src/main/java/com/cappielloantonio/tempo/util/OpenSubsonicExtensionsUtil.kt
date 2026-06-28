package com.cappielloantonio.tempo.util

import com.cappielloantonio.tempo.subsonic.models.OpenSubsonicExtension
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object OpenSubsonicExtensionsUtil {
    private fun getOpenSubsonicExtensions(): List<OpenSubsonicExtension>? {
        var extensions: List<OpenSubsonicExtension>? = null
        val extensionsJson = Preferences.getOpenSubsonicExtensions()
        if (Preferences.isOpenSubsonic() && extensionsJson != null) {
            val type = object : TypeToken<List<OpenSubsonicExtension>>() {}.type
            extensions = Gson().fromJson(extensionsJson, type)
        }
        return extensions
    }

    private fun getOpenSubsonicExtension(extensionName: String): OpenSubsonicExtension? {
        val extensions = getOpenSubsonicExtensions() ?: return null
        return extensions.find { it.name == extensionName }
    }

    @JvmStatic
    fun isTranscodeOffsetExtensionAvailable(): Boolean {
        return getOpenSubsonicExtension("transcodeOffset") != null
    }

    @JvmStatic
    fun isFormPostExtensionAvailable(): Boolean {
        return getOpenSubsonicExtension("formPost") != null
    }

    @JvmStatic
    fun isSongLyricsExtensionAvailable(): Boolean {
        return getOpenSubsonicExtension("songLyrics") != null
    }
}
