package com.cappielloantonio.tempo.service
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.TaskStackBuilder
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.*
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.session.*
import androidx.media3.session.MediaSession.ControllerInfo
import com.cappielloantonio.tempo.equalizer.BuiltinBackend
import com.cappielloantonio.tempo.equalizer.EqualizerBackend
import com.cappielloantonio.tempo.equalizer.EqualizerManager
import com.cappielloantonio.tempo.equalizer.ExternalBackend
import com.cappielloantonio.tempo.equalizer.DefaultBackend
import com.cappielloantonio.tempo.repository.QueueRepository
import com.cappielloantonio.tempo.playback.PlaybackDiagnosticsStore
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.util.*
import com.cappielloantonio.tempo.util.SleepTimerManager
import com.cappielloantonio.tempo.widget.WidgetUpdateManager
import java.util.concurrent.TimeUnit
import kotlin.random.Random
private const val TAG = "BaseMediaService"
open class BaseMediaService : MediaLibraryService() {
    companion object {
        const val ACTION_BIND_EQUALIZER = "com.cappielloantonio.tempo.service.BIND_EQUALIZER"
        const val ACTION_EQUALIZER_UPDATED = "com.cappielloantonio.tempo.service.EQUALIZER_UPDATED"
        const val ACTION_RELOAD_EQUALIZER = "com.cappielloantonio.tempo.service.ACTION_RELOAD_EQUALIZER"
    }
    protected lateinit var exoplayer: ExoPlayer
    protected lateinit var mediaLibrarySession: MediaLibrarySession
    protected var sessionCallback: MediaLibrarySession.Callback? = null
    private lateinit var bitmapLoader: SyncBitmapLoader
    private lateinit var networkCallback: CustomNetworkCallback
    private lateinit var equalizerManager: EqualizerManager
    private lateinit var playbackStatsListener: PlaybackStatsListener
    private val playbackDiagnosticsStore by lazy { PlaybackDiagnosticsStore(this) }
    private val widgetUpdateHandler = Handler(Looper.getMainLooper())
    private val queueRepository by lazy { QueueRepository() }
    private var playbackTimelineRevision = 0L
    private var serviceDestroyed = false
    private var widgetUpdateScheduled = false
    private val widgetUpdateRunnable = object : Runnable {
        override fun run() {
            val player = mediaLibrarySession.player
            if (!player.isPlaying) {
                widgetUpdateScheduled = false
                return
            }
            updateWidget(player)
            widgetUpdateHandler.postDelayed(this, WIDGET_UPDATE_INTERVAL_MS)
        }
    }
    private val binder = LocalBinder()
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RELOAD_EQUALIZER -> reloadEqualizer()
        }
        return super.onStartCommand(intent, flags, startId)
    }
    open fun playerInitHook() {
        initializeExoPlayer()
        initializeMediaLibrarySession(exoplayer)
        initializePlayerListener(exoplayer)
        initializeSleepTimer()
        setPlayer(null, exoplayer)
    }
    open fun getMediaLibrarySessionCallback(): MediaLibrarySession.Callback {
        return BaseSessionCallback(baseContext, this)
    }
    fun updateMediaItems(player: Player) {
        Log.d(TAG, "update items")
        val n = player.mediaItemCount
        val k = player.currentMediaItemIndex
        val current = player.currentPosition
        val items = (0..n - 1).map { MappingUtil.mapMediaItem(player.getMediaItemAt(it)) }
        player.clearMediaItems()
        player.setMediaItems(items, k, current)
    }
    fun restorePlayerFromQueue(player: Player) {
        if (player.mediaItemCount > 0) return
        val restoreRevision = playbackTimelineRevision
        queueRepository.loadRestoreSnapshot { snapshot ->
            val mediaItems = MappingUtil.mapMediaItems(snapshot.media)
            widgetUpdateHandler.post {
                if (!shouldApplyQueueRestore(
                        restoreRevision = restoreRevision,
                        currentRevision = playbackTimelineRevision,
                        serviceDestroyed = serviceDestroyed,
                        currentMediaItemCount = player.mediaItemCount,
                    ) || mediaItems.isEmpty()
                ) {
                    return@post
                }
                val restoreTarget = normalizeQueueRestoreTarget(
                    lastIndex = snapshot.lastIndex,
                    lastPosition = snapshot.lastPosition,
                    mediaItemCount = mediaItems.size,
                ) ?: return@post
                player.setMediaItems(mediaItems, restoreTarget.index, restoreTarget.positionMs)
                player.prepare()
                updateWidget(player)
            }
        }
    }
    fun initializePlayerListener(player: Player) {
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                Log.d(TAG, "onMediaItemTransition" + player.currentMediaItemIndex)
                if (mediaItem == null) return
                ReplayGainUtil.applyGain(player, mediaItem)
                // --- Add for AA : Constants.AA_START_INDEX if présent ---
                val extras = mediaItem.mediaMetadata.extras
                val startIndex = extras?.getInt(Constants.AA_START_INDEX, -1) ?: -1
                if (startIndex >= 0 ) {
                    val cleanExtras = Bundle(extras).apply {
                        remove(Constants.AA_START_INDEX)
                    }
                    val newMetadata = mediaItem.mediaMetadata.buildUpon()
                        .setExtras(cleanExtras)
                        .build()
                    val currentIdx = player.currentMediaItemIndex
                    if (player is ExoPlayer && currentIdx != C.INDEX_UNSET) {
                        player.replaceMediaItem(
                            currentIdx,
                            mediaItem.buildUpon().setMediaMetadata(newMetadata).build()
                        )
                    }
                    if (startIndex in 0 until player.mediaItemCount && startIndex != currentIdx) {
                        player.seekTo(startIndex, 0L)
                    }
                }
                // --- End add for AA ---
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK || reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    MediaManager.setLastPlayedTimestamp(mediaItem)
                }
                // Safety net: if a track transition fires while end-of-track is armed
                // (e.g. stream with unknown duration that ended before the poller could
                // trigger the fade), abort any in-progress fade and pause immediately.
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
                    SleepTimerManager.getInstance().isEndOfTrack()) {
                    SleepTimerManager.getInstance().stopEndOfTrackPoller()
                    SleepTimerManager.getInstance().cancelTimer()
                    player.volume = 1f
                    player.pause()
                }
                updateWidget(player)
            }
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                playbackTimelineRevision++
                Log.d(TAG, "onTimelineChanged reason=$reason")
                try {
                    ReplayGainUtil.prefetchQueueGains(player)
                } catch (t: Throwable) {
                    Log.w(TAG, "prefetchQueueGains failed: $t")
                }
                if (timeline.isEmpty) return
                val window = Timeline.Window()
                for (i in 0 until timeline.windowCount) {
                    timeline.getWindow(i, window)
                    window.mediaItem.mediaMetadata.artworkUri?.let { bitmapLoader.prewarm(it) }
                }
            }
            override fun onTracksChanged(tracks: Tracks) {
                Log.d(TAG, "onTracksChanged: " + player.currentMediaItemIndex)
                ReplayGainUtil.setReplayGain(player, tracks)
                val currentMediaItem = player.currentMediaItem
                if (currentMediaItem != null) {
                    val item = MappingUtil.mapMediaItem(currentMediaItem)
                    if (item.mediaMetadata.extras != null)
                        MediaManager.scrobble(item, false)
                    val browserFuture = MediaBrowser.Builder(
                        this@BaseMediaService,
                        SessionToken(this@BaseMediaService, ComponentName(this@BaseMediaService, this@BaseMediaService::class.java))
                    ).buildAsync()
                    val handled = MediaServiceExtensionRegistry.handler
                        ?.handle(player, currentMediaItem, browserFuture)
                        ?: false
                    if (player.nextMediaItemIndex == C.INDEX_UNSET) {
                        if (!handled && Preferences.isContinuousPlayEnabled()) {
                            MediaManager.continuousPlay(currentMediaItem, browserFuture)
                        }
                    }
                }
                if (player is ExoPlayer) {
                    // https://stackoverflow.com/questions/56937283/exoplayer-shuffle-doesnt-reproduce-all-the-songs
                    if (MediaManager.justStarted.get()) {
                        Log.d(TAG, "update shuffle order")
                        MediaManager.justStarted.set(false)
                        val shuffledList = IntArray(player.mediaItemCount) { i -> i }
                        shuffledList.shuffle()
                        val index = shuffledList.indexOf(player.currentMediaItemIndex)
                        // swap current media index to the first index
                        if (index > -1 && shuffledList.isNotEmpty()) {
                            val tmp = shuffledList[0]
                            shuffledList[0] = shuffledList[index]
                            shuffledList[index] = tmp
                        }
                        player.shuffleOrder =
                            DefaultShuffleOrder(shuffledList, Random.nextLong())
                    }
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d(TAG, "onIsPlayingChanged " + player.currentMediaItemIndex)
                if (isPlaying) {
                    val currentItem = player.currentMediaItem
                    if (currentItem != null) {
                        MediaManager.scrobble(currentItem, false)
                    }
                } else {
                    val currentItem = player.currentMediaItem
                    if (currentItem != null) {
                        MediaManager.setPlayingPausedTimestamp(
                            currentItem,
                            player.currentPosition
                        )
                    }
                }
                if (isPlaying) {
                    scheduleWidgetUpdates()
                } else {
                    stopWidgetUpdates()
                }
                updateWidget(player)
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d(TAG, "onPlaybackStateChanged")
                super.onPlaybackStateChanged(playbackState)
                if (!player.hasNextMediaItem() &&
                    playbackState == Player.STATE_ENDED &&
                    player.mediaMetadata.extras?.getString("type") == Constants.MEDIA_TYPE_MUSIC
                ) {
                    val currentItem = player.currentMediaItem
                    if (currentItem != null) {
                        MediaManager.scrobble(currentItem, true)
                        MediaManager.saveChronology(currentItem)
                    }
                }
                updateWidget(player)
            }
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                Log.d(TAG, "onPositionDiscontinuity reason=$reason old=${oldPosition.mediaItemIndex} new=${newPosition.mediaItemIndex}")
                super.onPositionDiscontinuity(oldPosition, newPosition, reason)
                // Re-apply gain whenever we stay on the same track for any reason
                // except an automatic transition to the next track.
                if (reason != Player.DISCONTINUITY_REASON_AUTO_TRANSITION &&
                    oldPosition.mediaItemIndex == newPosition.mediaItemIndex) {
                    // Clear pending gain immediately (main thread) before reapplying.
                    // This pre-empts the same-format gapless promotion in onFlush: if
                    // the decoder ran ahead (endOfStreamPending=true) before the seek,
                    // hasPendingFlushGain being false when onFlush fires ensures we
                    // restore to the correct current-track baseline instead of applying
                    // the next track's gain mid-track.
                    ReplayGainUtil.getAudioProcessor().clearPendingGain()
                    ReplayGainUtil.reapplyCurrentTrackGain(player)
                }
                if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                    val oldMediaItem = oldPosition.mediaItem
                    if (oldMediaItem != null && oldMediaItem.mediaMetadata.extras?.getString("type") == Constants.MEDIA_TYPE_MUSIC) {
                        MediaManager.scrobble(oldMediaItem, true)
                        MediaManager.saveChronology(oldMediaItem)
                    }
                    val newMediaItem = newPosition.mediaItem
                    if (newMediaItem != null && newMediaItem.mediaMetadata.extras?.getString("type") == Constants.MEDIA_TYPE_MUSIC) {
                        MediaManager.setLastPlayedTimestamp(newMediaItem)
                    }
                }
            }
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                Preferences.setShuffleModeEnabled(shuffleModeEnabled)
            }
            override fun onRepeatModeChanged(repeatMode: Int) {
                Preferences.setRepeatMode(repeatMode)
            }
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                Log.d(TAG, "onAudioSessionIdChanged")
                equalizerManager.attach(audioSessionId)
                sendBroadcast(Intent(ACTION_EQUALIZER_UPDATED))
            }
        })
        if (player.isPlaying) {
            scheduleWidgetUpdates()
        }
    }
    // -------------------------------------------------------------------------
    // Sleep timer
    // -------------------------------------------------------------------------
    /**
     * Registers a [SleepTimerManager.ServiceActionListener] on the singleton so
     * that fade-out and pause happen in the service regardless of whether the UI
     * is attached. Call once after the player is ready.
     */
    private fun initializeSleepTimer() {
        SleepTimerManager.getInstance().setServiceActionListener(object : SleepTimerManager.ServiceActionListener {
            override fun onTick(expired: Boolean) {
                if (expired) SleepTimerManager.getInstance().startFadeOutThenPause(mediaLibrarySession.player)
            }
            override fun onEndOfTrackArmed() {
                SleepTimerManager.getInstance().armEndOfTrackFadePoller(mediaLibrarySession.player)
            }
        })
        // If end-of-track was already armed when the service restarted (state
        // restored from SharedPreferences), re-arm the poller against the live player.
        if (SleepTimerManager.getInstance().isActive() &&
                SleepTimerManager.getInstance().isEndOfTrack()) {
            SleepTimerManager.getInstance().armEndOfTrackFadePoller(mediaLibrarySession.player)
        }
    }
    open fun onInstantMix(session: MediaSession, onComplete: Runnable? = null) {
        val player = session.player
        val currentMediaItem = player.currentMediaItem
        val currentIndex = player.currentMediaItemIndex
        val lastIndex = player.mediaItemCount - 1
        val browserFuture = MediaBrowser.Builder(
            this@BaseMediaService,
            SessionToken(this@BaseMediaService, ComponentName(this@BaseMediaService, this@BaseMediaService::class.java))
        ).buildAsync()
        if (currentIndex in 0 until lastIndex) {
            Log.d(TAG, "onInstantMix: remove range from $currentIndex to $lastIndex")
            MediaManager.removeRange(browserFuture, currentIndex + 1, lastIndex + 1)
        }
        if (currentMediaItem != null) {
            Log.d(TAG, "onInstantMix: start Continuous Play with $currentMediaItem")
            MediaManager.continuousPlay(currentMediaItem, browserFuture) {
                Handler(Looper.getMainLooper()).post { onComplete?.run() }
            }
        }
    }
    fun setPlayer(oldPlayer: Player?, newPlayer: Player) {
        if (oldPlayer === newPlayer) return
        if (oldPlayer != null) {
            val currentQueue = getQueueFromPlayer(oldPlayer)
            val currentIndex = oldPlayer.currentMediaItemIndex
            val currentPosition = oldPlayer.currentPosition
            val isPlaying = oldPlayer.playWhenReady
            oldPlayer.stop()
            newPlayer.setMediaItems(currentQueue, currentIndex, currentPosition)
            newPlayer.playWhenReady = isPlaying
            newPlayer.prepare()
        }
        mediaLibrarySession.player = newPlayer
        (sessionCallback as? BaseSessionCallback)?.handlePlayerChanged(oldPlayer, newPlayer)
    }
    open fun releasePlayers() {
        exoplayer.release()
    }
    fun getQueueFromPlayer(player: Player): List<MediaItem> {
        return (0..player.mediaItemCount - 1).map(player::getMediaItemAt)
    }
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaLibrarySession.player
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }
    override fun onCreate() {
        super.onCreate()
        playerInitHook()
        initializeEqualizer()
        initializeNetworkListener()
        restorePlayerFromQueue(mediaLibrarySession.player)
    }
    override fun onGetSession(controllerInfo: ControllerInfo): MediaLibrarySession {
        return mediaLibrarySession
    }
    override fun onDestroy() {
        serviceDestroyed = true
        releaseNetworkCallback()
        equalizerManager.release(exoplayer.audioSessionId)
        ReplayGainUtil.release()
        stopWidgetUpdates()
        SleepTimerManager.getInstance().stopEndOfTrackPoller()
        SleepTimerManager.getInstance().setServiceActionListener(null)
        if (::bitmapLoader.isInitialized) bitmapLoader.shutdown()
        releasePlayers()
        mediaLibrarySession.release()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? {
        // Check if the intent is for our custom equalizer binder
        if (intent?.action == ACTION_BIND_EQUALIZER) {
            return binder
        }
        // Otherwise, handle it as a normal MediaLibraryService connection
        return super.onBind(intent)
    }
    private fun initializeExoPlayer() {
        exoplayer = ExoPlayer.Builder(this)
            .setRenderersFactory(getRenderersFactory())
            .setMediaSourceFactory(getMediaSourceFactory())
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setLoadControl(initializeLoadControl())
            .build()
        playbackStatsListener = PlaybackStatsListener(
            false,
            PlaybackStatsListener.Callback { _, playbackStats ->
                playbackDiagnosticsStore.record(playbackStats)
            },
        )
        exoplayer.addAnalyticsListener(playbackStatsListener)
        exoplayer.shuffleModeEnabled = Preferences.isShuffleModeEnabled()
        exoplayer.repeatMode = Preferences.getRepeatMode()
    }
    private fun initializeEqualizer() {
        val equalizerBackend: EqualizerBackend =
            when (Preferences.getSelectedEqualizer()) {
            1 -> BuiltinBackend()
            2 -> ExternalBackend()
            else -> DefaultBackend()
        }
        equalizerManager = EqualizerManager(equalizerBackend, baseContext)
        equalizerManager.attach(exoplayer.audioSessionId)
        sendBroadcast(Intent(ACTION_EQUALIZER_UPDATED))
    }
    fun reloadEqualizer() {
        equalizerManager.release(exoplayer.audioSessionId)
        val backend: EqualizerBackend = when (Preferences.getSelectedEqualizer()) {
            1 -> BuiltinBackend()
            2 -> ExternalBackend()
            else -> DefaultBackend()
        }
        equalizerManager = EqualizerManager(backend, baseContext)
        equalizerManager.attach(exoplayer.audioSessionId)
        sendBroadcast(Intent(ACTION_RELOAD_EQUALIZER))
    }
    private fun initializeMediaLibrarySession(player: Player) {
        Log.d(TAG, "initializeMediaLibrarySession")
        val sessionActivityPendingIntent =
            TaskStackBuilder.create(this).run {
                addNextIntent(Intent(baseContext, MainActivity::class.java))
                getPendingIntent(0, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)
            }
        bitmapLoader = SyncBitmapLoader(applicationContext)
        mediaLibrarySession =
            MediaLibrarySession.Builder(this, player, getMediaLibrarySessionCallback())
                .setSessionActivity(sessionActivityPendingIntent)
                .setPeriodicPositionUpdateEnabled(false)
                .setBitmapLoader(bitmapLoader)
                .build()
    }
    private fun initializeNetworkListener() {
        networkCallback = CustomNetworkCallback()
        getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(
            networkCallback
        )
        updateMediaItems(mediaLibrarySession.player)
    }
    private fun initializeLoadControl(): DefaultLoadControl {
        val preloadSec = Preferences.getSongPreloadBuffer().toLong()
        val preloadMs = TimeUnit.SECONDS.toMillis(preloadSec).toInt()
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                preloadMs,
                preloadMs,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .build()
    }
    private fun releaseNetworkCallback() {
        getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(networkCallback)
    }
    private fun updateWidget(player: Player) {
        val mi = player.currentMediaItem
        val title = mi?.mediaMetadata?.title?.toString()
            ?: mi?.mediaMetadata?.extras?.getString("title")
        val artist = mi?.mediaMetadata?.artist?.toString()
            ?: mi?.mediaMetadata?.extras?.getString("artist")
        val album = mi?.mediaMetadata?.albumTitle?.toString()
            ?: mi?.mediaMetadata?.extras?.getString("album")
        val extras = mi?.mediaMetadata?.extras
        val coverId = extras?.getString("coverArtId")
        val songLink = extras?.getString("assetLinkSong")
            ?: AssetLinkUtil.buildLink(AssetLinkUtil.TYPE_SONG, extras?.getString("id"))
        val albumLink = extras?.getString("assetLinkAlbum")
            ?: AssetLinkUtil.buildLink(AssetLinkUtil.TYPE_ALBUM, extras?.getString("albumId"))
        val artistLink = extras?.getString("assetLinkArtist")
            ?: AssetLinkUtil.buildLink(AssetLinkUtil.TYPE_ARTIST, extras?.getString("artistId"))
        val position = player.currentPosition.takeIf { it != C.TIME_UNSET } ?: 0L
        val duration = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
        WidgetUpdateManager.updateFromState(
            this,
            title ?: "",
            artist ?: "",
            album ?: "",
            coverId,
            player.isPlaying,
            player.shuffleModeEnabled,
            player.repeatMode,
            position,
            duration,
            songLink,
            albumLink,
            artistLink
        )
    }
    private fun scheduleWidgetUpdates() {
        if (widgetUpdateScheduled) return
        widgetUpdateHandler.postDelayed(widgetUpdateRunnable, WIDGET_UPDATE_INTERVAL_MS)
        widgetUpdateScheduled = true
    }
    private fun stopWidgetUpdates() {
        if (!widgetUpdateScheduled) return
        widgetUpdateHandler.removeCallbacks(widgetUpdateRunnable)
        widgetUpdateScheduled = false
    }
    private fun getRenderersFactory(): DefaultRenderersFactory {
        val extensionRendererMode = if (DownloadUtil.useExtensionRenderers())
            DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
        else
            DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
        return object : DefaultRenderersFactory(this) {
            init {
                setExtensionRendererMode(extensionRendererMode)
            }
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf(ReplayGainUtil.getAudioProcessor()))
                    .setEnableFloatOutput(enableFloatOutput)
                    .build()
            }
        }
    }
    private fun getMediaSourceFactory(): MediaSource.Factory = DynamicMediaSourceFactory(this)
    private inner class CustomNetworkCallback : ConnectivityManager.NetworkCallback() {
        var wasWifi = false
        init {
            val manager = getSystemService(ConnectivityManager::class.java)
            val network = manager.activeNetwork
            val capabilities = manager.getNetworkCapabilities(network)
            if (capabilities != null)
                wasWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        }
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val isWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            if (isWifi != wasWifi) {
                wasWifi = isWifi
                widgetUpdateHandler.post {
                    updateMediaItems(mediaLibrarySession.player)
                }
            }
        }
    }
    inner class LocalBinder : Binder() {
        fun getEqualizerManager(): EqualizerManager {
            return equalizerManager
        }
        fun getPlayer(): ExoPlayer {
            return exoplayer
        }
    }
}
internal fun shouldApplyQueueRestore(
    restoreRevision: Long,
    currentRevision: Long,
    serviceDestroyed: Boolean,
    currentMediaItemCount: Int,
): Boolean = !serviceDestroyed &&
    restoreRevision == currentRevision &&
    currentMediaItemCount == 0
internal data class QueueRestoreTarget(
    val index: Int,
    val positionMs: Long,
)
internal fun normalizeQueueRestoreTarget(
    lastIndex: Int,
    lastPosition: Long,
    mediaItemCount: Int,
): QueueRestoreTarget? {
    if (mediaItemCount <= 0) return null
    return QueueRestoreTarget(
        index = lastIndex.coerceIn(0, mediaItemCount - 1),
        positionMs = lastPosition.coerceAtLeast(0L),
    )
}
private const val WIDGET_UPDATE_INTERVAL_MS = 1000L
private const val RADIO_HEADER_CHECK_INTERVAL_SECONDS = 30L // Reduced frequency - only fallback when ICY fails
