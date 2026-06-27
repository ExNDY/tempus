package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.repository.PlaylistRepository
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaylistCatalogueArgs(val type: String = Constants.PLAYLIST_ALL)

data class PlaylistCatalogueUiState(
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true,
    val type: String = Constants.PLAYLIST_ALL,
)

@UnstableApi
class PlaylistCatalogueViewModel(
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlaylistCatalogueUiState())
    val uiState: StateFlow<PlaylistCatalogueUiState> = _uiState.asStateFlow()
    private var startedType: String? = null

    fun onStart(args: PlaylistCatalogueArgs) {
        if (startedType == args.type) return
        startedType = args.type
        _uiState.value = PlaylistCatalogueUiState(type = args.type, isLoading = true)
        refresh()
    }

    fun refresh(sortOrder: String = Preferences.getHomeSortPlaylists()) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            playlistRepository.getSortedPlaylists(sortOrder).asFlow().collectLatest { playlists ->
                val filtered = when (_uiState.value.type) {
                    Constants.PLAYLIST_DOWNLOADED -> playlists?.filter { it.coverArtId != null || it.songCount > 0 }
                    else -> playlists
                }.orEmpty()
                _uiState.update { it.copy(playlists = filtered, isLoading = false) }
            }
        }
    }
}
