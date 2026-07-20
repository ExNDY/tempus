package com.cappielloantonio.tempo.util

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.extractor.metadata.id3.InternalFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.model.ReplayGain
import com.cappielloantonio.tempo.subsonic.models.ReplayGainInfo
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.math.log10

@androidx.media3.common.util.UnstableApi
object ReplayGainUtil {
    private const val TAG = "ReplayGainUtil"
    private val tags = arrayOf(
        "REPLAYGAIN_TRACK_GAIN", "REPLAYGAIN_ALBUM_GAIN",
        "R128_TRACK_GAIN", "R128_ALBUM_GAIN",
        "REPLAYGAIN_TRACK_PEAK", "REPLAYGAIN_ALBUM_PEAK"
    )
    private val gainDataMap = ConcurrentHashMap<String, List<ReplayGain>>()
    private val prefetchedIds = ConcurrentHashMap.newKeySet<String>()
    private val prefetchExecutor: ExecutorService = Executors.newFixedThreadPool(2)
    private val audioProcessor = ReplayGainAudioProcessor()
    private var playerRef = WeakReference<Player>(null)
    private val mainHandler = Handler(Looper.getMainLooper())

    @JvmStatic
    fun getAudioProcessor(): ReplayGainAudioProcessor {
        return audioProcessor
    }

    @JvmStatic
    fun release() {
        gainDataMap.clear()
        prefetchedIds.clear()
        playerRef = WeakReference(null)
    }

    @JvmStatic
    fun prefetchQueueGains(player: Player) {
        if (Preferences.getReplayGainMode() == "disabled") return
        playerRef = WeakReference(player)
        for (i in 0 until player.mediaItemCount) {
            val item = player.getMediaItemAt(i)
            if (item.localConfiguration == null) continue
            val serverInfo = extractServerInfo(item)
            if (serverInfo != null) {
                if (prefetchedIds.add(item.mediaId)) {
                    gainDataMap[item.mediaId] = serverInfoToGains(serverInfo)
                    Log.d(TAG, "Prefetch skip (server RG available) " + item.mediaId)
                }
                continue
            }
            if (!prefetchedIds.add(item.mediaId)) continue
            submitPrefetch(item)
        }
    }

    private fun submitPrefetch(item: MediaItem) {
        prefetchExecutor.execute {
            try {
                val metadataList = retrieveFallbackMetadata(item)
                val gains = getReplayGains(metadataList)
                val prefetchedGainsValid =
                    resolveTrackGain(gains) != 0f || resolveAlbumGain(gains) != 0f
                if (prefetchedGainsValid) {
                    gainDataMap[item.mediaId] = gains
                }
                Log.d(
                    TAG, "Prefetched " + item.mediaId +
                        " trackGain=" + resolveTrackGain(gains) +
                        " valid=" + prefetchedGainsValid
                )
                mainHandler.post {
                    val p = playerRef.get() ?: return@post
                    val current = p.currentMediaItem
                    if (current != null && item.mediaId == current.mediaId) {
                        val gain = resolveGain(p, gains)
                        if (gain != 0f) {
                            val peak = resolvePeak(p, gains)
                            val totalGain = computeTotalGain(gain, peak)
                            Log.d(
                                TAG, "Late prefetch for current track " + item.mediaId +
                                    " — applying gain immediately totalGain=" + totalGain
                            )
                            audioProcessor.setGainImmediate(totalGain)
                        } else {
                            Log.d(
                                TAG,
                                "Late prefetch for current track " + item.mediaId + " — empty gains, skipping setGainImmediate"
                            )
                        }
                    }
                    queuePendingForNextTrack(p)
                }
            } catch (e: Throwable) {
                Log.d(TAG, "Prefetch failed for " + item.mediaId + ": " + e)
                prefetchedIds.remove(item.mediaId)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun retrieveFallbackMetadata(item: MediaItem): List<Metadata> {
        // Used only when the server response does not include ReplayGain metadata.
        androidx.media3.exoplayer.MetadataRetriever.Builder(App.getInstance(), item).build()
            .use { retriever ->
                val trackGroups = retriever.retrieveTrackGroups().get(20, TimeUnit.SECONDS)
                return extractMetadata(trackGroups)
            }
    }

    @JvmStatic
    fun applyGain(player: Player, mediaItem: MediaItem?) {
        audioProcessor.clearPendingGain()
        if (mediaItem?.mediaId == null) {
            Log.d(TAG, "applyGain: null mediaItem or mediaId, skipping")
            return
        }
        val serverInfo = extractServerInfo(mediaItem)
        if (serverInfo != null) {
            val gains = serverInfoToGains(serverInfo)
            gainDataMap[mediaItem.mediaId] = gains
            prefetchedIds.add(mediaItem.mediaId)
            val gain = resolveGain(player, gains)
            val peak = resolvePeak(player, gains)
            val totalGain = computeTotalGain(gain, peak)
            Log.d(
                TAG, "applyGain: server RG for " + mediaItem.mediaId +
                    " gain=" + gain + " peak=" + peak +
                    " totalGain=" + totalGain
            )
            audioProcessor.setGainImmediate(totalGain)
            queuePendingForNextTrack(player)
            return
        }
        val gains = gainDataMap[mediaItem.mediaId]
        if (gains != null) {
            val gain = resolveGain(player, gains)
            if (gain != 0f) {
                val peak = resolvePeak(player, gains)
                val totalGain = computeTotalGain(gain, peak)
                Log.d(
                    TAG, "applyGain: tag cache hit for " + mediaItem.mediaId +
                        " gain=" + gain + " peak=" + peak +
                        " totalGain=" + totalGain
                )
                audioProcessor.setGainImmediate(totalGain)
            } else {
                val preampOnly = computeTotalGain(0f, 0f)
                Log.d(
                    TAG, "applyGain: cache hit but gain=0 for " + mediaItem.mediaId +
                        ", applying preamp-only totalGain=" + preampOnly
                )
                audioProcessor.setGainImmediate(preampOnly)
            }
        } else {
            Log.d(
                TAG,
                "applyGain: cache miss for " + mediaItem.mediaId + ", holding current gain until onTracksChanged"
            )
        }
        queuePendingForNextTrack(player)
    }

    @JvmStatic
    fun setReplayGain(player: Player, tracks: Tracks?) {
        if (tracks == null || tracks.groups.isEmpty()) return
        val currentItem = player.currentMediaItem
        if (currentItem != null && extractServerInfo(currentItem) != null) {
            Log.d(
                TAG, "setReplayGain: server RG already applied for " +
                    currentItem.mediaId + ", ignoring tag-extracted values"
            )
            queuePendingForNextTrack(player)
            return
        }
        val metadataList = extractMetadata(tracks)
        var gains = getReplayGains(metadataList)
        val mediaId = currentItem?.mediaId
        val cached = if (mediaId != null) gainDataMap[mediaId] else null
        val extractedIsEmpty = resolveTrackGain(gains) == 0f && resolveAlbumGain(gains) == 0f
        if (extractedIsEmpty && cached != null) {
            Log.d(
                TAG, "setReplayGain: extracted gains empty (seek past header?), " +
                    "keeping cached gains for " + mediaId
            )
            gains = cached
        } else if (mediaId != null) {
            gainDataMap[mediaId] = gains
            prefetchedIds.add(mediaId)
        }
        val gain = resolveGain(player, gains)
        if (gain == 0f) {
            val preampOnly = computeTotalGain(0f, 0f)
            Log.d(
                TAG, "setReplayGain: no effective gain data for " + mediaId +
                    ", applying preamp-only totalGain=" + preampOnly
            )
            audioProcessor.setGainImmediate(preampOnly)
            queuePendingForNextTrack(player)
            return
        }
        val peak = resolvePeak(player, gains)
        audioProcessor.setGainImmediate(computeTotalGain(gain, peak))
        queuePendingForNextTrack(player)
    }

    @JvmStatic
    fun reapplyCurrentTrackGain(player: Player) {
        val currentItem = player.currentMediaItem
        if (currentItem?.mediaId == null) {
            Log.d(TAG, "reapplyCurrentTrackGain: no current item, skipping")
            return
        }
        val serverInfo = extractServerInfo(currentItem)
        if (serverInfo != null) {
            val gains = serverInfoToGains(serverInfo)
            val gain = resolveGain(player, gains)
            val peak = resolvePeak(player, gains)
            val totalGain = computeTotalGain(gain, peak)
            Log.d(
                TAG,
                "reapplyCurrentTrackGain: server RG for " + currentItem.mediaId + " totalGain=" + totalGain
            )
            audioProcessor.setGainImmediate(totalGain)
            return
        }
        val cached = gainDataMap[currentItem.mediaId]
        if (cached != null) {
            val gain = resolveGain(player, cached)
            if (gain != 0f) {
                val peak = resolvePeak(player, cached)
                val totalGain = computeTotalGain(gain, peak)
                Log.d(
                    TAG,
                    "reapplyCurrentTrackGain: cache hit for " + currentItem.mediaId + " totalGain=" + totalGain
                )
                audioProcessor.setGainImmediate(totalGain)
                return
            }
            Log.d(
                TAG,
                "reapplyCurrentTrackGain: cache hit but gain=0 for " + currentItem.mediaId + ", keeping current gain"
            )
            return
        }
        Log.d(
            TAG,
            "reapplyCurrentTrackGain: no cached data for " + currentItem.mediaId + ", keeping current gain"
        )
    }

    private fun queuePendingForNextTrack(player: Player) {
        val nextIndex = player.nextMediaItemIndex
        if (nextIndex == C.INDEX_UNSET) return
        val nextItem = player.getMediaItemAt(nextIndex)
        val gains = gainDataMap[nextItem.mediaId]
        val resolvedGain = if (gains != null) resolveGainForNextTrack(player, gains) else 0f
        if (resolvedGain == 0f) {
            if (gains == null) {
                Log.d(
                    TAG,
                    "queuePendingForNextTrack: no RG data yet for " + nextItem.mediaId + ", carrying over current gain"
                )
                return
            }
            val preampOnly = computeTotalGain(0f, 0f)
            audioProcessor.setPendingGain(preampOnly)
            Log.d(
                TAG,
                "queuePendingForNextTrack: no RG tags for " + nextItem.mediaId + ", queuing preamp-only totalGain=" + preampOnly
            )
            return
        }
        val totalGain = computeTotalGain(resolvedGain, resolvePeakForNextTrack(player, gains))
        audioProcessor.setPendingGain(totalGain)
    }

    private fun extractMetadata(tracks: Tracks?): List<Metadata> {
        val result = ArrayList<Metadata>()
        if (tracks == null) return result
        for (group in tracks.groups) {
            val mediaTrackGroup = group.mediaTrackGroup
            for (j in 0 until mediaTrackGroup.length) {
                val m = group.getTrackFormat(j).metadata
                if (m != null) result.add(m)
            }
        }
        return result
    }

    private fun extractMetadata(trackGroups: TrackGroupArray?): List<Metadata> {
        val result = ArrayList<Metadata>()
        if (trackGroups == null) return result
        for (i in 0 until trackGroups.length) {
            val group = trackGroups[i]
            for (j in 0 until group.length) {
                val m = group.getFormat(j).metadata
                if (m != null) result.add(m)
            }
        }
        return result
    }

    private fun getReplayGains(metadataList: List<Metadata>?): List<ReplayGain> {
        val id3Gains = ReplayGain()
        val fallbackGains = ReplayGain()
        if (metadataList != null) {
            for (metadata in metadataList) {
                for (j in 0 until metadata.length()) {
                    val entry = metadata[j]
                    if (!isReplayGainEntry(entry)) continue
                    val isId3 = entry is TextInformationFrame || entry is InternalFrame
                    mergeIntoReplayGain(entry, if (isId3) id3Gains else fallbackGains)
                }
            }
        }
        val gains = ArrayList<ReplayGain>()
        gains.add(id3Gains)
        gains.add(fallbackGains)
        return gains
    }

    private fun isReplayGainEntry(entry: Metadata.Entry): Boolean {
        val upper = entry.toString().uppercase(Locale.ROOT)
        for (tag in tags) {
            if (upper.contains(tag)) return true
        }
        return false
    }

    private fun mergeIntoReplayGain(entry: Metadata.Entry, target: ReplayGain) {
        var str = entry.toString()
        if (entry is InternalFrame) {
            str = entry.description + entry.text
        } else if (entry is TextInformationFrame) {
            val desc = entry.description ?: entry.id
            str = desc + if (entry.values.isNotEmpty()) entry.values[0] else ""
        }
        val upper = str.uppercase(Locale.ROOT)
        if (upper.contains(tags[0])) target.trackGain = parseReplayGainTag(str)
        if (upper.contains(tags[1])) target.albumGain = parseReplayGainTag(str)
        if (upper.contains(tags[2])) target.trackGain = parseReplayGainTag(str) / 256f + 5f
        if (upper.contains(tags[3])) target.albumGain = parseReplayGainTag(str) / 256f + 5f
        if (upper.contains(tags[4])) target.trackPeak = parseReplayGainTag(str)
        if (upper.contains(tags[5])) target.albumPeak = parseReplayGainTag(str)
    }

    private fun parseReplayGainTag(entry: String): Float {
        return try {
            val matcher = Pattern.compile(
                "(-?\\d+(?:\\.\\d+)?)\\s*(?:dB)?\\s*$",
                Pattern.CASE_INSENSITIVE
            ).matcher(entry.trim { it <= ' ' })
            var lastMatch: String? = null
            while (matcher.find()) lastMatch = matcher.group(1)
            lastMatch?.toFloat() ?: 0f
        } catch (_: NumberFormatException) {
            0f
        }
    }

    private fun resolveGain(player: Player, gains: List<ReplayGain>?): Float {
        if (Preferences.getReplayGainMode() == "disabled" || gains.isNullOrEmpty()) return 0f
        val mode = Preferences.getReplayGainMode()
        return when (mode) {
            "track" -> resolveTrackGain(gains)
            "album" -> resolveAlbumGain(gains)
            "auto" -> if (areTracksConsecutive(player)) resolveAlbumGain(gains) else resolveTrackGain(
                gains
            )
            else -> 0f
        }
    }

    private fun resolveGainForNextTrack(player: Player, gains: List<ReplayGain>?): Float {
        if (Preferences.getReplayGainMode() == "disabled" || gains.isNullOrEmpty()) return 0f
        val mode = Preferences.getReplayGainMode()
        return when (mode) {
            "track" -> resolveTrackGain(gains)
            "album" -> resolveAlbumGain(gains)
            "auto" -> if (areCurrentAndNextConsecutive(player)) resolveAlbumGain(gains) else resolveTrackGain(
                gains
            )
            else -> 0f
        }
    }

    private fun resolveTrackGain(gains: List<ReplayGain>): Float {
        val primary = gains[0].trackGain
        val secondary = gains[1].trackGain
        return if (primary != 0f) primary else secondary
    }

    private fun resolveAlbumGain(gains: List<ReplayGain>): Float {
        val primary = gains[0].albumGain
        val secondary = gains[1].albumGain
        val album = if (primary != 0f) primary else secondary
        return if (album != 0f) album else resolveTrackGain(gains)
    }

    private fun resolvePeak(player: Player, gains: List<ReplayGain>?): Float {
        if (Preferences.getReplayGainMode() == "disabled" || gains.isNullOrEmpty()) return 0f
        val useAlbum = Preferences.getReplayGainMode() == "album" ||
            (Preferences.getReplayGainMode() == "auto" && areTracksConsecutive(player))
        return resolveTrackOrAlbumPeak(gains, useAlbum)
    }

    private fun resolvePeakForNextTrack(player: Player, gains: List<ReplayGain>?): Float {
        if (Preferences.getReplayGainMode() == "disabled" || gains.isNullOrEmpty()) return 0f
        val useAlbum = Preferences.getReplayGainMode() == "album" ||
            (Preferences.getReplayGainMode() == "auto" && areCurrentAndNextConsecutive(player))
        return resolveTrackOrAlbumPeak(gains, useAlbum)
    }

    private fun resolveTrackOrAlbumPeak(gains: List<ReplayGain>, useAlbum: Boolean): Float {
        if (useAlbum) {
            val primary = gains[0].albumPeak
            val secondary = gains[1].albumPeak
            val albumPeak = if (primary != 0f) primary else secondary
            if (albumPeak != 0f) return albumPeak
        }
        val primary = gains[0].trackPeak
        val secondary = gains[1].trackPeak
        return if (primary != 0f) primary else secondary
    }

    private fun areTracksConsecutive(player: Player): Boolean {
        val current = player.currentMediaItem
        val prevIdx = player.previousMediaItemIndex
        val prev = if (prevIdx == C.INDEX_UNSET) null else player.getMediaItemAt(prevIdx)
        return current != null && prev != null &&
            current.mediaMetadata.albumTitle != null &&
            prev.mediaMetadata.albumTitle != null &&
            prev.mediaMetadata.albumTitle.toString() == current.mediaMetadata.albumTitle.toString()
    }

    private fun areCurrentAndNextConsecutive(player: Player): Boolean {
        val current = player.currentMediaItem
        val nextIdx = player.nextMediaItemIndex
        val next = if (nextIdx == C.INDEX_UNSET) null else player.getMediaItemAt(nextIdx)
        return current != null && next != null &&
            current.mediaMetadata.albumTitle != null &&
            next.mediaMetadata.albumTitle != null &&
            current.mediaMetadata.albumTitle.toString() == next.mediaMetadata.albumTitle.toString()
    }

    private fun computeTotalGain(gain: Float, peak: Float): Float {
        val preamp = Preferences.getLoudnessPreamp()
        var totalGain = gain + preamp
        if (Preferences.isReplayGainPreventClipping() && peak > 0f) {
            val maxGainForPeak = -(20.0 * log10(peak.toDouble())).toFloat()
            if (totalGain > maxGainForPeak) totalGain = maxGainForPeak
        }
        return (-60f).coerceAtLeast(15f.coerceAtMost(totalGain))
    }

    private fun extractServerInfo(item: MediaItem?): ReplayGainInfo? {
        if (item?.mediaMetadata == null) return null
        if (!ReplayGainBundleUtil.isPresent(item.mediaMetadata.extras)) return null
        val info = ReplayGainBundleUtil.fromBundle(item.mediaMetadata.extras)
        return if (info != null && info.hasAnyValue()) info else null
    }

    private fun serverInfoToGains(info: ReplayGainInfo): List<ReplayGain> {
        val primary = ReplayGain()
        if (info.trackGain != null) primary.trackGain = info.trackGain!!
        if (info.albumGain != null) primary.albumGain = info.albumGain!!
        if (info.trackPeak != null) primary.trackPeak = info.trackPeak!!
        if (info.albumPeak != null) primary.albumPeak = info.albumPeak!!
        val secondary = ReplayGain()
        val fallback = info.fallbackGain
        if (fallback != null) {
            secondary.trackGain = fallback
            secondary.albumGain = fallback
        }
        val gains = ArrayList<ReplayGain>()
        gains.add(primary)
        gains.add(secondary)
        return gains
    }
}
