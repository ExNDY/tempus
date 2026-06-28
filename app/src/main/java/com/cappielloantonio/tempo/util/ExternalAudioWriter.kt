package com.cappielloantonio.tempo.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.repository.DownloadRepository
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.activity.MainActivity
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@UnstableApi
object ExternalAudioWriter {
    private val EXECUTOR: ExecutorService = Executors.newSingleThreadExecutor()
    private const val BUFFER_SIZE = 8192
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 60_000

    private fun sanitizeFileName(name: String): String {
        var sanitized = name.replace("[\\\\/:*?\\\"<>|]".toRegex(), "_")
        sanitized = sanitized.replace("\\s+".toRegex(), " ").trim()
        return sanitized
    }

    private fun normalizeForComparison(name: String): String {
        var s = sanitizeFileName(name)
        s = Normalizer.normalize(s, Normalizer.Form.NFKD)
        s = s.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return s.lowercase(Locale.ROOT)
    }

    private fun findFile(dir: DocumentFile, fileName: String): DocumentFile? {
        val normalized = normalizeForComparison(fileName)
        for (file in dir.listFiles()) {
            if (file.isDirectory) continue
            val existing = file.name
            if (existing != null && normalizeForComparison(existing) == normalized) {
                return file
            }
        }
        return null
    }

    @JvmStatic
    fun downloadToUserDirectory(context: Context?, child: Child?) {
        downloadToUserDirectory(context, child, null, null)
    }

    @JvmStatic
    fun downloadToUserDirectory(
        context: Context?,
        child: Child?,
        playlistId: String?,
        playlistName: String?
    ) {
        if (context == null || child == null) return
        val appContext = context.applicationContext
        val mediaItem = MappingUtil.mapDownload(child)
        val fallbackName = child.title ?: child.id

        EXECUTOR.execute {
            performDownload(
                appContext,
                mediaItem,
                fallbackName,
                child,
                playlistId,
                playlistName
            )
        }
    }

    private fun performDownload(
        context: Context,
        mediaItem: MediaItem?,
        fallbackName: String?,
        child: Child,
        playlistId: String?,
        playlistName: String?
    ) {
        val uriString = Preferences.getDownloadDirectoryUri()
        if (uriString == null) {
            notifyUnavailable(context)
            return
        }

        val directory = DocumentFile.fromTreeUri(context, Uri.parse(uriString))
        if (directory == null || !directory.canWrite()) {
            notifyFailure(
                context,
                context.getString(R.string.download_notification_cannot_write_to_folder)
            )
            return
        }

        val artist = child.artist ?: ""
        val title = child.title ?: (fallbackName ?: "")
        val album = child.album ?: ""
        var baseName = if (artist.isEmpty()) title else "$artist - $title"
        if (album.isNotEmpty()) baseName += " ($album)"
        if (baseName.isEmpty()) {
            baseName = fallbackName ?: "download"
        }
        val metadataKey = normalizeForComparison(baseName)

        val mediaUri = mediaItem?.requestMetadata?.mediaUri
        if (mediaUri == null) {
            notifyFailure(context, context.getString(R.string.download_notification_invalid_media_uri))
            ExternalDownloadMetadataStore.remove(metadataKey)
            return
        }

        val scheme = mediaUri.scheme?.lowercase(Locale.ROOT) ?: ""

        var connection: HttpURLConnection? = null
        var sourceDocument: DocumentFile? = null
        var sourceFile: File? = null
        var remoteLength: Long = -1
        var mimeType: String? = null
        var targetFile: DocumentFile? = null

        try {
            when (scheme) {
                "http", "https" -> {
                    connection = URL(mediaUri.toString()).openConnection() as HttpURLConnection
                    connection.connectTimeout = CONNECT_TIMEOUT_MS
                    connection.readTimeout = READ_TIMEOUT_MS
                    connection.setRequestProperty("Accept-Encoding", "identity")
                    connection.connect()

                    val responseCode = connection.responseCode
                    if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
                        notifyFailure(
                            context,
                            context.getString(R.string.download_notification_server_returned, responseCode)
                        )
                        ExternalDownloadMetadataStore.remove(metadataKey)
                        return
                    }

                    mimeType = connection.contentType
                    remoteLength = connection.contentLengthLong
                }

                "content" -> {
                    sourceDocument = DocumentFile.fromSingleUri(context, mediaUri)
                    mimeType = context.contentResolver.getType(mediaUri)
                    if (sourceDocument != null) {
                        remoteLength = sourceDocument.length()
                    }
                }

                "file" -> {
                    val path = mediaUri.path
                    if (path != null) {
                        sourceFile = File(path)
                        if (sourceFile.exists()) {
                            remoteLength = sourceFile.length()
                        }
                    }
                    val ext = MimeTypeMap.getFileExtensionFromUrl(mediaUri.toString())
                    if (ext != null && ext.isNotEmpty()) {
                        mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
                    }
                }

                else -> {
                    notifyFailure(
                        context,
                        context.getString(R.string.download_notification_unsupported_media_uri)
                    )
                    ExternalDownloadMetadataStore.remove(metadataKey)
                    return
                }
            }

            if (mimeType == null || mimeType.isEmpty()) {
                mimeType = "application/octet-stream"
            }

            var extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            if ((extension == null || extension.isEmpty()) && sourceDocument?.name != null) {
                val name = sourceDocument.name!!
                val dot = name.lastIndexOf('.')
                if (dot >= 0 && dot < name.length - 1) {
                    extension = name.substring(dot + 1)
                }
            }
            if ((extension == null || extension.isEmpty()) && sourceFile != null) {
                val name = sourceFile.name
                val dot = name.lastIndexOf('.')
                if (dot >= 0 && dot < name.length - 1) {
                    extension = name.substring(dot + 1)
                }
            }
            if (extension == null || extension.isEmpty()) {
                val suffix = child.suffix
                extension = if (suffix != null && suffix.isNotEmpty()) {
                    suffix
                } else {
                    "bin"
                }
            }

            var sanitized = sanitizeFileName(baseName)
            if (sanitized.isEmpty()) sanitized = sanitizeFileName(fallbackName ?: "download")
            if (sanitized.isEmpty()) sanitized = "download"
            val fileName = "$sanitized.$extension"

            val existingFile = findFile(directory, fileName)
            val recordedSize = ExternalDownloadMetadataStore.getSize(metadataKey)
            if (existingFile != null && existingFile.exists()) {
                val localLength = existingFile.length()
                var matches = false
                if (remoteLength > 0 && localLength == remoteLength) {
                    matches = true
                } else if (remoteLength <= 0L && recordedSize != null && localLength == recordedSize) {
                    matches = true
                }
                if (matches) {
                    ExternalDownloadMetadataStore.recordSize(metadataKey, localLength)
                    recordDownload(child, existingFile.uri, playlistId, playlistName)
                    ExternalAudioReader.refreshCacheAsync()
                    notifyExists(context, fileName)
                    return
                } else {
                    existingFile.delete()
                    ExternalDownloadMetadataStore.remove(metadataKey)
                }
            }

            targetFile = directory.createFile(mimeType, fileName)
            if (targetFile == null) {
                notifyFailure(
                    context,
                    context.getString(R.string.download_notification_failed_to_create_file)
                )
                return
            }

            val targetUri = targetFile.uri
            try {
                openInputStream(context, mediaUri, scheme, connection, sourceFile).use { `in` ->
                    context.contentResolver.openOutputStream(targetUri).use { out ->
                        if (out == null) {
                            notifyFailure(
                                context,
                                context.getString(R.string.download_notification_cannot_open_output_stream)
                            )
                            targetFile.delete()
                            return
                        }

                        val buffer = ByteArray(BUFFER_SIZE)
                        var len: Int
                        var total: Long = 0
                        while (`in`.read(buffer).also { len = it } != -1) {
                            out.write(buffer, 0, len)
                            total += len.toLong()
                        }
                        out.flush()

                        if (total <= 0) {
                            targetFile.delete()
                            ExternalDownloadMetadataStore.remove(metadataKey)
                            notifyFailure(
                                context,
                                context.getString(R.string.download_notification_empty_download)
                            )
                            return
                        }

                        if (remoteLength > 0 && total != remoteLength) {
                            targetFile.delete()
                            ExternalDownloadMetadataStore.remove(metadataKey)
                            notifyFailure(
                                context,
                                context.getString(R.string.download_notification_incomplete_download)
                            )
                            return
                        }

                        ExternalDownloadMetadataStore.recordSize(metadataKey, total)
                        recordDownload(child, targetUri, playlistId, playlistName)
                        notifySuccess(context, fileName, child, targetUri)
                        ExternalAudioReader.refreshCacheAsync()
                    }
                }
            } catch (e: Exception) {
                targetFile.delete()
                throw e
            }
        } catch (e: Exception) {
            if (targetFile != null) {
                targetFile.delete()
            }
            ExternalDownloadMetadataStore.remove(metadataKey)
            notifyFailure(
                context,
                if (e.message != null) e.message!! else context.getString(R.string.download_notification_failed)
            )
        } finally {
            connection?.disconnect()
        }
    }

    private fun notifyUnavailable(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val settingsIntent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        val openSettings = PendingIntent.getActivity(
            context, 0, settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, DownloadUtil.DOWNLOAD_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.download_notification_no_folder_title))
            .setContentText(context.getString(R.string.download_notification_no_folder_text))
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setContentIntent(openSettings)
            .setAutoCancel(true)

        manager.notify(1011, builder.build())
    }

    private fun notifyFailure(context: Context, message: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context, DownloadUtil.DOWNLOAD_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.download_notification_failed))
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun notifySuccess(context: Context, name: String, child: Child, fileUri: Uri) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context, DownloadUtil.DOWNLOAD_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.downloader_download_completed))
            .setContentText(name)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)

        val playIntent = buildPlayIntent(context, child, fileUri)
        if (playIntent != null) {
            builder.setContentIntent(playIntent)
        }

        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun recordDownload(
        child: Child,
        fileUri: Uri?,
        playlistId: String?,
        playlistName: String?
    ) {
        val download = Download(child)
        download.downloadState = 1
        download.playlistId = playlistId
        download.playlistName = playlistName

        if (fileUri != null) {
            download.downloadUri = fileUri.toString()
        }

        DownloadRepository().insert(download)
    }

    private fun notifyExists(context: Context, name: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context, DownloadUtil.DOWNLOAD_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.download_notification_already_downloaded))
            .setContentText(name)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setAutoCancel(true)
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun buildPlayIntent(context: Context, child: Child, fileUri: Uri?): PendingIntent? {
        if (fileUri == null) return null
        val intent = Intent(context, MainActivity::class.java)
            .setAction(Constants.ACTION_PLAY_EXTERNAL_DOWNLOAD)
            .putExtra(Constants.EXTRA_DOWNLOAD_URI, fileUri.toString())
            .putExtra(Constants.EXTRA_DOWNLOAD_MEDIA_ID, child.id)
            .putExtra(Constants.EXTRA_DOWNLOAD_TITLE, child.title)
            .putExtra(Constants.EXTRA_DOWNLOAD_ARTIST, child.artist)
            .putExtra(Constants.EXTRA_DOWNLOAD_ALBUM, child.album)
            .putExtra(
                Constants.EXTRA_DOWNLOAD_DURATION,
                if (child.duration != null) child.duration else 0
            )
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val requestCode = child.id?.hashCode()?.let { Math.abs(it) } ?: Math.abs(fileUri.toString().hashCode())

        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @Throws(IOException::class)
    private fun openInputStream(
        context: Context,
        mediaUri: Uri,
        scheme: String,
        connection: HttpURLConnection?,
        sourceFile: File?
    ): InputStream {
        return when (scheme) {
            "http", "https" -> {
                if (connection == null) {
                    throw IOException("Connection not initialized")
                }
                connection.inputStream
            }

            "content" -> {
                context.contentResolver.openInputStream(mediaUri)
                    ?: throw IOException("Cannot open content stream")
            }

            "file" -> {
                if (sourceFile == null || !sourceFile.exists()) {
                    throw IOException("Missing source file")
                }
                FileInputStream(sourceFile)
            }

            else -> throw IOException("Unsupported scheme $scheme")
        }
    }
}
