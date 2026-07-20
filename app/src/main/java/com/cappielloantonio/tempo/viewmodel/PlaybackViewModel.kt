package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.cappielloantonio.tempo.playback.PlaybackStateStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class PlaybackViewModel(
    private val playbackStateStore: PlaybackStateStore,
) : ViewModel() {

    val currentSongId = playbackStateStore.state
        .map { it.currentSongId }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = playbackStateStore.state.value.currentSongId,
        )

    val isPlaying = playbackStateStore.state
        .map { it.isPlaying }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = playbackStateStore.state.value.isPlaying,
        )

    // Java Interop
    fun getCurrentSongIdLiveData(): LiveData<String?> = currentSongId.asLiveData()
    fun getIsPlayingLiveData(): LiveData<Boolean> = isPlaying.asLiveData()

    fun update(songId: String?, playing: Boolean) {
        playbackStateStore.update(songId, playing)
    }

    fun clear() {
        playbackStateStore.clear()
    }
}
