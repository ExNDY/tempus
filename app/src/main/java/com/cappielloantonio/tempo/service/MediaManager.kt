package com.cappielloantonio.tempo.service

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.Observer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import com.cappielloantonio.tempo.interfaces.MediaIndexCallback
import com.cappielloantonio.tempo.model.Chronology
import com.cappielloantonio.tempo.repository.ChronologyRepository
import com.cappielloantonio.tempo.repository.QueueRepository
import com.cappielloantonio.tempo.repository.SongRepository
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.InternetRadioStation
import com.cappielloantonio.tempo.subsonic.models.PodcastEpisode
import com.cappielloantonio.tempo.util.MappingUtil
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.viewmodel.PlaybackViewModel
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.lang.ref.WeakReference
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors

object MediaManager {
    private const val TAG = "MediaManager"
    private var attachedBrowserRef = WeakReference<MediaBrowser?>(null)
    @JvmField
    val justStarted = AtomicBoolean(false)
    @JvmField
    val continuousPlayIsRunning = AtomicBoolean(false)

    private val backgroundExecutor = Executors.newSingleThreadExecutor()

    @JvmStatic
    fun registerPlaybackObserver(
        browserFuture: ListenableFuture<MediaBrowser>?,
        playbackViewModel: PlaybackViewModel
    ) {
        if (browserFuture == null) return

        Futures.addCallback(browserFuture, object : FutureCallback<MediaBrowser> {
            override fun onSuccess(browser: MediaBrowser?) {
                if (browser == null) return
                val current = attachedBrowserRef.get()
                if (current != browser) {
                    browser.addListener(object : Player.Listener {
                        override fun onEvents(player: Player, events: Player.Events) {
                            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)
                                || events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED)
                                || events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED)
                            ) {

                                val mediaId = player.currentMediaItem?.mediaId
                                val playing = player.playbackState == Player.STATE_READY
                                        && player.playWhenReady

                                playbackViewModel.update(mediaId, playing)
                            }
                        }
                    })

                    val mediaId = browser.currentMediaItem?.mediaId
                    val playing = browser.playbackState == Player.STATE_READY && browser.playWhenReady
                    playbackViewModel.update(mediaId, playing)

                    attachedBrowserRef = WeakReference(browser)
                } else {
                    val mediaId = browser.currentMediaItem?.mediaId
                    val playing = browser.playbackState == Player.STATE_READY && browser.playWhenReady
                    playbackViewModel.update(mediaId, playing)
                }
            }

            override fun onFailure(t: Throwable) {
                Log.e(TAG, "Failed to get MediaBrowser instance", t)
            }
        }, MoreExecutors.directExecutor())
    }

    @JvmStatic
    fun onBrowserReleased(released: MediaBrowser?) {
        val attached = attachedBrowserRef.get()
        if (attached == released) {
            attachedBrowserRef.clear()
        }
    }

    @JvmStatic
    fun reset(mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?) {
        mediaBrowserListenableFuture?.addListener({
            val browser = getResolvedBrowser(mediaBrowserListenableFuture, "reset")
            if (browser != null) {
                if (browser.isPlaying) {
                    browser.pause()
                }

                browser.stop()
                browser.clearMediaItems()
                clearDatabase()
            }
        }, MoreExecutors.directExecutor())
    }

    @JvmStatic
    fun hide(mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?) {
        mediaBrowserListenableFuture?.addListener({
            val browser = getResolvedBrowser(mediaBrowserListenableFuture, "hide")
            if (browser != null && browser.isPlaying) {
                browser.pause()
            }
        }, MoreExecutors.directExecutor())
    }

    @JvmStatic
    fun check(mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?) {
        mediaBrowserListenableFuture?.addListener({
            val browser = getResolvedBrowser(mediaBrowserListenableFuture, "check")
            if (browser != null && browser.mediaItemCount < 1) {
                val media = queueRepository.media
                if (media != null && media.size >= 1) {
                    init(mediaBrowserListenableFuture, media)
                }
            }
        }, MoreExecutors.directExecutor())
    }

    @JvmStatic
    fun init(mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?, media: List<Child>) {
        mediaBrowserListenableFuture?.addListener({
            val browser = getResolvedBrowser(mediaBrowserListenableFuture, "init")
            if (browser != null) {
                browser.clearMediaItems()
                browser.setMediaItems(MappingUtil.mapMediaItems(media))
                browser.seekTo(queueRepository.lastPlayedMediaIndex, queueRepository.lastPlayedMediaTimestamp)
                browser.prepare()
            }
        }, MoreExecutors.directExecutor())
    }

    @JvmStatic
    @OptIn(UnstableApi::class)
    fun startQueue(mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?, media: List<Child>, startIndex: Int) {
        mediaBrowserListenableFuture?.addListener({
            try {
                if (mediaBrowserListenableFuture.isDone) {
                    val browser = mediaBrowserListenableFuture.get()
                    val items = MappingUtil.mapMediaItems(media)

                    Handler(Looper.getMainLooper()).post {
                        justStarted.set(true)
                        browser.setMediaItems(items, startIndex, 0)
                        browser.prepare()

                        val timelineListener = object : Player.Listener {
                            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                                val itemCount = browser.mediaItemCount
                                if (itemCount > 0 && startIndex >= 0 && startIndex < itemCount) {
                                    browser.seekTo(startIndex, 0)
                                    browser.play()
                                    browser.removeListener(this)
                                } else {
                                    Log.d(TAG, "Cannot start playback: itemCount=$itemCount, startIndex=$startIndex")
                                }
                            }
                        }

                        browser.addListener(timelineListener)
                    }

                    backgroundExecutor.execute {
                        Log.d(TAG, "Background: enqueuing to database")
                        enqueueDatabase(media, true, 0)
                    }
                }
            } catch (e: ExecutionException) {
                Log.e(TAG, "Error in startQueue: " + e.message, e)
            } catch (e: InterruptedException) {
                Log.e(TAG, "Error in startQueue: " + e.message, e)
            }
        }, MoreExecutors.directExecutor())
    }

    @JvmStatic
    fun startQueue(mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?, media: Child) {
        mediaBrowserListenableFuture?.addListener({
            try {
                if (mediaBrowserListenableFuture.isDone) {
                    val browser = mediaBrowserListenableFuture.get()
                    justStarted.set(true)
                    browser.setMediaItem(MappingUtil.mapMediaItem(media))
                    browser.prepare()
                    browser.play()
                    enqueueDatabase(media, true, 0)
                }
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    @JvmStatic
    fun playDownloadedMediaItem(mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?, mediaItem: MediaItem?) {
        if (mediaBrowserListenableFuture != null && mediaItem != null) {
            mediaBrowserListenableFuture.addListener({
                try {
                    if (mediaBrowserListenableFuture.isDone) {
                        val mediaBrowser = mediaBrowserListenableFuture.get()
                        justStarted.set(true)
                        mediaBrowser.setMediaItem(mediaItem)
                        mediaBrowser.prepare()
                        mediaBrowser.play()
                        clearDatabase()
                    }
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }, MoreExecutors.directExecutor())
        }
    }

    @JvmStatic
    fun startRadio(mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?, internetRadioStation: InternetRadioStation) {
        mediaBrowserListenableFuture?.addListener({
            try {
                if (mediaBrowserListenableFuture.isDone) {
                    val browser = mediaBrowserListenableFuture.get()
                    justStarted.set(true)
                    browser.setMediaItem(MappingUtil.mapInternetRadioStation(internetRadioStation))
                    browser.prepare()
                    browser.play()
                }
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    @JvmStatic
    fun startPodcast(mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?, podcastEpisode: PodcastEpisode) {
        mediaBrowserListenableFuture?.addListener({
            try {
                if (mediaBrowserListenableFuture.isDone) {
                    val browser = mediaBrowserListenableFuture.get()
                    justStarted.set(true)
                    browser.setMediaItem(MappingUtil.mapMediaItem(podcastEpisode))
                    browser.prepare()
                    browser.play()
                }
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    @JvmStatic
    fun enqueue(mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?, media: List<Child>, playImmediatelyAfter: Boolean) {
        mediaBrowserListenableFuture?.addListener({
            try {
                if (mediaBrowserListenableFuture.isDone) {
                    Log.d(TAG, "enqueue")
                    val browser = mediaBrowserListenableFuture.get()
                    if (playImmediatelyAfter && browser.nextMediaItemIndex != -1) {
                        enqueueDatabase(media, false, browser.nextMediaItemIndex)
                        browser.addMediaItems(browser.nextMediaItemIndex, MappingUtil.mapMediaItems(media))
                    } else {
                        enqueueDatabase(media, false, mediaBrowserListenableFuture.get().mediaItemCount)
                        mediaBrowserListenableFuture.get().addMediaItems(MappingUtil.mapMediaItems(media))
                    }
                }
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    @JvmStatic
    fun enqueue(mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?, media: Child, playImmediatelyAfter: Boolean) {
        mediaBrowserListenableFuture?.addListener({
            try {
                if (mediaBrowserListenableFuture.isDone) {
                    Log.e(TAG, "enqueue")
                    val browser = mediaBrowserListenableFuture.get()
                    if (playImmediatelyAfter && browser.nextMediaItemIndex != -1) {
                        enqueueDatabase(media, false, browser.nextMediaItemIndex)
                        browser.addMediaItem(browser.nextMediaItemIndex, MappingUtil.mapMediaItem(media))
                    } else {
                        enqueueDatabase(media, false, mediaBrowserListenableFuture.get().mediaItemCount)
                        mediaBrowserListenableFuture.get().addMediaItem(MappingUtil.mapMediaItem(media))
                    }
                }
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    @JvmStatic
    fun shuffle(mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?, media: List<Child>, startIndex: Int, endIndex: Int) {
        mediaBrowserListenableFuture?.addListener({
            try {
                if (mediaBrowserListenableFuture.isDone) {
                    Log.e(TAG, "shuffle")
                    val browser = mediaBrowserListenableFuture.get()
                    browser.removeMediaItems(startIndex, endIndex + 1)
                    browser.addMediaItems(MappingUtil.mapMediaItems(media).subList(startIndex, endIndex + 1))
                    swapDatabase(media)
                }
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    @JvmStatic
    fun swap(mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?, media: List<Child>, from: Int, to: Int) {
        mediaBrowserListenableFuture?.addListener({
            try {
                if (mediaBrowserListenableFuture.isDone) {
                    Log.e(TAG, "swap")
                    mediaBrowserListenableFuture.get().moveMediaItem(from, to)
                    swapDatabase(media)
                }
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    @JvmStatic
    fun remove(mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?, media: MutableList<Child>, toRemove: Int) {
        mediaBrowserListenableFuture?.addListener({
            try {
                if (mediaBrowserListenableFuture.isDone) {
                    Log.e(TAG, "remove")
                    if (mediaBrowserListenableFuture.get().mediaItemCount > 1 && mediaBrowserListenableFuture.get().currentMediaItemIndex != toRemove) {
                        mediaBrowserListenableFuture.get().removeMediaItem(toRemove)
                        removeDatabase(media, toRemove)
                    } else {
                        removeDatabase(media, -1)
                    }
                }
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    @JvmStatic
    fun removeRange(mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?, media: MutableList<Child>, fromItem: Int, toItem: Int) {
        mediaBrowserListenableFuture?.addListener({
            try {
                if (mediaBrowserListenableFuture.isDone) {
                    Log.e(TAG, "remove range")
                    mediaBrowserListenableFuture.get().removeMediaItems(fromItem, toItem)
                    removeRangeDatabase(media, fromItem, toItem)
                }
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    @JvmStatic
    fun removeRange(mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?, fromItem: Int, toItem: Int) {
        mediaBrowserListenableFuture?.addListener({
            try {
                if (mediaBrowserListenableFuture.isDone) {
                    mediaBrowserListenableFuture.get().removeMediaItems(fromItem, toItem)
                    queueRepository.deleteRange(fromItem, toItem)
                }
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    @JvmStatic
    fun getCurrentIndex(mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?, callback: MediaIndexCallback) {
        mediaBrowserListenableFuture?.addListener({
            val browser = getResolvedBrowser(mediaBrowserListenableFuture, "getCurrentIndex")
            if (browser != null) {
                callback.onRecovery(browser.currentMediaItemIndex)
            }
        }, MoreExecutors.directExecutor())
    }

    @JvmStatic
    fun setLastPlayedTimestamp(mediaItem: MediaItem?) {
        if (mediaItem != null) queueRepository.setLastPlayedTimestamp(mediaItem.mediaId)
    }

    @JvmStatic
    fun setPlayingPausedTimestamp(mediaItem: MediaItem?, ms: Long) {
        if (mediaItem != null)
            queueRepository.setPlayingPausedTimestamp(mediaItem.mediaId, ms)
    }

    @JvmStatic
    fun scrobble(mediaItem: MediaItem?, submission: Boolean) {
        if (mediaItem != null && mediaItem.mediaMetadata.extras != null && Preferences.isScrobblingEnabled()) {
            songRepository.scrobble(mediaItem.mediaMetadata.extras!!.getString("id"), submission)
        }
    }

    @JvmStatic
    @OptIn(UnstableApi::class)
    fun continuousPlay(
        mediaItem: MediaItem,
        existingBrowserFuture: ListenableFuture<MediaBrowser>?
    ) {
        continuousPlay(mediaItem, existingBrowserFuture, null)
    }

    @JvmStatic
    @OptIn(UnstableApi::class)
    fun continuousPlay(
        mediaItem: MediaItem,
        existingBrowserFuture: ListenableFuture<MediaBrowser>?,
        onComplete: Runnable?
    ) {
        if (continuousPlayIsRunning.get() || !Preferences.isInstantMixUsable()) {
            Log.d(TAG, "Continuous Play: already running")
            onComplete?.run()
            return
        }
        Log.d(TAG, "Continuous Play")

        Preferences.setLastInstantMix()
        continuousPlayIsRunning.set(true)

        val instantMix = songRepository.getContinuousMix(mediaItem.mediaId, 25)

        instantMix.observeForever(object : Observer<List<Child>> {
            override fun onChanged(value: List<Child>) {
                if (value.isEmpty()) {
                    Log.w(TAG, "Continuous Play: no similar track found. Is server correctly configured?")
                } else {
                    if (existingBrowserFuture != null) {
                        Log.d(TAG, "Continuous Play: found " + value.size + " similar tracks")

                        val browser: MediaBrowser
                        try {
                            browser = existingBrowserFuture.get()
                        } catch (e: ExecutionException) {
                            Log.e(TAG, "Continuous Play: browser unavailable", e)
                            instantMix.removeObserver(this)
                            continuousPlayIsRunning.set(false)
                            return
                        } catch (e: InterruptedException) {
                            Log.e(TAG, "Continuous Play: browser unavailable", e)
                            instantMix.removeObserver(this)
                            continuousPlayIsRunning.set(false)
                            return
                        }

                        val currentIds = ArrayList<String>()
                        for (i in 0 until browser.mediaItemCount) {
                            currentIds.add(browser.getMediaItemAt(i).mediaId)
                        }
                        val filteredMedia = value.stream()
                            .filter { child -> !currentIds.contains(child.id) }
                            .collect(Collectors.toList())

                        Log.d(TAG, "Continuous Play: adding " + filteredMedia.size + " tracks to queue")
                        enqueue(existingBrowserFuture, filteredMedia, true)
                    }
                }
                instantMix.removeObserver(this)
                continuousPlayIsRunning.set(false)
                onComplete?.run()
            }
        })
    }

    @JvmStatic
    fun saveChronology(mediaItem: MediaItem?) {
        if (mediaItem != null) {
            chronologyRepository.insert(Chronology(mediaItem))
        }
    }

    private val queueRepository: QueueRepository
        get() = QueueRepository()

    private val songRepository: SongRepository
        get() = SongRepository()

    private val chronologyRepository: ChronologyRepository
        get() = ChronologyRepository()

    private fun enqueueDatabase(media: List<Child>, reset: Boolean, afterIndex: Int) {
        queueRepository.insertAll(media, reset, afterIndex)
    }

    private fun enqueueDatabase(media: Child, reset: Boolean, afterIndex: Int) {
        queueRepository.insert(media, reset, afterIndex)
    }

    private fun swapDatabase(media: List<Child>) {
        queueRepository.insertAll(media, true, 0)
    }

    private fun removeDatabase(media: MutableList<Child>, toRemove: Int) {
        if (toRemove != -1) {
            media.removeAt(toRemove)
            queueRepository.insertAll(media, true, 0)
        }
    }

    private fun removeRangeDatabase(media: MutableList<Child>, fromItem: Int, toItem: Int) {
        val toRemove = media.subList(fromItem, toItem)
        media.removeAll(toRemove)
        queueRepository.insertAll(media, true, 0)
    }

    @JvmStatic
    fun clearDatabase() {
        queueRepository.deleteAll()
    }

    private fun getResolvedBrowser(
        mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?,
        action: String
    ): MediaBrowser? {
        if (mediaBrowserListenableFuture == null || !mediaBrowserListenableFuture.isDone) {
            return null
        }
        if (mediaBrowserListenableFuture.isCancelled) {
            Log.d(TAG, "$action: MediaBrowser future was cancelled")
            return null
        }

        return try {
            mediaBrowserListenableFuture.get()
        } catch (e: CancellationException) {
            Log.d(TAG, "$action: MediaBrowser future was cancelled", e)
            null
        } catch (e: ExecutionException) {
            Log.e(TAG, "$action: Failed to resolve MediaBrowser future", e)
            null
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            Log.e(TAG, "$action: Interrupted while resolving MediaBrowser future", e)
            null
        }
    }
}
