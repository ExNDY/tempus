package com.cappielloantonio.tempo.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
data class PlaylistBottomSheetUiState(
    val playlist: Playlist? = null,
    val isPinned: Boolean = false,
    val songs: List<Child> = emptyList(),
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
)
class PlaylistBottomSheetViewModel(
    private val playlistRepository: PlaylistRepository,
    private val sharingRepository: SharingRepository,
) : ViewModel() {
    private val _playlist = MutableStateFlow<Playlist?>(null)
    private val _isPinned = MutableStateFlow(false)
    private val _songs = MutableStateFlow<List<Child>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _hasError = MutableStateFlow(false)
    private var startedPlaylistId: String? = null
    private var loadJob: Job? = null
    private var pinnedJob: Job? = null
    private var songsJob: Job? = null
    val uiState: StateFlow<PlaylistBottomSheetUiState> = combine(
        _playlist, _isPinned, _songs, _isLoading, _hasError
    ) { playlist, pinned, songs, loading, hasError ->
        PlaylistBottomSheetUiState(playlist, pinned, songs, loading, hasError)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlaylistBottomSheetUiState()
    )
    fun onStart(playlistId: String, force: Boolean = false) {
        if (!force && startedPlaylistId == playlistId && (_playlist.value != null || _isLoading.value)) return
        startedPlaylistId = playlistId
        loadJob?.cancel()
        pinnedJob?.cancel()
        songsJob?.cancel()
        loadJob = viewModelScope.launch {
            _playlist.value = null
            _songs.value = emptyList()
            _isPinned.value = false
            _hasError.value = false
            _isLoading.value = true
            if (playlistId.isBlank()) {
                _isLoading.value = false
                _hasError.value = true
                return@launch
            }
            val playlist = runCatching {
                playlistRepository.getPlaylist(playlistId).asFlow().first()
            }.getOrNull()
            if (playlist == null) {
                _isLoading.value = false
                _hasError.value = true
                return@launch
            }
            _playlist.value = playlist
            observePinnedStatus(playlistId)
            observeSongs(playlistId)
        }
    }
    fun retry(playlistId: String) = onStart(playlistId, force = true)
    private fun observePinnedStatus(id: String) {
        pinnedJob = viewModelScope.launch {
            playlistRepository.getPinnedPlaylists().asFlow().collect { playlists ->
                _isPinned.value = playlists.any { it.playlistId == id }
            }
        }
    }
    private fun observeSongs(id: String) {
        songsJob = viewModelScope.launch {
            playlistRepository.getPlaylistSongs(id).asFlow().collect { songs ->
                _songs.value = songs.orEmpty()
                _isLoading.value = false
            }
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
