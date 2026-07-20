package com.cappielloantonio.tempo.util

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.scheduler.Requirements
import com.cappielloantonio.tempo.service.DownloaderManager
import java.io.File
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.Executors

@androidx.media3.common.util.UnstableApi
object DownloadUtil {
    const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "download_channel"
    const val DOWNLOAD_NOTIFICATION_SUCCESSFUL_GROUP =
        "com.cappielloantonio.tempo.SuccessfulDownload"
    const val DOWNLOAD_NOTIFICATION_FAILED_GROUP = "com.cappielloantonio.tempo.FailedDownload"
    private const val STREAMING_CACHE_CONTENT_DIRECTORY = "streaming_cache"
    private const val DOWNLOAD_CONTENT_DIRECTORY = "downloads"
    private var dataSourceFactory: DataSource.Factory? = null
    private var httpDataSourceFactory: DataSource.Factory? = null
    private var databaseProvider: DatabaseProvider? = null
    private var streamingCacheDirectory: File? = null
    private var downloadDirectory: File? = null
    private var downloadCache: Cache? = null
    private var streamingCache: SimpleCache? = null
    private var downloadManager: DownloadManager? = null
    private var downloaderManager: DownloaderManager? = null
    private var downloadNotificationHelper: DownloadNotificationHelper? = null

    @JvmStatic
    fun useExtensionRenderers(): Boolean {
        return true
    }

    @JvmStatic
    fun buildRenderersFactory(
        context: Context,
        preferExtensionRenderer: Boolean
    ): RenderersFactory {
        val extensionRendererMode = if (useExtensionRenderers()) {
            if (preferExtensionRenderer) DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
        } else {
            DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
        }
        return DefaultRenderersFactory(context.applicationContext).setExtensionRendererMode(
            extensionRendererMode
        )
    }

    @JvmStatic
    @Synchronized
    fun getHttpDataSourceFactory(): DataSource.Factory {
        if (httpDataSourceFactory == null) {
            val cookieManager = CookieManager()
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
            CookieHandler.setDefault(cookieManager)
            httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
        }
        return httpDataSourceFactory!!
    }

    @JvmStatic
    @Synchronized
    fun getUpstreamDataSourceFactory(context: Context): DataSource.Factory {
        val upstreamFactory = DefaultDataSource.Factory(context, getHttpDataSourceFactory())
        dataSourceFactory = buildReadOnlyCacheDataSource(upstreamFactory, getDownloadCache(context))
        return dataSourceFactory!!
    }

    @JvmStatic
    @Synchronized
    fun getCacheDataSourceFactory(context: Context): DataSource.Factory {
        val streamCacheFactory = CacheDataSource.Factory()
            .setCache(getStreamingCache(context))
            .setUpstreamDataSourceFactory(getUpstreamDataSourceFactory(context))
        val resolvingFactory = ResolvingDataSource.Factory(
            StreamingCacheDataSource.Factory(streamCacheFactory)
        ) { dataSpec: DataSpec ->
            val builder = dataSpec.buildUpon()
            builder.setFlags(dataSpec.flags and DataSpec.FLAG_DONT_CACHE_IF_LENGTH_UNKNOWN.inv())
            builder.build()
        }
        dataSourceFactory =
            buildReadOnlyCacheDataSource(resolvingFactory, getDownloadCache(context))
        return dataSourceFactory!!
    }

    @JvmStatic
    @Synchronized
    fun getDownloadNotificationHelper(context: Context): DownloadNotificationHelper {
        if (downloadNotificationHelper == null) {
            downloadNotificationHelper =
                DownloadNotificationHelper(context, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
        }
        return downloadNotificationHelper!!
    }

    @JvmStatic
    @Synchronized
    fun getDownloadManager(context: Context): DownloadManager {
        ensureDownloadManagerInitialized(context)
        return downloadManager!!
    }

    @JvmStatic
    @Synchronized
    fun getDownloadTracker(context: Context): DownloaderManager {
        ensureDownloadManagerInitialized(context)
        return downloaderManager!!
    }

    @JvmStatic
    @Synchronized
    private fun getDownloadCache(context: Context): Cache {
        if (downloadCache == null) {
            val downloadContentDirectory =
                File(getDownloadDirectory(context), DOWNLOAD_CONTENT_DIRECTORY)
            downloadCache = SimpleCache(
                downloadContentDirectory,
                NoOpCacheEvictor(),
                getDatabaseProvider(context)
            )
        }
        return downloadCache!!
    }

    @JvmStatic
    @Synchronized
    private fun getStreamingCache(context: Context): SimpleCache {
        if (streamingCache == null) {
            val streamingCacheContentDirectory =
                File(getStreamingCacheDirectory(context), STREAMING_CACHE_CONTENT_DIRECTORY)
            streamingCache = SimpleCache(
                streamingCacheContentDirectory,
                LeastRecentlyUsedCacheEvictor(Preferences.getStreamingCacheSize() * 1024 * 1024),
                getDatabaseProvider(context)
            )
        }
        return streamingCache!!
    }

    @JvmStatic
    @Synchronized
    private fun ensureDownloadManagerInitialized(context: Context) {
        if (downloadManager == null) {
            downloadManager = DownloadManager(
                context,
                getDatabaseProvider(context),
                getDownloadCache(context),
                getHttpDataSourceFactory(),
                Executors.newFixedThreadPool(6)
            )
            if (Preferences.isDownloadWifiOnly()) {
                downloadManager!!.setRequirements(Requirements(Requirements.NETWORK_UNMETERED))
            }
            downloaderManager =
                DownloaderManager(context, getHttpDataSourceFactory(), downloadManager!!)
        }
    }

    @JvmStatic
    @Synchronized
    private fun getDatabaseProvider(context: Context): DatabaseProvider {
        if (databaseProvider == null) {
            databaseProvider = StandaloneDatabaseProvider(context)
        }
        return databaseProvider!!
    }

    @JvmStatic
    @Synchronized
    private fun getStreamingCacheDirectory(context: Context): File {
        if (streamingCacheDirectory == null) {
            if (Preferences.getStreamingCacheStoragePreference() == 0) {
                streamingCacheDirectory = context.getExternalFilesDirs(null)[0]
                if (streamingCacheDirectory == null) {
                    streamingCacheDirectory = context.filesDir
                }
            } else {
                try {
                    streamingCacheDirectory = context.getExternalFilesDirs(null)[1]
                } catch (exception: Exception) {
                    streamingCacheDirectory = context.getExternalFilesDirs(null)[0]
                    Preferences.setStreamingCacheStoragePreference(0)
                }
            }
        }
        return streamingCacheDirectory!!
    }

    @JvmStatic
    @Synchronized
    private fun getDownloadDirectory(context: Context): File {
        if (downloadDirectory == null) {
            val pref = Preferences.getDownloadStoragePreference()
            if (pref == 0) {
                downloadDirectory = context.getExternalFilesDirs(null)[0]
                if (downloadDirectory == null) {
                    downloadDirectory = context.filesDir
                }
            } else if (pref == 1) {
                try {
                    downloadDirectory = context.getExternalFilesDirs(null)[1]
                } catch (exception: Exception) {
                    downloadDirectory = context.getExternalFilesDirs(null)[0]
                    Preferences.setDownloadStoragePreference(0)
                }
            } else {
                downloadDirectory = context.getExternalFilesDirs(null)[0]
            }
        }
        return downloadDirectory!!
    }

    private fun buildReadOnlyCacheDataSource(
        upstreamFactory: DataSource.Factory,
        cache: Cache
    ): CacheDataSource.Factory {
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheWriteDataSinkFactory(null)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    @JvmStatic
    @Synchronized
    fun eraseDownloadFolder(context: Context) {
        val directory = getDownloadDirectory(context)
        val files = listFiles(directory, ArrayList())
        for (file in files) {
            file.delete()
        }
    }

    @JvmStatic
    @Synchronized
    private fun listFiles(directory: File, files: ArrayList<File>): ArrayList<File> {
        if (directory.isDirectory) {
            val list = directory.listFiles()
            if (list != null) {
                for (file in list) {
                    if (file.isFile && file.name.lowercase().endsWith(".exo")) {
                        files.add(file)
                    } else if (file.isDirectory) {
                        listFiles(file, files)
                    }
                }
            }
        }
        return files
    }

    @JvmStatic
    @Synchronized
    fun getStreamingCacheSize(context: Context): Long {
        return getStreamingCache(context).cacheSpace
    }

    @JvmStatic
    fun buildGroupSummaryNotification(
        context: Context,
        channelId: String,
        groupId: String,
        icon: Int,
        title: String
    ): Notification {
        return NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setSmallIcon(icon)
            .setGroup(groupId)
            .setGroupSummary(true)
            .build()
    }
}
