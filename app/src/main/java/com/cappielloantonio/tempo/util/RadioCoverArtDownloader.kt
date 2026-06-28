package com.cappielloantonio.tempo.util

import android.content.Context
import android.util.Log
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.glide.CustomGlideRequest
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object RadioCoverArtDownloader {
    private const val TAG = "RadioCoverArtDownloader"
    private const val COVER_DIR = "radio_covers"
    private val executor: ExecutorService = Executors.newFixedThreadPool(2)

    @JvmStatic
    fun getCoverDir(): File {
        val context = App.getContext()
        val dir = File(context.filesDir, COVER_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    @JvmStatic
    fun getLocalCoverFile(stationId: String): File {
        return File(getCoverDir(), stationId)
    }

    @JvmStatic
    fun downloadCoverArt(stationId: String, coverArtUrl: String?) {
        if (coverArtUrl.isNullOrEmpty()) return

        executor.execute {
            try {
                val targetFile = getLocalCoverFile(stationId)
                val urlString = resolveCoverArtUrl(coverArtUrl) ?: return@execute

                if (urlString.contains("/rest/getCoverArt")) {
                    downloadViaCoil(urlString, targetFile)
                } else {
                    downloadDirect(urlString, targetFile)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download cover art for station $stationId", e)
            }
        }
    }

    private fun resolveCoverArtUrl(coverArtUrl: String): String? {
        if (coverArtUrl.startsWith("http://") || coverArtUrl.startsWith("https://")) {
            return coverArtUrl
        }
        return CustomGlideRequest.createUrl(coverArtUrl, Preferences.getImageSize())
    }

    private fun downloadDirect(urlString: String, targetFile: File): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.instanceFollowRedirects = true

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "HTTP $responseCode downloading cover from $urlString")
                false
            } else {
                connection.inputStream.use { `in` ->
                    FileOutputStream(targetFile).use { out ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (`in`.read(buffer).also { bytesRead = it } != -1) {
                            out.write(buffer, 0, bytesRead)
                        }
                    }
                }
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download cover from $urlString", e)
            false
        } finally {
            connection?.disconnect()
        }
    }

    private fun downloadViaCoil(urlString: String, targetFile: File): Boolean {
        return try {
            val context = App.getContext()
            val cachedFile = CustomGlideRequest.getCachedFileBlocking(context, urlString, urlString)

            if (cachedFile != null && cachedFile.exists()) {
                FileInputStream(cachedFile).use { `in` ->
                    FileOutputStream(targetFile).use { out ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (`in`.read(buffer).also { bytesRead = it } != -1) {
                            out.write(buffer, 0, bytesRead)
                        }
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download cover via Coil from $urlString", e)
            false
        }
    }

    @JvmStatic
    fun deleteCoverArt(stationId: String) {
        executor.execute {
            val file = getLocalCoverFile(stationId)
            if (file.exists()) {
                file.delete()
            }
        }
    }
}
