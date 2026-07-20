package com.cappielloantonio.tempo.playback

import android.content.Context
import androidx.media3.exoplayer.analytics.PlaybackStats

data class PlaybackDiagnosticsAggregate(
    val playbackCount: Long = 0L,
    val validJoinTimeCount: Long = 0L,
    val totalValidJoinTimeMs: Long = 0L,
    val abandonedBeforeReadyCount: Long = 0L,
    val totalRebufferCount: Long = 0L,
    val maxRebufferTimeMs: Long = 0L,
    val totalAudioUnderruns: Long = 0L,
    val fatalErrorCount: Long = 0L,
    val nonFatalErrorCount: Long = 0L,
) {
    val hasData: Boolean
        get() = playbackCount > 0L

    fun merge(sample: PlaybackDiagnosticsAggregate): PlaybackDiagnosticsAggregate = copy(
        playbackCount = playbackCount.safePlus(sample.playbackCount),
        validJoinTimeCount = validJoinTimeCount.safePlus(sample.validJoinTimeCount),
        totalValidJoinTimeMs = totalValidJoinTimeMs.safePlus(sample.totalValidJoinTimeMs),
        abandonedBeforeReadyCount = abandonedBeforeReadyCount.safePlus(sample.abandonedBeforeReadyCount),
        totalRebufferCount = totalRebufferCount.safePlus(sample.totalRebufferCount),
        maxRebufferTimeMs = maxOf(maxRebufferTimeMs, sample.maxRebufferTimeMs),
        totalAudioUnderruns = totalAudioUnderruns.safePlus(sample.totalAudioUnderruns),
        fatalErrorCount = fatalErrorCount.safePlus(sample.fatalErrorCount),
        nonFatalErrorCount = nonFatalErrorCount.safePlus(sample.nonFatalErrorCount),
    )

    fun toReport(metadata: PlaybackDiagnosticsMetadata): String = buildString {
        appendLine("Tempus playback diagnostics")
        appendLine("App version: ${metadata.appVersion}")
        appendLine("Android SDK: ${metadata.androidSdk}")
        appendLine("Device: ${metadata.deviceModel}")
        appendLine()
        appendLine("Playback count: $playbackCount")
        appendLine("Valid join time count: $validJoinTimeCount")
        appendLine("Total valid join time (ms): $totalValidJoinTimeMs")
        appendLine("Abandoned before ready: $abandonedBeforeReadyCount")
        appendLine("Total rebuffer count: $totalRebufferCount")
        appendLine("Maximum rebuffer time (ms): $maxRebufferTimeMs")
        appendLine("Total audio underruns: $totalAudioUnderruns")
        appendLine("Fatal errors: $fatalErrorCount")
        appendLine("Non-fatal errors: $nonFatalErrorCount")
    }.trimEnd()
}

data class PlaybackDiagnosticsMetadata(
    val appVersion: String,
    val androidSdk: Int,
    val deviceModel: String,
)

class PlaybackDiagnosticsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun record(stats: PlaybackStats) {
        synchronized(STORE_LOCK) {
            save(readSnapshot().merge(stats.toDiagnosticsAggregate()))
        }
    }

    fun snapshot(): PlaybackDiagnosticsAggregate = synchronized(STORE_LOCK) {
        readSnapshot()
    }

    fun clear() {
        synchronized(STORE_LOCK) {
            preferences.edit().clear().apply()
        }
    }

    private fun readSnapshot(): PlaybackDiagnosticsAggregate = PlaybackDiagnosticsAggregate(
        playbackCount = preferences.getLong(KEY_PLAYBACK_COUNT, 0L),
        validJoinTimeCount = preferences.getLong(KEY_VALID_JOIN_TIME_COUNT, 0L),
        totalValidJoinTimeMs = preferences.getLong(KEY_TOTAL_VALID_JOIN_TIME_MS, 0L),
        abandonedBeforeReadyCount = preferences.getLong(KEY_ABANDONED_BEFORE_READY_COUNT, 0L),
        totalRebufferCount = preferences.getLong(KEY_TOTAL_REBUFFER_COUNT, 0L),
        maxRebufferTimeMs = preferences.getLong(KEY_MAX_REBUFFER_TIME_MS, 0L),
        totalAudioUnderruns = preferences.getLong(KEY_TOTAL_AUDIO_UNDERRUNS, 0L),
        fatalErrorCount = preferences.getLong(KEY_FATAL_ERROR_COUNT, 0L),
        nonFatalErrorCount = preferences.getLong(KEY_NON_FATAL_ERROR_COUNT, 0L),
    )

    private fun save(aggregate: PlaybackDiagnosticsAggregate) {
        preferences.edit()
            .putLong(KEY_PLAYBACK_COUNT, aggregate.playbackCount)
            .putLong(KEY_VALID_JOIN_TIME_COUNT, aggregate.validJoinTimeCount)
            .putLong(KEY_TOTAL_VALID_JOIN_TIME_MS, aggregate.totalValidJoinTimeMs)
            .putLong(KEY_ABANDONED_BEFORE_READY_COUNT, aggregate.abandonedBeforeReadyCount)
            .putLong(KEY_TOTAL_REBUFFER_COUNT, aggregate.totalRebufferCount)
            .putLong(KEY_MAX_REBUFFER_TIME_MS, aggregate.maxRebufferTimeMs)
            .putLong(KEY_TOTAL_AUDIO_UNDERRUNS, aggregate.totalAudioUnderruns)
            .putLong(KEY_FATAL_ERROR_COUNT, aggregate.fatalErrorCount)
            .putLong(KEY_NON_FATAL_ERROR_COUNT, aggregate.nonFatalErrorCount)
            .apply()
    }

    private companion object {
        val STORE_LOCK = Any()
        const val PREFERENCES_NAME = "playback_diagnostics"
        const val KEY_PLAYBACK_COUNT = "playback_count"
        const val KEY_VALID_JOIN_TIME_COUNT = "valid_join_time_count"
        const val KEY_TOTAL_VALID_JOIN_TIME_MS = "total_valid_join_time_ms"
        const val KEY_ABANDONED_BEFORE_READY_COUNT = "abandoned_before_ready_count"
        const val KEY_TOTAL_REBUFFER_COUNT = "total_rebuffer_count"
        const val KEY_MAX_REBUFFER_TIME_MS = "max_rebuffer_time_ms"
        const val KEY_TOTAL_AUDIO_UNDERRUNS = "total_audio_underruns"
        const val KEY_FATAL_ERROR_COUNT = "fatal_error_count"
        const val KEY_NON_FATAL_ERROR_COUNT = "non_fatal_error_count"
    }
}

internal fun PlaybackStats.toDiagnosticsAggregate(): PlaybackDiagnosticsAggregate =
    PlaybackDiagnosticsAggregate(
        playbackCount = playbackCount.toLong().coerceAtLeast(0L),
        validJoinTimeCount = validJoinTimeCount.toLong().coerceAtLeast(0L),
        totalValidJoinTimeMs = totalValidJoinTimeMs.coerceAtLeast(0L),
        abandonedBeforeReadyCount = abandonedBeforeReadyCount.toLong().coerceAtLeast(0L),
        totalRebufferCount = totalRebufferCount.toLong().coerceAtLeast(0L),
        maxRebufferTimeMs = maxRebufferTimeMs.coerceAtLeast(0L),
        totalAudioUnderruns = totalAudioUnderruns.coerceAtLeast(0L),
        fatalErrorCount = fatalErrorCount.toLong().coerceAtLeast(0L),
        nonFatalErrorCount = nonFatalErrorCount.toLong().coerceAtLeast(0L),
    )

private fun Long.safePlus(other: Long): Long =
    if (other > 0L && this > Long.MAX_VALUE - other) Long.MAX_VALUE else this + other
