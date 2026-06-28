package com.cappielloantonio.tempo.github.utils

import com.cappielloantonio.tempo.BuildConfig
import com.cappielloantonio.tempo.github.models.LatestRelease

object UpdateUtil {
    @JvmStatic
    fun showUpdateDialog(release: LatestRelease): Boolean {
        val tagName = release.tagName ?: return false
        val remoteTag = tagName.replace("^\\D+".toRegex(), "")
        return try {
            val local = BuildConfig.VERSION_NAME.split(".").toTypedArray()
            val remote = remoteTag.split(".").toTypedArray()
            for (i in local.indices) {
                if (i >= remote.size) break
                val localPart = local[i].toInt()
                val remotePart = remote[i].toInt()
                if (localPart > remotePart) {
                    return false
                } else if (localPart < remotePart) {
                    return true
                }
            }
            false
        } catch (exception: Exception) {
            false
        }
    }
}
