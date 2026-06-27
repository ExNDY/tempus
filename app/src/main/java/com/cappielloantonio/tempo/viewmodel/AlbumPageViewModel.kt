package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.asFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cappielloantonio.tempo.interfaces.StarCallback
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.FavoriteRepository
import com.cappielloantonio.tempo.subsonic.models.*
import com.cappielloantonio.tempo.util.NetworkUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date

data class AlbumPageUiState(
    val album: AlbumID3? = null,
    val albumInfo: AlbumInfo? = null,
    val songs: List<Child> = emptyList(),
    val isLoading: Boolean = true
)

class AlbumPageViewModel(
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
    private val favoriteRepository: FavoriteRepository,
) : ViewModel() {

    private val _album = MutableStateFlow<AlbumID3?>(null)
    private val _albumInfo = MutableStateFlow<AlbumInfo?>(null)
    private val _songs = MutableStateFlow<List<Child>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private var startedAlbumId: String? = null
    private var loadJob: Job? = null

    val uiState: StateFlow<AlbumPageUiState> = combine(
        _album, _albumInfo, _songs, _isLoading
    ) { album, albumInfo, songs, loading ->
        AlbumPageUiState(album, albumInfo, songs, loading)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AlbumPageUiState()
    )

    fun onStart(album: AlbumID3) {
        if (startedAlbumId == album.id && _album.value?.id == album.id) return
        startedAlbumId = album.id
        _album.value = album
        _songs.value = emptyList()
        _albumInfo.value = null
        loadData(album)
    }

    private fun loadData(album: AlbumID3) {
        val albumId = album.id.orEmpty()
        if (albumId.isBlank()) {
            _isLoading.value = false
            return
        }

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            val albumDeferred = async { albumRepository.getAlbum(albumId).asFlow().first() }
            val tracksDeferred = async { albumRepository.getAlbumTracks(albumId).asFlow().first().orEmpty() }
            val infoDeferred = async { albumRepository.getAlbumInfo(albumId).asFlow().first() }

            _album.value = albumDeferred.await() ?: album
            _songs.value = tracksDeferred.await().filter { !it.isDir && !it.isVideo }
            _albumInfo.value = infoDeferred.await()
            _isLoading.value = false
        }
    }

    fun setFavorite() {
        val currentAlbum = _album.value ?: return

        if (currentAlbum.starred != null) {
            if (NetworkUtil.isOffline()) {
                removeFavoriteOffline(currentAlbum)
            } else {
                removeFavoriteOnline(currentAlbum)
            }
        } else {
            if (NetworkUtil.isOffline()) {
                setFavoriteOffline(currentAlbum)
            } else {
                setFavoriteOnline(currentAlbum)
            }
        }
    }

    private fun removeFavoriteOffline(album: AlbumID3) {
        val albumId = album.id ?: return
        favoriteRepository.starLater(null, albumId, null, false)
        _album.value = album.apply { starred = null }
    }

    private fun removeFavoriteOnline(album: AlbumID3) {
        val albumId = album.id ?: return
        favoriteRepository.unstar(null, albumId, null, object : StarCallback {
            override fun onSuccess() {
                _album.value = album.apply { starred = null }
            }
            override fun onError() {
                favoriteRepository.starLater(null, albumId, null, false)
            }
        })
    }

    private fun setFavoriteOffline(album: AlbumID3) {
        val albumId = album.id ?: return
        favoriteRepository.starLater(null, albumId, null, true)
        _album.value = album.apply { starred = Date() }
    }

    private fun setFavoriteOnline(album: AlbumID3) {
        val albumId = album.id ?: return
        favoriteRepository.star(null, albumId, null, object : StarCallback {
            override fun onSuccess() {
                _album.value = album.apply { starred = Date() }
            }
            override fun onError() {
                favoriteRepository.starLater(null, albumId, null, true)
            }
        })
    }

    fun getArtist(): Flow<ArtistID3?> {
        val id = _album.value?.artistId ?: return flowOf(null)
        return artistRepository.getArtistInfo(id).asFlow()
    }
}
