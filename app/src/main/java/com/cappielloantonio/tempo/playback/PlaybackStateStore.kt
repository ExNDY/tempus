package com.cappielloantonio.tempo.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackState(
    val currentSongId: String? = null,
    val isPlaying: Boolean = false,
)

class PlaybackStateStore {
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    fun update(songId: String?, playing: Boolean) {
        val current = _state.value
        if (current.currentSongId == songId && current.isPlaying == playing) return
        _state.value = current.copy(
            currentSongId = songId,
            isPlaying = playing,
        )
    }

    fun clear() {
        if (_state.value == PlaybackState()) return
        _state.value = PlaybackState()
    }
}
