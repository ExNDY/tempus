package com.cappielloantonio.tempo.service

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Log
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadHelper
import androidx.media3.exoplayer.offline.DownloadIndex
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.cappielloantonio.tempo.repository.DownloadRepository
import com.cappielloantonio.tempo.util.DownloadUtil
import java.io.IOException

@androidx.media3.common.util.UnstableApi
class DownloaderManager(
    context: Context,
    private val dataSourceFactory: DataSource.Factory,
    downloadManager: DownloadManager
) {
    private val context: Context = context.applicationContext
    private val downloadIndex: DownloadIndex = downloadManager.downloadIndex

    init {
        loadDownloads()
    }

    private fun buildDownloadRequest(
        mediaItem: MediaItem,
        batchName: String?,
        index: Int,
        total: Int
    ): DownloadRequest {
        var data: ByteArray? = null
        if (batchName != null) {
            val dataString = "$batchName|$index|$total"
            data = Util.getUtf8Bytes(dataString)
        }
        return DownloadHelper
            .Factory()
            .setDataSourceFactory(dataSourceFactory)
            .create(mediaItem)
            .getDownloadRequest(mediaItem.mediaId, data)
            .copyWithId(mediaItem.mediaId)
    }

    fun isDownloaded(mediaId: String): Boolean {
        val download = downloads[mediaId]
        return download != null && download.state != Download.STATE_FAILED
    }

    fun isDownloaded(mediaItem: MediaItem): Boolean {
        return isDownloaded(mediaItem.mediaId)
    }

    fun areDownloaded(mediaItems: List<MediaItem>): Boolean {
        return mediaItems.any { isDownloaded(it) }
    }

    fun download(mediaItem: MediaItem, download: com.cappielloantonio.tempo.model.Download) {
        download.downloadUri = mediaItem.requestMetadata.mediaUri.toString()
        DownloadService.sendAddDownload(
            context,
            DownloaderService::class.java,
            buildDownloadRequest(mediaItem, null, 0, 0),
            false
        )
        insertDatabase(download)
    }

    fun download(
        mediaItems: List<MediaItem>,
        downloadsList: List<com.cappielloantonio.tempo.model.Download>
    ) {
        var batchName: String? = null
        if (mediaItems.isNotEmpty()) {
            batchName = mediaItems[0].mediaMetadata.albumTitle?.toString() ?: "Multiple tracks"
        }
        for (counter in mediaItems.indices) {
            val mediaItem = mediaItems[counter]
            val downloadModel = downloadsList[counter]
            downloadModel.downloadUri = mediaItem.requestMetadata.mediaUri.toString()
            DownloadService.sendAddDownload(
                context,
                DownloaderService::class.java,
                buildDownloadRequest(mediaItem, batchName, counter + 1, mediaItems.size),
                false
            )
            insertDatabase(downloadModel)
        }
    }

    fun remove(mediaItem: MediaItem, download: com.cappielloantonio.tempo.model.Download) {
        DownloadService.sendRemoveDownload(
            context,
            DownloaderService::class.java,
            buildDownloadRequest(mediaItem, null, 0, 0).id,
            false
        )
        deleteDatabase(download.id)
        downloads.remove(download.id)
    }

    fun remove(
        mediaItems: List<MediaItem>,
        downloadsList: List<com.cappielloantonio.tempo.model.Download>
    ) {
        for (counter in mediaItems.indices) {
            remove(mediaItems[counter], downloadsList[counter])
        }
    }

    fun removeAll() {
        DownloadService.sendRemoveAllDownloads(context, DownloaderService::class.java, false)
        deleteAllDatabase()
        DownloadUtil.eraseDownloadFolder(context)
    }

    private fun loadDownloads() {
        try {
            val loadedDownloads = downloadIndex.getDownloads()

            loadedDownloads.use { loadedDownloads ->
                while (loadedDownloads.moveToNext()) {
                    val download = loadedDownloads.download
                    downloads[download.request.id] = download
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Failed to query downloads", e)
        }
    }

    companion object {
        private const val TAG = "DownloaderManager"
        private val downloads = HashMap<String, Download>()

        @JvmStatic
        fun getDownloadNotificationMessage(id: String): String? {
            val download = downloadRepository.getDownload(id) ?: return null
            return if (download.artist != null) "${download.artist} — ${download.title}" else download.title
        }

        @JvmStatic
        fun updateRequestDownload(download: Download) {
            updateDatabase(download.request.id)
            downloads[download.request.id] = download
        }

        @JvmStatic
        fun removeRequestDownload(download: Download) {
            deleteDatabase(download.request.id)
            downloads.remove(download.request.id)
        }

        private val downloadRepository: DownloadRepository
            get() = DownloadRepository()

        private fun insertDatabase(download: com.cappielloantonio.tempo.model.Download) {
            downloadRepository.insert(download)
        }

        private fun deleteDatabase(id: String) {
            downloadRepository.delete(id)
        }

        private fun deleteAllDatabase() {
            downloadRepository.deleteAll()
        }

        private fun updateDatabase(id: String) {
            downloadRepository.update(id)
        }
    }
}
