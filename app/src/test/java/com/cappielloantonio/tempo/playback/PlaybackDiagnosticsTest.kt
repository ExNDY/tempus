package com.cappielloantonio.tempo.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackDiagnosticsTest {
    @Test
    fun `merge sums counters and keeps maximum rebuffer time`() {
        val aggregate = PlaybackDiagnosticsAggregate(
            playbackCount = 2,
            validJoinTimeCount = 1,
            totalValidJoinTimeMs = 800,
            abandonedBeforeReadyCount = 1,
            totalRebufferCount = 2,
            maxRebufferTimeMs = 250,
            totalAudioUnderruns = 3,
            fatalErrorCount = 1,
            nonFatalErrorCount = 4,
        ).merge(
            PlaybackDiagnosticsAggregate(
                playbackCount = 3,
                validJoinTimeCount = 2,
                totalValidJoinTimeMs = 1_200,
                abandonedBeforeReadyCount = 2,
                totalRebufferCount = 5,
                maxRebufferTimeMs = 175,
                totalAudioUnderruns = 1,
                fatalErrorCount = 2,
                nonFatalErrorCount = 6,
            ),
        )

        assertEquals(5L, aggregate.playbackCount)
        assertEquals(3L, aggregate.validJoinTimeCount)
        assertEquals(2_000L, aggregate.totalValidJoinTimeMs)
        assertEquals(3L, aggregate.abandonedBeforeReadyCount)
        assertEquals(7L, aggregate.totalRebufferCount)
        assertEquals(250L, aggregate.maxRebufferTimeMs)
        assertEquals(4L, aggregate.totalAudioUnderruns)
        assertEquals(3L, aggregate.fatalErrorCount)
        assertEquals(10L, aggregate.nonFatalErrorCount)
    }

    @Test
    fun `empty aggregate reports no data and counters saturate`() {
        assertFalse(PlaybackDiagnosticsAggregate().hasData)
        assertEquals(
            Long.MAX_VALUE,
            PlaybackDiagnosticsAggregate(playbackCount = Long.MAX_VALUE)
                .merge(PlaybackDiagnosticsAggregate(playbackCount = 1))
                .playbackCount,
        )
    }

    @Test
    fun `report contains device facts but no media metadata`() {
        val report = PlaybackDiagnosticsAggregate(playbackCount = 1).toReport(
            PlaybackDiagnosticsMetadata(
                appVersion = "1.2.3",
                androidSdk = 35,
                deviceModel = "Example Device",
            ),
        )

        assertTrue(report.contains("App version: 1.2.3"))
        assertTrue(report.contains("Android SDK: 35"))
        assertTrue(report.contains("Device: Example Device"))
        assertFalse(report.contains("mediaId", ignoreCase = true))
        assertFalse(report.contains("title", ignoreCase = true))
        assertFalse(report.contains("url", ignoreCase = true))
        assertFalse(report.contains("server", ignoreCase = true))
        assertFalse(report.contains("user", ignoreCase = true))
    }
}
