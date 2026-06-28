package com.cappielloantonio.tempo.util

import android.content.SharedPreferences
import com.cappielloantonio.tempo.App
import org.json.JSONException
import org.json.JSONObject

object ExternalDownloadMetadataStore {
    private const val PREF_KEY = "external_download_metadata"

    private val preferences: SharedPreferences
        get() = App.getSharedPreferences()

    private fun readAll(): JSONObject {
        val raw = preferences.getString(PREF_KEY, "{}")
        return try {
            JSONObject(raw)
        } catch (e: JSONException) {
            JSONObject()
        }
    }

    private fun writeAll(obj: JSONObject) {
        preferences.edit().putString(PREF_KEY, obj.toString()).apply()
    }

    @JvmStatic
    @Synchronized
    fun clear() {
        writeAll(JSONObject())
    }

    @JvmStatic
    @Synchronized
    fun recordSize(key: String?, size: Long) {
        if (key == null || size <= 0) {
            return
        }
        val obj = readAll()
        try {
            obj.put(key, size)
        } catch (ignored: JSONException) {
        }
        writeAll(obj)
    }

    @JvmStatic
    @Synchronized
    fun remove(key: String?) {
        if (key == null) {
            return
        }
        val obj = readAll()
        obj.remove(key)
        writeAll(obj)
    }

    @JvmStatic
    @Synchronized
    fun getSize(key: String?): Long? {
        if (key == null) {
            return null
        }
        val obj = readAll()
        if (!obj.has(key)) {
            return null
        }
        val size = obj.optLong(key, -1L)
        return if (size > 0) size else null
    }

    @JvmStatic
    @Synchronized
    fun snapshot(): Map<String, Long> {
        val obj = readAll()
        if (obj.length() == 0) {
            return emptyMap()
        }
        val sizes = HashMap<String, Long>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val size = obj.optLong(key, -1L)
            if (size > 0) {
                sizes[key] = size
            }
        }
        return sizes
    }

    @JvmStatic
    @Synchronized
    fun retainOnly(keysToKeep: Set<String>?) {
        if (keysToKeep == null || keysToKeep.isEmpty()) {
            clear()
            return
        }
        val obj = readAll()
        if (obj.length() == 0) {
            return
        }
        val keys = HashSet<String>()
        val iterator = obj.keys()
        while (iterator.hasNext()) {
            keys.add(iterator.next())
        }
        var changed = false
        for (key in keys) {
            if (!keysToKeep.contains(key)) {
                obj.remove(key)
                changed = true
            }
        }
        if (changed) {
            writeAll(obj)
        }
    }
}
