package com.cappielloantonio.tempo.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.repository.DownloadRepository
import com.cappielloantonio.tempo.repository.PlaylistRepository
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.Preferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
data class PlaylistCatalogueArgs(val type: String = Constants.PLAYLIST_ALL)
data class PlaylistCatalogueUiState(
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true,
    val type: String = Constants.PLAYLIST_ALL,
    val pinnedPlaylistIds: Set<String> = emptySet(),
    val downloadedPlaylistIds: Set<String> = emptySet(),
)
class PlaylistCatalogueViewModel(
    private val playlistRepository: PlaylistRepository,
    private val downloadRepository: DownloadRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlaylistCatalogueUiState())
    val uiState: StateFlow<PlaylistCatalogueUiState> = _uiState.asStateFlow()
    private var startedType: String? = null
    private var allPlaylists: List<Playlist> = emptyList()
    private var refreshJob: Job? = null
    private var pinnedJob: Job? = null
    private var downloadsJob: Job? = null
    private var playlistUpdatesJob: Job? = null
    fun onStart(args: PlaylistCatalogueArgs) {
        if (startedType == args.type) return
        startedType = args.type
        _uiState.value = PlaylistCatalogueUiState(type = args.type, isLoading = true)
        observePinnedPlaylists()
        observeDownloadedPlaylists()
        observePlaylistUpdates()
        refresh()
    }
    fun refresh(sortOrder: String = Preferences.getHomeSortPlaylists()) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            allPlaylists = playlistRepository.getSortedPlaylists(sortOrder).asFlow().first().orEmpty()
            _uiState.update { current ->
                current.copy(
                    playlists = filterPlaylists(
                        playlists = allPlaylists,
                        type = current.type,
                        downloadedPlaylistIds = current.downloadedPlaylistIds,
                    ),
                    isLoading = false,
                )
            }
        }
    }
    private fun observePinnedPlaylists() {
        if (pinnedJob != null) return
        pinnedJob = viewModelScope.launch {
            playlistRepository.getPinnedPlaylists().asFlow().collect { playlists ->
                val pinnedIds = playlists.orEmpty().map { it.playlistId }.toSet()
                _uiState.update { it.copy(pinnedPlaylistIds = pinnedIds) }
                if (Preferences.getHomeSortPlaylists() == Constants.PLAYLIST_ORDER_BY_PINNED) {
                    refresh()
                }
            }
        }
    }
    private fun observeDownloadedPlaylists() {
        if (downloadsJob != null) return
        downloadsJob = viewModelScope.launch {
            downloadRepository.getLiveDownload().asFlow().collect { downloads ->
                val downloadedIds = downloads
                    .orEmpty()
                    .asSequence()
                    .mapNotNull(Download::playlistId)
                    .filter(String::isNotBlank)
                    .toSet()
                _uiState.update { current ->
                    current.copy(
                        downloadedPlaylistIds = downloadedIds,
                        playlists = filterPlaylists(
                            playlists = allPlaylists,
                            type = current.type,
                            downloadedPlaylistIds = downloadedIds,
                        ),
                    )
                }
            }
        }
    }
    private fun observePlaylistUpdates() {
        if (playlistUpdatesJob != null) return
        playlistUpdatesJob = viewModelScope.launch {
            playlistRepository.getPlaylistUpdateTrigger().asFlow().collect {
                refresh()
            }
        }
    }
    private fun filterPlaylists(
        playlists: List<Playlist>,
        type: String,
        downloadedPlaylistIds: Set<String>,
    ): List<Playlist> {
        return when (type) {
            Constants.PLAYLIST_DOWNLOADED -> playlists.filter { it.id in downloadedPlaylistIds }
            else -> playlists
        }
    }
}
