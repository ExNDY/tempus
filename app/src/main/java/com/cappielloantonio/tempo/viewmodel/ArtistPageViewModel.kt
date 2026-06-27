package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.asFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.interfaces.StarCallback
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.FavoriteRepository
import com.cappielloantonio.tempo.subsonic.models.*
import com.cappielloantonio.tempo.util.NetworkUtil
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class ArtistPageUiState(
    val artist: ArtistID3? = null,
    val artistInfo: ArtistInfo2? = null,
    val topSongs: List<Child> = emptyList(),
    val albums: Map<String, List<AlbumID3>> = emptyMap(),
    val isLoading: Boolean = true
)

@UnstableApi
class ArtistPageViewModel(
    private val artistRepository: ArtistRepository,
    private val albumRepository: AlbumRepository,
    private val favoriteRepository: FavoriteRepository,
) : ViewModel() {

    private val _artist = MutableStateFlow<ArtistID3?>(null)
    private val _artistInfo = MutableStateFlow<ArtistInfo2?>(null)
    private val _topSongs = MutableStateFlow<List<Child>>(emptyList())
    private val _albums = MutableStateFlow<Map<String, List<AlbumID3>>>(emptyMap())
    private val _isLoading = MutableStateFlow(true)
    private var startedArtistId: String? = null

    val uiState: StateFlow<ArtistPageUiState> = combine(
        _artist, _artistInfo, _topSongs, _albums, _isLoading
    ) { artist, info, songs, albums, loading ->
        ArtistPageUiState(artist, info, songs, albums, loading)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ArtistPageUiState()
    )

    fun onStart(artist: ArtistID3) {
        if (startedArtistId == artist.id && _artist.value?.id == artist.id) return
        startedArtistId = artist.id
        _artist.value = artist
        loadData(artist)
    }

    private fun loadData(artist: ArtistID3) {
        viewModelScope.launch {
            _isLoading.value = true
            
            artistRepository.getArtistInfo(artist.id ?: "").observeForever { _artist.value = it }
            artistRepository.getArtistFullInfo(artist.id ?: "").observeForever { _artistInfo.value = it }
            artistRepository.getTopSongs(artist.name ?: "", 20).observeForever { _topSongs.value = it ?: emptyList() }
            
            albumRepository.getArtistAlbums(artist.id ?: "").observeForever { albumList ->
                if (albumList != null) {
                    val grouped = albumList.groupBy { it.releaseTypes?.firstOrNull() ?: "album" }
                    _albums.value = grouped
                }
                _isLoading.value = false
            }
        }
    }

    fun setFavorite() {
        val currentArtist = _artist.value ?: return

        if (currentArtist.starred != null) {
            if (NetworkUtil.isOffline()) {
                removeFavoriteOffline(currentArtist)
            } else {
                removeFavoriteOnline(currentArtist)
            }
        } else {
            if (NetworkUtil.isOffline()) {
                setFavoriteOffline(currentArtist)
            } else {
                setFavoriteOnline(currentArtist)
            }
        }
    }

    private fun removeFavoriteOffline(artist: ArtistID3) {
        favoriteRepository.starLater(null, null, artist.id, false)
        _artist.value = artist.apply { starred = null }
    }

    private fun removeFavoriteOnline(artist: ArtistID3) {
        val artistId = artist.id ?: return
        favoriteRepository.unstar(null, null, artistId, object : StarCallback {
            override fun onSuccess() {
                _artist.value = artist.apply { starred = null }
            }
            override fun onError() {
                favoriteRepository.starLater(null, null, artistId, false)
            }
        })
    }

    private fun setFavoriteOffline(artist: ArtistID3) {
        favoriteRepository.starLater(null, null, artist.id, true)
        _artist.value = artist.apply { starred = Date() }
    }

    private fun setFavoriteOnline(artist: ArtistID3) {
        val artistId = artist.id ?: return
        favoriteRepository.star(null, null, artistId, object : StarCallback {
            override fun onSuccess() {
                _artist.value = artist.apply { starred = Date() }
            }
            override fun onError() {
                favoriteRepository.starLater(null, null, artistId, true)
            }
        })
    }

    fun getArtistInstantMix(): Flow<List<Child>> {
        val artist = _artist.value ?: return flowOf(emptyList())
        return artistRepository.getInstantMix(artist, 30).asFlow()
    }
}
