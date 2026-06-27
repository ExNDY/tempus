package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.asFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.repository.PlaylistRepository
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.util.Preferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PlaylistPageUiState(
    val playlist: Playlist? = null,
    val isPinned: Boolean = false,
    val isEditable: Boolean = false,
    val songs: List<Child> = emptyList(),
    val isLoading: Boolean = true
)

@UnstableApi
class PlaylistPageViewModel(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _playlist = MutableStateFlow<Playlist?>(null)
    private val _isPinned = MutableStateFlow(false)
    private val _isEditable = MutableStateFlow(false)
    private val _songs = MutableStateFlow<List<Child>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private var startedPlaylistId: String? = null
    private var loadJob: Job? = null

    val uiState: StateFlow<PlaylistPageUiState> = combine(
        _playlist, _isPinned, _isEditable, _songs, _isLoading
    ) { playlist, pinned, editable, songs, loading ->
        PlaylistPageUiState(playlist, pinned, editable, songs, loading)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlaylistPageUiState()
    )

    init {
        observePinnedState()
        observePlaylistMutations()
    }

    fun onStart(playlist: Playlist) {
        if (startedPlaylistId == playlist.id && _playlist.value?.id == playlist.id) return
        startedPlaylistId = playlist.id
        _playlist.value = playlist
        _isEditable.value = isEditableByCurrentUser(playlist)
        _songs.value = emptyList()
        refreshPinnedState(playlist.id)
        loadData(playlist)
    }

    private fun loadData(playlist: Playlist) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true

            val playlistDeferred = async { playlistRepository.getPlaylist(playlist.id).asFlow().first() }
            val songsDeferred = async { playlistRepository.getPlaylistSongs(playlist.id).asFlow().first().orEmpty() }

            val refreshedPlaylist = playlistDeferred.await()
            _playlist.value = refreshedPlaylist
            _isEditable.value = refreshedPlaylist?.let(::isEditableByCurrentUser) ?: false
            _songs.value = songsDeferred.await().filter { !it.isDir && !it.isVideo }
            refreshPinnedState(playlist.id)
            _isLoading.value = false
        }
    }

    private fun observePinnedState() {
        viewModelScope.launch {
            playlistRepository.getPinnedPlaylists().asFlow().collect { playlists ->
                val currentPlaylistId = _playlist.value?.id
                _isPinned.value = if (currentPlaylistId == null) {
                    false
                } else {
                    playlists.orEmpty().any { it.playlistId == currentPlaylistId }
                }
            }
        }
    }

    private fun observePlaylistMutations() {
        viewModelScope.launch {
            playlistRepository.getPlaylistUpdateTrigger().asFlow().collect { shouldRefresh ->
                val currentPlaylist = _playlist.value ?: return@collect
                if (shouldRefresh == true) {
                    loadData(currentPlaylist)
                }
            }
        }
    }

    private fun refreshPinnedState(playlistId: String) {
        _isPinned.value = playlistRepository
            .getPinnedPlaylists()
            .value
            .orEmpty()
            .any { it.playlistId == playlistId }
    }

    fun togglePinned() {
        val currentPlaylist = _playlist.value ?: return
        playlistRepository.insert(currentPlaylist)
        if (_isPinned.value) {
            playlistRepository.unpin(currentPlaylist.id)
        } else {
            playlistRepository.pin(currentPlaylist.id)
        }
        _isPinned.update { !it }
    }

    private fun isEditableByCurrentUser(playlist: Playlist): Boolean {
        val currentUser = Preferences.getUser()
        return playlist.owner.isNullOrEmpty() || playlist.owner == currentUser
    }
}
