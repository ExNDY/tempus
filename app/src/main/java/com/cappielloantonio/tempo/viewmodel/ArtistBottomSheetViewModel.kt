package com.cappielloantonio.tempo.viewmodel
import androidx.lifecycle.asFlow
import androidx.lifecycle.ViewModel
import com.cappielloantonio.tempo.interfaces.StarCallback
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.FavoriteRepository
import com.cappielloantonio.tempo.repository.SharingRepository
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Share
import com.cappielloantonio.tempo.util.NetworkUtil
import com.cappielloantonio.tempo.util.Preferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import java.util.Date
data class ArtistBottomSheetUiState(
    val artist: ArtistID3? = null,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
)
class ArtistBottomSheetViewModel(
    private val artistRepository: ArtistRepository,
    private val favoriteRepository: FavoriteRepository,
    private val sharingRepository: SharingRepository,
) : ViewModel() {
    sealed interface Action {
        data class RequestDownloads(val songs: List<Child>) : Action
    }
    private val _artist = MutableStateFlow<ArtistID3?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _actions = Channel<Action>(Channel.BUFFERED)
    private var startedArtistId: String? = null
    private var loadJob: Job? = null
    val actions = _actions.receiveAsFlow()
    private val _hasError = MutableStateFlow(false)
    val uiState: StateFlow<ArtistBottomSheetUiState> = combine(
        _artist, _isLoading, _hasError
    ) { artist, loading, hasError ->
        ArtistBottomSheetUiState(artist, loading, hasError)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ArtistBottomSheetUiState()
    )
    fun onStart(artistId: String, force: Boolean = false) {
        if (!force && startedArtistId == artistId && (_artist.value != null || _isLoading.value)) return
        startedArtistId = artistId
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _artist.value = null
            _hasError.value = false
            _isLoading.value = true
            if (artistId.isBlank()) {
                _isLoading.value = false
                _hasError.value = true
                return@launch
            }
            val artist = runCatching {
                artistRepository.getArtist(artistId).asFlow().first()
            }.getOrNull()
            _artist.value = artist
            _isLoading.value = false
            _hasError.value = artist == null
        }
    }
    fun retry(artistId: String) = onStart(artistId, force = true)
    fun setFavorite() {
        val artist = _artist.value ?: return
        if (artist.starred != null) {
            if (NetworkUtil.isOffline()) {
                removeFavoriteOffline(artist)
            } else {
                removeFavoriteOnline(artist)
            }
        } else {
            if (NetworkUtil.isOffline()) {
                setFavoriteOffline(artist)
            } else {
                setFavoriteOnline(artist)
            }
        }
    }
    fun getArtistInstantMix(count: Int = 30): Flow<List<Child>> {
        val artist = _artist.value ?: return flowOf(emptyList())
        return artistRepository.getInstantMix(artist, count).asFlow()
    }
    fun getRandomSongs(count: Int = 50): Flow<List<Child>> {
        val artist = _artist.value ?: return flowOf(emptyList())
        return artistRepository.getRandomSong(artist, count).asFlow()
    }
    fun getAllSongs(): Flow<List<Child>> {
        val artistId = _artist.value?.id ?: return flowOf(emptyList())
        return callbackFlow {
            artistRepository.getArtistAllSongs(artistId) { songs ->
                trySend(songs)
                close()
            }
            awaitClose {}
        }
    }
    suspend fun shareArtist(): Share? {
        val artist = _artist.value ?: return null
        val artistId = artist.id ?: return null
        return sharingRepository.createShare(artistId, artist.name, null).asFlow().first()
    }
    private fun removeFavoriteOffline(artist: ArtistID3) {
        favoriteRepository.starLater(null, null, artist.id, false)
        _artist.update { artist.withStarred(null) }
    }
    private fun removeFavoriteOnline(artist: ArtistID3) {
        val artistId = artist.id ?: return
        favoriteRepository.unstar(null, null, artistId, object : StarCallback {
            override fun onSuccess() = Unit
            override fun onError() {
                favoriteRepository.starLater(null, null, artistId, false)
            }
        })
        _artist.update { artist.withStarred(null) }
    }
    private fun setFavoriteOffline(artist: ArtistID3) {
        favoriteRepository.starLater(null, null, artist.id, true)
        _artist.update { artist.withStarred(Date()) }
    }
    private fun setFavoriteOnline(artist: ArtistID3) {
        val artistId = artist.id ?: return
        favoriteRepository.star(null, null, artistId, object : StarCallback {
            override fun onSuccess() = Unit
            override fun onError() {
                favoriteRepository.starLater(null, null, artistId, true)
            }
        })
        _artist.update { artist.withStarred(Date()) }
        if (Preferences.isStarredArtistsSyncEnabled()) {
            artistRepository.getArtistAllSongs(artistId) { songs ->
                if (songs.isNotEmpty()) {
                    viewModelScope.launch {
                        _actions.send(Action.RequestDownloads(songs))
                    }
                }
            }
        }
    }
    private fun ArtistID3.withStarred(starred: Date?): ArtistID3 {
        return ArtistID3(
            id = id,
            name = name,
            coverArtId = coverArtId,
            albumCount = albumCount,
            starred = starred,
        )
    }
}
