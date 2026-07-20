package com.cappielloantonio.tempo.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

data class PlayerProgressSnapshot(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
)

@Stable
interface PlayerProgressController {
    fun snapshot(): PlayerProgressSnapshot

    fun seekTo(positionMs: Long)
}

internal class MediaPlayerProgressController(
    private val player: Player?,
) : PlayerProgressController {
    override fun snapshot(): PlayerProgressSnapshot = normalizePlayerProgress(
        positionMs = player?.currentPosition ?: 0L,
        durationMs = player?.duration ?: 0L,
    )

    override fun seekTo(positionMs: Long) {
        val activePlayer = player ?: return
        val boundedPosition = normalizePlayerProgress(
            positionMs = positionMs,
            durationMs = activePlayer.duration,
        ).positionMs
        activePlayer.seekTo(boundedPosition)
    }
}

internal fun normalizePlayerProgress(
    positionMs: Long,
    durationMs: Long,
): PlayerProgressSnapshot {
    val normalizedDuration = durationMs.coerceAtLeast(0L)
    return PlayerProgressSnapshot(
        positionMs = positionMs.coerceIn(0L, normalizedDuration),
        durationMs = normalizedDuration,
    )
}

@Composable
internal fun rememberPlayerProgressSnapshot(
    controller: PlayerProgressController,
    isPlaying: Boolean,
    playbackState: Int,
    mediaId: String?,
    refreshToken: Int,
): PlayerProgressSnapshot {
    var snapshot by remember(controller) { mutableStateOf(controller.snapshot()) }

    LaunchedEffect(controller, isPlaying, playbackState, mediaId, refreshToken) {
        snapshot = controller.snapshot()
        while (isActive && isPlaying && playbackState == Player.STATE_READY) {
            delay(PLAYER_PROGRESS_UPDATE_INTERVAL_MS)
            snapshot = controller.snapshot()
        }
    }

    return snapshot
}

private const val PLAYER_PROGRESS_UPDATE_INTERVAL_MS = 500L
