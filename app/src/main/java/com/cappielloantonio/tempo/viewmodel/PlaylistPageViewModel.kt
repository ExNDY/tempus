package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.repository.PlaylistRepository
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Playlist
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PlaylistPageUiState(
    val playlist: Playlist? = null,
    val songs: List<Child> = emptyList(),
    val isLoading: Boolean = true
)

@UnstableApi
class PlaylistPageViewModel(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _playlist = MutableStateFlow<Playlist?>(null)
    private val _songs = MutableStateFlow<List<Child>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private var startedPlaylistId: String? = null

    val uiState: StateFlow<PlaylistPageUiState> = combine(
        _playlist, _songs, _isLoading
    ) { playlist, songs, loading ->
        PlaylistPageUiState(playlist, songs, loading)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlaylistPageUiState()
    )

    fun onStart(playlist: Playlist) {
        if (startedPlaylistId == playlist.id && _playlist.value?.id == playlist.id) return
        startedPlaylistId = playlist.id
        _playlist.value = playlist
        _songs.value = emptyList()
        loadSongs(playlist.id)
    }

    private fun loadSongs(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            playlistRepository.getPlaylistSongs(id).observeForever { songs ->
                _songs.value = songs ?: emptyList()
                _isLoading.value = false
            }
        }
    }

    fun isPinned(): Flow<Boolean> = flow {
        val currentPlaylistId = _playlist.value?.id ?: return@flow
        emit(playlistRepository.getPinnedPlaylists().value?.any { it.playlistId == currentPlaylistId } ?: false)
    }

    fun setPinned(isNowPinned: Boolean) {
        val currentPlaylist = _playlist.value ?: return
        playlistRepository.insert(currentPlaylist)
        if (isNowPinned) {
            playlistRepository.pin(currentPlaylist.id)
        } else {
            playlistRepository.unpin(currentPlaylist.id)
        }
    }
}
