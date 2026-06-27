package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.repository.PlaylistRepository
import com.cappielloantonio.tempo.repository.SharingRepository
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.subsonic.models.Share
import com.cappielloantonio.tempo.util.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class PlaylistBottomSheetUiState(
    val playlist: Playlist? = null,
    val isPinned: Boolean = false,
    val songs: List<Child> = emptyList(),
    val isLoading: Boolean = false
)

@UnstableApi
class PlaylistBottomSheetViewModel(
    private val playlistRepository: PlaylistRepository,
    private val sharingRepository: SharingRepository,
) : ViewModel() {

    private val _playlist = MutableStateFlow<Playlist?>(null)
    private val _isPinned = MutableStateFlow(false)
    private val _songs = MutableStateFlow<List<Child>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private var startedPlaylistId: String? = null

    val uiState: StateFlow<PlaylistBottomSheetUiState> = combine(
        _playlist, _isPinned, _songs, _isLoading
    ) { playlist, pinned, songs, loading ->
        PlaylistBottomSheetUiState(playlist, pinned, songs, loading)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlaylistBottomSheetUiState()
    )

    fun onStart(playlist: Playlist) {
        if (startedPlaylistId == playlist.id && _playlist.value?.id == playlist.id) {
            return
        }
        startedPlaylistId = playlist.id
        _playlist.value = playlist
        checkPinnedStatus(playlist.id)
        loadSongs(playlist.id)
    }

    private fun checkPinnedStatus(id: String) {
        playlistRepository.getPinnedPlaylists().observeForever { playlists ->
            _isPinned.value = playlists.any { it.playlistId == id }
        }
    }

    private fun loadSongs(id: String) {
        _isLoading.value = true
        playlistRepository.getPlaylistSongs(id).observeForever { songs ->
            _songs.value = songs ?: emptyList()
            _isLoading.value = false
        }
    }

    fun togglePin() {
        val currentPlaylist = _playlist.value ?: return
        if (_isPinned.value) {
            playlistRepository.unpin(currentPlaylist.id)
        } else {
            playlistRepository.pin(currentPlaylist.id)
        }
        _isPinned.update { !it }
    }

    fun isEditableByCurrentUser(): Boolean {
        val playlist = _playlist.value ?: return false
        val currentUser = Preferences.getUser()
        return playlist.owner.isNullOrEmpty() || playlist.owner == currentUser
    }

    fun getSongs(): Flow<List<Child>> = if (_playlist.value == null) flowOf(emptyList()) else _songs

    suspend fun sharePlaylist(): Share? {
        val playlist = _playlist.value ?: return null
        return sharingRepository.createShare(playlist.id, playlist.name, null).asFlow().first()
    }
}
