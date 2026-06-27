package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlaybackViewModel : ViewModel() {

    private val _currentSongId = MutableStateFlow<String?>(null)
    val currentSongId = _currentSongId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    // Java Interop
    fun getCurrentSongIdLiveData(): LiveData<String?> = _currentSongId.asLiveData()
    fun getIsPlayingLiveData(): LiveData<Boolean> = _isPlaying.asLiveData()

    fun update(songId: String?, playing: Boolean) {
        if (_currentSongId.value != songId) {
            _currentSongId.value = songId
        }
        if (_isPlaying.value != playing) {
            _isPlaying.value = playing
        }
    }

    fun clear() {
        _currentSongId.value = null
        _isPlaying.value = false
    }
}
