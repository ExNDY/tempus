package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.interfaces.StarCallback
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.FavoriteRepository
import com.cappielloantonio.tempo.repository.SharingRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Share
import com.cappielloantonio.tempo.util.NetworkUtil
import com.cappielloantonio.tempo.util.Preferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

data class AlbumBottomSheetUiState(
    val album: AlbumID3? = null,
    val isLoading: Boolean = false
)

@UnstableApi
class AlbumBottomSheetViewModel(
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
    private val favoriteRepository: FavoriteRepository,
    private val sharingRepository: SharingRepository,
) : ViewModel() {

    sealed interface Action {
        data class RequestDownloads(val songs: List<Child>) : Action
    }

    private val _album = MutableStateFlow<AlbumID3?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _actions = Channel<Action>(Channel.BUFFERED)
    private var startedAlbumId: String? = null

    val actions = _actions.receiveAsFlow()

    val uiState: StateFlow<AlbumBottomSheetUiState> = combine(
        _album,
        _isLoading
    ) { album, loading ->
        AlbumBottomSheetUiState(album, loading)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AlbumBottomSheetUiState()
    )

    fun onStart(album: AlbumID3) {
        if (startedAlbumId == album.id && _album.value?.id == album.id) {
            return
        }
        startedAlbumId = album.id
        _album.value = album
    }

    fun getArtist(): Flow<ArtistID3?> {
        val artistId = _album.value?.artistId
        return if (artistId.isNullOrEmpty()) {
            flowOf(null)
        } else {
            artistRepository.getArtist(artistId).asFlow()
        }
    }

    fun getAlbumTracks(): Flow<List<Child>> {
        val albumId = _album.value?.id
        return if (albumId.isNullOrEmpty()) {
            flowOf(emptyList())
        } else {
            albumRepository.getAlbumTracks(albumId).asFlow()
        }
    }

    fun getAlbumInstantMix(count: Int = 30): Flow<List<Child>> {
        val album = _album.value ?: return flowOf(emptyList())
        return albumRepository.getInstantMix(album, count).asFlow()
    }

    fun setFavorite() {
        val album = _album.value ?: return
        if (album.starred != null) {
            if (NetworkUtil.isOffline()) {
                removeFavoriteOffline(album)
            } else {
                removeFavoriteOnline(album)
            }
        } else {
            if (NetworkUtil.isOffline()) {
                setFavoriteOffline(album)
            } else {
                setFavoriteOnline(album)
            }
        }
    }

    suspend fun shareAlbum(): Share? {
        val album = _album.value ?: return null
        val albumId = album.id ?: return null
        return sharingRepository.createShare(albumId, album.name, null).asFlow().first()
    }

    private fun removeFavoriteOffline(album: AlbumID3) {
        favoriteRepository.starLater(null, album.id, null, false)
        album.starred = null
        _album.update { album }
    }

    private fun removeFavoriteOnline(album: AlbumID3) {
        favoriteRepository.unstar(null, album.id, null, object : StarCallback {
            override fun onSuccess() = Unit

            override fun onError() {
                favoriteRepository.starLater(null, album.id, null, false)
            }
        })
        album.starred = null
        _album.update { album }
    }

    private fun setFavoriteOffline(album: AlbumID3) {
        favoriteRepository.starLater(null, album.id, null, true)
        album.starred = Date()
        _album.update { album }
    }

    private fun setFavoriteOnline(album: AlbumID3) {
        favoriteRepository.star(null, album.id, null, object : StarCallback {
            override fun onSuccess() = Unit

            override fun onError() {
                favoriteRepository.starLater(null, album.id, null, true)
            }
        })

        album.starred = Date()
        _album.update { album }

        if (Preferences.isStarredAlbumsSyncEnabled()) {
            viewModelScope.launch {
                val songs = getAlbumTracks().first()
                if (songs.isNotEmpty()) {
                    _actions.send(Action.RequestDownloads(songs))
                }
            }
        }
    }
}
