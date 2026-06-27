package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.interfaces.StarCallback
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.FavoriteRepository
import com.cappielloantonio.tempo.repository.PlaylistRepository
import com.cappielloantonio.tempo.repository.SharingRepository
import com.cappielloantonio.tempo.repository.SongRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Share
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.NetworkUtil
import com.cappielloantonio.tempo.util.Preferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class SongBottomSheetUiState(
    val song: Child? = null,
    val isLoading: Boolean = false
)

@UnstableApi
class SongBottomSheetViewModel(
    private val songRepository: SongRepository,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
    private val favoriteRepository: FavoriteRepository,
    private val sharingRepository: SharingRepository,
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {

    sealed interface Action {
        data class RequestDownload(val media: Child) : Action
    }

    enum class PlaylistRemovalResult {
        Success,
        Failure,
        AllSkipped,
    }

    private val _song = MutableStateFlow<Child?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _actions = Channel<Action>(Channel.BUFFERED)
    private var startedSongId: String? = null

    val actions = _actions.receiveAsFlow()

    val uiState: StateFlow<SongBottomSheetUiState> = combine(
        _song,
        _isLoading
    ) { song, loading ->
        SongBottomSheetUiState(song, loading)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SongBottomSheetUiState()
    )

    fun onStart(song: Child) {
        if (startedSongId == song.id && _song.value?.id == song.id) {
            return
        }
        startedSongId = song.id
        _song.value = song
    }

    fun setFavorite() {
        val song = _song.value ?: return

        if (song.starred != null) {
            if (NetworkUtil.isOffline()) {
                removeFavoriteOffline(song)
            } else {
                removeFavoriteOnline(song)
            }
        } else {
            if (NetworkUtil.isOffline()) {
                setFavoriteOffline(song)
            } else {
                setFavoriteOnline(song)
            }
        }
    }

    fun getAlbum(): Flow<AlbumID3?> {
        val albumId = _song.value?.albumId
        return if (albumId.isNullOrEmpty()) {
            flowOf(null)
        } else {
            albumRepository.getAlbum(albumId).asFlow()
        }
    }

    fun getArtist(): Flow<ArtistID3?> {
        val artistId = _song.value?.artistId
        return if (artistId.isNullOrEmpty()) {
            flowOf(null)
        } else {
            artistRepository.getArtist(artistId).asFlow()
        }
    }

    fun getInstantMix(count: Int = 30): Flow<List<Child>> {
        val songId = _song.value?.id
        return if (songId.isNullOrEmpty()) {
            flowOf(emptyList())
        } else {
            songRepository.getInstantMix(songId, Constants.SeedType.TRACK, count).asFlow()
        }
    }

    suspend fun removeFromPlaylist(playlistId: String, index: Int): PlaylistRemovalResult =
        suspendCoroutine { continuation ->
            playlistRepository.removeSongFromPlaylist(
                playlistId,
                index,
                object : PlaylistRepository.AddToPlaylistCallback {
                    override fun onSuccess() {
                        continuation.resume(PlaylistRemovalResult.Success)
                    }

                    override fun onFailure() {
                        continuation.resume(PlaylistRemovalResult.Failure)
                    }

                    override fun onAllSkipped() {
                        continuation.resume(PlaylistRemovalResult.AllSkipped)
                    }
                }
            )
        }

    suspend fun shareTrack(): Share? {
        val song = _song.value ?: return null
        return sharingRepository.createShare(song.id, song.title, null).asFlow().first()
    }

    private fun removeFavoriteOffline(song: Child) {
        favoriteRepository.starLater(song.id, null, null, false)
        song.starred = null
        _song.update { song }
    }

    private fun removeFavoriteOnline(song: Child) {
        favoriteRepository.unstar(song.id, null, null, object : StarCallback {
            override fun onSuccess() = Unit

            override fun onError() {
                favoriteRepository.starLater(song.id, null, null, false)
            }
        })

        song.starred = null
        _song.update { song }
    }

    private fun setFavoriteOffline(song: Child) {
        favoriteRepository.starLater(song.id, null, null, true)
        song.starred = Date()
        _song.update { song }
    }

    private fun setFavoriteOnline(song: Child) {
        favoriteRepository.star(song.id, null, null, object : StarCallback {
            override fun onSuccess() = Unit

            override fun onError() {
                favoriteRepository.starLater(song.id, null, null, true)
            }
        })

        song.starred = Date()
        _song.update { song }

        if (Preferences.isStarredSyncEnabled() && Preferences.getDownloadDirectoryUri() == null) {
            viewModelScope.launch {
                _actions.send(Action.RequestDownload(song))
            }
        }
    }
}
