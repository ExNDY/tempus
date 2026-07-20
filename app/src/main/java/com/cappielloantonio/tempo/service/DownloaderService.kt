package com.cappielloantonio.tempo.service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.NotificationUtil
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Requirements
import androidx.media3.exoplayer.scheduler.Scheduler
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.util.DownloadUtil
class DownloaderService : DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    DownloadUtil.DOWNLOAD_NOTIFICATION_CHANNEL_ID,
    R.string.exo_download_notification_channel_name,
    0
) {
    override fun onCreate() {
        super.onCreate()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            DownloadUtil.DOWNLOAD_NOTIFICATION_CHANNEL_ID,
            getString(R.string.exo_download_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW // Silent by default
        )
        notificationManager.createNotificationChannel(channel)
    }
    override fun getDownloadManager(): DownloadManager {
        val downloadManager = DownloadUtil.getDownloadManager(this)
        val downloadNotificationHelper = DownloadUtil.getDownloadNotificationHelper(this)
        downloadManager.addListener(
            TerminalStateNotificationHelper(
                this,
                downloadNotificationHelper,
                SUMMARY_NOTIFICATION_ID
            )
        )
        return downloadManager
    }
    override fun getScheduler(): Scheduler {
        return PlatformScheduler(this, JOB_ID)
    }
    override fun getForegroundNotification(
        downloads: List<Download>,
        @Requirements.RequirementFlags notMetRequirements: Int
    ): Notification {
        // Remove the summary notification if a new download has started
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(SUMMARY_NOTIFICATION_ID)
        var contentTitle: String? = null
        if (downloads.isNotEmpty()) {
            var displayDownload: Download? = null
            var maxIndex = -1
            
            // Only consider those that are being downloaded or already downloaded in this batch
            for (d in downloads) {
                // Ignore those that are just queued (STATE_QUEUED) for calculating the current "passed" index
                if (d.state == Download.STATE_QUEUED) continue
                val dData = d.request.data
                if (dData.isNotEmpty()) {
                    val dDataString = Util.fromUtf8Bytes(dData)
                    val dParts = dDataString.split("|")
                    if (dParts.size == 3) {
                        try {
                            val idx = dParts[1].toInt()
                            if (idx > maxIndex) {
                                maxIndex = idx
                                displayDownload = d
                            }
                        } catch (ignored: NumberFormatException) {
                        }
                    }
                }
            }
            // If all are in queue (start), take the first one for the header
            if (displayDownload == null) {
                displayDownload = downloads[0]
            }
            // Form the header
            displayDownload.let {
                val data = it.request.data
                if (data.isNotEmpty()) {
                    val dataString = Util.fromUtf8Bytes(data)
                    val parts = dataString.split("|")
                    if (parts.size == 3) {
                        val batchName = parts[0]
                        val index = if (maxIndex == -1) "1" else maxIndex.toString()
                        val total = parts[2]
                        contentTitle = getString(R.string.downloader_downloading_batch, batchName, index, total)
                    }
                }
            }
        }
        val notification = DownloadUtil.getDownloadNotificationHelper(this).buildProgressNotification(
            this,
            R.drawable.ic_download,
            null,
            contentTitle,
            downloads,
            notMetRequirements
        )
        // Add setOnlyAlertOnce(true) so that there is no sound/vibration with each progress update
        return NotificationCompat.Builder(this, DownloadUtil.DOWNLOAD_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(contentTitle)
            .setOnlyAlertOnce(true)
            .setProgress(notification.extras.getInt(Notification.EXTRA_PROGRESS_MAX), notification.extras.getInt(Notification.EXTRA_PROGRESS), notification.extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE))
            .setOngoing(true)
            .build()
    }
    private class TerminalStateNotificationHelper(
        private val context: Context,
        private val notificationHelper: DownloadNotificationHelper,
        private val summaryNotificationId: Int
    ) : DownloadManager.Listener {
        private var sessionCompletedCount = 0
        private var sessionFailedCount = 0
        private var lastCompletedDownloadId: String? = null
        private val sessionBatchNames = mutableSetOf<String>()
        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: Exception?
        ) {
            if (download.state == Download.STATE_COMPLETED) {
                sessionCompletedCount++
                lastCompletedDownloadId = download.request.id
                
                val data = download.request.data
                if (data.isNotEmpty()) {
                    val dataString = Util.fromUtf8Bytes(data)
                    val parts = dataString.split("|")
                    if (parts.size == 3) {
                        sessionBatchNames.add(parts[0])
                    }
                }
                
                DownloaderManager.updateRequestDownload(download)
            } else if (download.state == Download.STATE_FAILED) {
                sessionFailedCount++
            }
        }
        override fun onIdle(downloadManager: DownloadManager) {
            if (sessionCompletedCount > 0 || sessionFailedCount > 0) {
                val message: String
                val icon: Int
                if (sessionFailedCount == 0) {
                    icon = R.drawable.ic_check_circle
                    message = when {
                        sessionCompletedCount == 1 -> {
                            val songTitle = lastCompletedDownloadId?.let {
                                DownloaderManager.getDownloadNotificationMessage(it)
                            }
                            if (songTitle != null) context.getString(R.string.downloader_downloaded_title, songTitle) else context.getString(R.string.downloader_download_completed)
                        }
                        sessionBatchNames.size == 1 -> {
                            val albumName = sessionBatchNames.first()
                            context.getString(R.string.downloader_album_downloaded, albumName)
                        }
                        else -> context.getString(R.string.downloader_all_downloads_completed, sessionCompletedCount)
                    }
                } else if (sessionCompletedCount == 0) {
                    icon = R.drawable.ic_error
                    message = if (sessionFailedCount == 1) context.getString(R.string.downloader_error_loading) else context.getString(R.string.downloader_errors_loading, sessionFailedCount)
                } else {
                    icon = R.drawable.ic_error
                    message = context.getString(R.string.downloader_downloaded_and_errors, sessionCompletedCount, sessionFailedCount)
                }
                val summaryBuilder = NotificationCompat.Builder(context, DownloadUtil.DOWNLOAD_NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(icon)
                    .setContentTitle(message)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(false) // Final notification can make a sound
                if (sessionFailedCount > 0) {
                    // Use ACTION_RESTART which DownloadService handles for restart
                    val retryIntent = Intent("androidx.media3.exoplayer.downloadService.action.RESTART")
                    retryIntent.setPackage(context.packageName)
                    val retryPendingIntent = PendingIntent.getService(context, 0, retryIntent, PendingIntent.FLAG_IMMUTABLE)
                    summaryBuilder.addAction(R.drawable.ic_refresh, context.getString(R.string.downloader_retry), retryPendingIntent)
                }
                NotificationUtil.setNotification(context, summaryNotificationId, summaryBuilder.build())
            }
            sessionCompletedCount = 0
            sessionFailedCount = 0
            lastCompletedDownloadId = null
            sessionBatchNames.clear()
        }
        override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
            DownloaderManager.removeRequestDownload(download)
        }
    }
    companion object {
        private const val JOB_ID = 1
        private const val FOREGROUND_NOTIFICATION_ID = 1
        private const val SUMMARY_NOTIFICATION_ID = 100
    }
}
