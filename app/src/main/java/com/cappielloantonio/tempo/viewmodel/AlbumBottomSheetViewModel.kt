package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Date

data class AlbumBottomSheetUiState(
    val album: AlbumID3? = null,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
)

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
    private var loadJob: Job? = null
    val actions = _actions.receiveAsFlow()
    private val _hasError = MutableStateFlow(false)
    val uiState: StateFlow<AlbumBottomSheetUiState> = combine(
        _album,
        _isLoading,
        _hasError,
    ) { album, loading, hasError ->
        AlbumBottomSheetUiState(album, loading, hasError)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AlbumBottomSheetUiState()
    )

    fun onStart(albumId: String, force: Boolean = false) {
        if (!force && startedAlbumId == albumId && (_album.value != null || _isLoading.value)) return
        startedAlbumId = albumId
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _album.value = null
            _hasError.value = false
            _isLoading.value = true
            if (albumId.isBlank()) {
                _isLoading.value = false
                _hasError.value = true
                return@launch
            }
            val album = runCatching {
                albumRepository.getAlbum(albumId).asFlow().first()
            }.getOrNull()
            _album.value = album
            _isLoading.value = false
            _hasError.value = album == null
        }
    }

    fun retry(albumId: String) = onStart(albumId, force = true)
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
        _album.update { album.withStarred(null) }
    }

    private fun removeFavoriteOnline(album: AlbumID3) {
        favoriteRepository.unstar(null, album.id, null, object : StarCallback {
            override fun onSuccess() = Unit
            override fun onError() {
                favoriteRepository.starLater(null, album.id, null, false)
            }
        })
        _album.update { album.withStarred(null) }
    }

    private fun setFavoriteOffline(album: AlbumID3) {
        favoriteRepository.starLater(null, album.id, null, true)
        _album.update { album.withStarred(Date()) }
    }

    private fun setFavoriteOnline(album: AlbumID3) {
        favoriteRepository.star(null, album.id, null, object : StarCallback {
            override fun onSuccess() = Unit
            override fun onError() {
                favoriteRepository.starLater(null, album.id, null, true)
            }
        })
        _album.update { album.withStarred(Date()) }
        if (Preferences.isStarredAlbumsSyncEnabled()) {
            viewModelScope.launch {
                val songs = getAlbumTracks().first()
                if (songs.isNotEmpty()) {
                    _actions.send(Action.RequestDownloads(songs))
                }
            }
        }
    }

    private fun AlbumID3.withStarred(starred: Date?): AlbumID3 {
        return AlbumID3(
            id = id,
            name = name,
            artist = artist,
            artistId = artistId,
            coverArtId = coverArtId,
            songCount = songCount,
            duration = duration,
            playCount = playCount,
            created = created,
            starred = starred,
            year = year,
            genre = genre,
            played = played,
            userRating = userRating,
            recordLabels = recordLabels,
            musicBrainzId = musicBrainzId,
            genres = genres,
            artists = artists,
            displayArtist = displayArtist,
            releaseTypes = releaseTypes,
            moods = moods,
            sortName = sortName,
            originalReleaseDate = originalReleaseDate,
            releaseDate = releaseDate,
            isCompilation = isCompilation,
            discTitles = discTitles,
        )
    }
}
