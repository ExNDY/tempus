package com.cappielloantonio.tempo.provider

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.BuildConfig
import com.cappielloantonio.tempo.glide.CustomGlideRequest
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.util.RadioCoverArtDownloader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AlbumArtContentProvider : ContentProvider() {
    private var executor: ExecutorService? = null

    @OptIn(UnstableApi::class)
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val context = context
        val albumId = uri.lastPathSegment
        var artworkUri: Uri? = null
        var localFile: File? = null

        if (albumId != null && albumId.startsWith("rl_")) {
            localFile = RadioCoverArtDownloader.getLocalCoverFile(albumId.substring("rl_".length))
        } else if (albumId != null && albumId.startsWith("ir_")) {
            val encodedUrl = albumId.substring("ir_".length)
            val decodedUrl = String(Base64.decode(encodedUrl, Base64.URL_SAFE or Base64.NO_WRAP))
            artworkUri = Uri.parse(decodedUrl)
        } else {
            artworkUri = Uri.parse(CustomGlideRequest.createUrl(albumId, Preferences.getImageSize()))
        }

        val localFileFinal = localFile
        val artworkUriFinal = artworkUri

        return try {
            val pipe = ParcelFileDescriptor.createPipe()
            val readSide = pipe[0]
            val writeSide = pipe[1]

            executor?.execute {
                try {
                    ParcelFileDescriptor.AutoCloseOutputStream(writeSide).use { out ->
                        val file = if (localFileFinal != null) {
                            localFileFinal
                        } else {
                            CustomGlideRequest.getCachedFileBlocking(
                                context!!,
                                artworkUriFinal,
                                artworkUriFinal?.toString()
                            )
                        }

                        if (file == null || !file.exists()) {
                            throw FileNotFoundException("Artwork not cached: $artworkUriFinal")
                        }

                        FileInputStream(file).use { `in` ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (`in`.read(buffer).also { bytesRead = it } != -1) {
                                out.write(buffer, 0, bytesRead)
                            }
                        }
                    }
                } catch (e: Exception) {
                    try {
                        writeSide.closeWithError("Failed to load image: ${e.message}")
                    } catch (ignored: IOException) {
                    }
                }
            }
            readSide
        } catch (e: IOException) {
            throw FileNotFoundException("Could not create pipe: ${e.message}")
        }
    }

    override fun onCreate(): Boolean {
        executor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2)
        )
        return true
    }

    override fun shutdown() {
        executor?.let {
            it.shutdown()
            try {
                if (!it.awaitTermination(5, TimeUnit.SECONDS)) {
                    it.shutdownNow()
                }
            } catch (e: InterruptedException) {
                it.shutdownNow()
            }
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String = ""

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int = 0

    companion object {
        val AUTHORITY = "${BuildConfig.APPLICATION_ID}.albumart.provider"
        const val ALBUM_ART = "albumArt"
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "albumArt/*", 1)
        }

        @JvmStatic
        fun contentUri(artworkId: String?): Uri {
            return Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(AUTHORITY)
                .appendPath(ALBUM_ART)
                .appendPath(artworkId)
                .build()
        }
    }
}
