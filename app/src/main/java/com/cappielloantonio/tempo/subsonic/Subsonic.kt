package com.cappielloantonio.tempo.subsonic

import com.cappielloantonio.tempo.subsonic.base.Version

class Subsonic(private val preferences: SubsonicPreferences) {
    val apiVersion: Version = API_MAX_VERSION

    val url: String
        get() {
            var serverUrl = preferences.serverUrl
            if (serverUrl.isNullOrBlank()) {
                return "http://localhost/rest/"
            }
            if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
                serverUrl = "http://$serverUrl"
            }
            val url = "$serverUrl/rest/"
            return url.replace("//rest", "/rest")
        }

    val params: Map<String, String>
        get() {
            val params = mutableMapOf<String, String>()
            preferences.username?.let { params["u"] = it }

            preferences.authentication?.let { auth ->
                auth.password?.let { params["p"] = it }
                auth.salt?.let { params["s"] = it }
                auth.token?.let { params["t"] = it }
            }

            params["v"] = apiVersion.versionString
            params["c"] = preferences.clientName
            params["f"] = "json"

            return params
        }

    companion object {
        private val API_MAX_VERSION = Version.of("1.15.0")
    }
}
