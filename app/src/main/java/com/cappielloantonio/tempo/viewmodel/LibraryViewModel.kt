package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.DirectoryRepository
import com.cappielloantonio.tempo.repository.GenreRepository
import com.cappielloantonio.tempo.repository.PlaylistRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Genre
import com.cappielloantonio.tempo.subsonic.models.MusicFolder
import com.cappielloantonio.tempo.subsonic.models.Playlist
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val musicFolders: List<MusicFolder> = emptyList(),
    val albums: List<AlbumID3> = emptyList(),
    val artists: List<ArtistID3> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true,
)

class LibraryViewModel(
    private val directoryRepository: DirectoryRepository,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
    private val genreRepository: GenreRepository,
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private var started = false
    private var musicFoldersJob: Job? = null
    private var albumsJob: Job? = null
    private var artistsJob: Job? = null
    private var genresJob: Job? = null
    private var playlistsJob: Job? = null

    fun onStart() {
        if (started) return
        started = true
        refreshAll()
    }

    fun refreshAll() {
        _uiState.update { it.copy(isLoading = true) }
        refreshMusicFolders()
        refreshAlbumSample()
        refreshArtistSample()
        refreshGenreSample()
        refreshPlaylistSample(markLoadingDone = true)
    }

    fun refreshAlbumSample() {
        albumsJob?.cancel()
        albumsJob = viewModelScope.launch {
            albumRepository.getAlbums("random", 10, null, null).asFlow().collectLatest { albums ->
                _uiState.update { it.copy(albums = albums ?: emptyList(), isLoading = false) }
            }
        }
    }

    fun refreshArtistSample() {
        artistsJob?.cancel()
        artistsJob = viewModelScope.launch {
            artistRepository.getArtists(true, 10).asFlow().collectLatest { artists ->
                _uiState.update { it.copy(artists = artists ?: emptyList(), isLoading = false) }
            }
        }
    }

    fun refreshGenreSample() {
        genresJob?.cancel()
        genresJob = viewModelScope.launch {
            genreRepository.getGenres(true, 15).asFlow().collectLatest { genres ->
                _uiState.update { it.copy(genres = genres ?: emptyList(), isLoading = false) }
            }
        }
    }

    fun refreshPlaylistSample(markLoadingDone: Boolean = false) {
        playlistsJob?.cancel()
        playlistsJob = viewModelScope.launch {
            playlistRepository.getPlaylists(true, 10).asFlow().collectLatest { playlists ->
                _uiState.update {
                    it.copy(
                        playlists = playlists ?: emptyList(),
                        isLoading = if (markLoadingDone) false else it.isLoading,
                    )
                }
            }
        }
    }

    private fun refreshMusicFolders() {
        musicFoldersJob?.cancel()
        musicFoldersJob = viewModelScope.launch {
            directoryRepository.getMusicFolders().asFlow().collectLatest { folders ->
                _uiState.update { it.copy(musicFolders = folders ?: emptyList(), isLoading = false) }
            }
        }
    }
}
