package com.cappielloantonio.tempo.util

import android.net.Uri
import android.os.Looper
import android.os.SystemClock
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.PodcastEpisode
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object ExternalAudioReader {
    private val cache: MutableMap<String, DocumentFile> = ConcurrentHashMap()
    private val LOCK = Any()
    private val REFRESH_EXECUTOR: ExecutorService = Executors.newSingleThreadExecutor()
    private val refreshEvents = MutableLiveData<Long>()

    @Volatile
    private var cachedDirUri: String? = null

    @Volatile
    private var refreshInProgress = false

    @Volatile
    private var refreshQueued = false

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

    private fun ensureCache() {
        val uriString = Preferences.getDownloadDirectoryUri()
        if (uriString == null) {
            synchronized(LOCK) {
                cache.clear()
                cachedDirUri = null
            }
            ExternalDownloadMetadataStore.clear()
            return
        }

        if (uriString == cachedDirUri) {
            return
        }

        var runSynchronously = false
        synchronized(LOCK) {
            if (refreshInProgress) {
                return
            }

            if (Looper.myLooper() == Looper.getMainLooper()) {
                scheduleRefreshLocked()
                return
            }

            refreshInProgress = true
            runSynchronously = true
        }

        if (runSynchronously) {
            try {
                rebuildCache()
            } finally {
                onRefreshFinished()
            }
        }
    }

    @JvmStatic
    fun refreshCache() {
        refreshCacheAsync()
    }

    @JvmStatic
    fun refreshCacheAsync() {
        synchronized(LOCK) {
            cachedDirUri = null
            cache.clear()
        }
        requestRefresh()
    }

    @JvmStatic
    fun getRefreshEvents(): LiveData<Long> {
        return refreshEvents
    }

    private fun buildKey(artist: String?, title: String?, album: String?): String {
        var name = if (artist != null && artist.isNotEmpty()) "$artist - $title" else title ?: ""
        if (album != null && album.isNotEmpty()) name += " ($album)"
        return normalizeForComparison(name)
    }

    private fun findUri(artist: String?, title: String?, album: String?): Uri? {
        ensureCache()
        if (cachedDirUri == null) return null

        val file = cache[buildKey(artist, title, album)]
        return if (file != null && file.exists()) file.uri else null
    }

    @JvmStatic
    fun getUri(media: Child): Uri? {
        return findUri(media.artist, media.title, media.album)
    }

    @JvmStatic
    fun getUri(episode: PodcastEpisode): Uri? {
        return findUri(episode.artist, episode.title, episode.album)
    }

    @JvmStatic
    @Synchronized
    fun removeMetadata(media: Child?) {
        if (media == null) {
            return
        }

        val key = buildKey(media.artist, media.title, media.album)
        cache.remove(key)
        ExternalDownloadMetadataStore.remove(key)
    }

    @JvmStatic
    fun delete(media: Child): Boolean {
        ensureCache()
        if (cachedDirUri == null) return false

        val key = buildKey(media.artist, media.title, media.album)
        val file = cache[key]
        var deleted = false
        if (file != null && file.exists()) {
            deleted = file.delete()
        }
        if (deleted) {
            cache.remove(key)
            ExternalDownloadMetadataStore.remove(key)
        }
        return deleted
    }

    private fun requestRefresh() {
        synchronized(LOCK) {
            scheduleRefreshLocked()
        }
    }

    private fun scheduleRefreshLocked() {
        if (refreshInProgress) {
            refreshQueued = true
            return
        }

        refreshInProgress = true
        REFRESH_EXECUTOR.execute {
            try {
                rebuildCache()
            } finally {
                onRefreshFinished()
            }
        }
    }

    private fun rebuildCache() {
        val uriString = Preferences.getDownloadDirectoryUri()
        if (uriString == null) {
            synchronized(LOCK) {
                cache.clear()
                cachedDirUri = null
            }
            ExternalDownloadMetadataStore.clear()
            return
        }

        val directory = DocumentFile.fromTreeUri(App.getContext(), Uri.parse(uriString))
        val expectedSizes = ExternalDownloadMetadataStore.snapshot()
        val verifiedKeys: MutableSet<String> = HashSet()
        val newEntries: MutableMap<String, DocumentFile> = HashMap()

        if (directory != null && directory.canRead()) {
            for (file in directory.listFiles()) {
                if (file == null || file.isDirectory) continue
                val existing = file.name ?: continue

                val base = existing.replaceFirst("\\.[^\\.]+$".toRegex(), "")
                val key = normalizeForComparison(base)
                val expected = expectedSizes[key]
                val actualLength = file.length()

                if (expected != null && expected > 0 && actualLength == expected) {
                    newEntries[key] = file
                    verifiedKeys.add(key)
                } else {
                    ExternalDownloadMetadataStore.remove(key)
                }
            }
        }

        if (expectedSizes.isNotEmpty()) {
            if (verifiedKeys.isEmpty()) {
                ExternalDownloadMetadataStore.clear()
            } else {
                for (key in expectedSizes.keys) {
                    if (!verifiedKeys.contains(key)) {
                        ExternalDownloadMetadataStore.remove(key)
                    }
                }
            }
        }

        synchronized(LOCK) {
            cache.clear()
            cache.putAll(newEntries)
            cachedDirUri = uriString
        }
    }

    private fun onRefreshFinished() {
        var runAgain: Boolean
        synchronized(LOCK) {
            refreshInProgress = false
            runAgain = refreshQueued
            refreshQueued = false
        }

        refreshEvents.postValue(SystemClock.elapsedRealtime())

        if (runAgain) {
            requestRefresh()
        }
    }
}
