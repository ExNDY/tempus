package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.interfaces.MediaCallback
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
import com.cappielloantonio.tempo.util.Constants.SeedType
import com.cappielloantonio.tempo.util.NetworkUtil
import com.cappielloantonio.tempo.util.Preferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.Collections
import java.util.Date

@OptIn(UnstableApi::class)
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

    private var song: Child? = null
    private val instantMix = MutableLiveData<List<Child>?>(null)
    private val _actions = Channel<Action>(Channel.BUFFERED)
    val actions: LiveData<Action> = _actions.receiveAsFlow().asLiveData()

    fun getSong(): Child? = song

    fun setSong(song: Child?) {
        this.song = song
    }

    fun removeFromPlaylist(playlistId: String, index: Int, callback: PlaylistRepository.AddToPlaylistCallback?) {
        playlistRepository.removeSongFromPlaylist(playlistId, index, callback)
    }

    fun setFavorite() {
        val currentSong = song ?: return
        if (currentSong.starred != null) {
            if (NetworkUtil.isOffline()) {
                removeFavoriteOffline(currentSong)
            } else {
                removeFavoriteOnline(currentSong)
            }
        } else {
            if (NetworkUtil.isOffline()) {
                setFavoriteOffline(currentSong)
            } else {
                setFavoriteOnline(currentSong)
            }
        }
    }

    private fun removeFavoriteOffline(media: Child) {
        favoriteRepository.starLater(media.id, null, null, false)
        media.starred = null
    }

    private fun removeFavoriteOnline(media: Child) {
        favoriteRepository.unstar(media.id, null, null, object : StarCallback {
            override fun onError() {
                favoriteRepository.starLater(media.id, null, null, false)
            }
        })

        media.starred = null
    }

    private fun setFavoriteOffline(media: Child) {
        favoriteRepository.starLater(media.id, null, null, true)
        media.starred = Date()
    }

    private fun setFavoriteOnline(media: Child) {
        favoriteRepository.star(media.id, null, null, object : StarCallback {
            override fun onError() {
                favoriteRepository.starLater(media.id, null, null, true)
            }
        })

        media.starred = Date()

        if (Preferences.isStarredSyncEnabled() && Preferences.getDownloadDirectoryUri() == null) {
            sendAction(_actions, Action.RequestDownload(media))
        }
    }

    fun getAlbum(): LiveData<AlbumID3> {
        return albumRepository.getAlbum(song?.albumId ?: "")
    }

    fun getArtist(): LiveData<ArtistID3> {
        return artistRepository.getArtist(song?.artistId ?: "")
    }

    fun getInstantMix(owner: LifecycleOwner, media: Child): LiveData<List<Child>?> {
        instantMix.value = Collections.emptyList()
        songRepository.getInstantMix(media.id, SeedType.TRACK, 30).observe(owner) { instantMix.postValue(it) }
        return instantMix
    }

    fun getInstantMix(media: Child, count: Int, callback: MediaCallback) {
        songRepository.getInstantMix(media.id, SeedType.TRACK, count) { songs ->
            if (songs.isNotEmpty()) {
                callback.onLoadMedia(songs)
            } else {
                callback.onLoadMedia(emptyList<Child>())
            }
        }
    }

    fun shareTrack(): MutableLiveData<Share?> {
        val currentSong = song ?: return MutableLiveData()
        return sharingRepository.createShare(currentSong.id, currentSong.title, null)
    }
}
