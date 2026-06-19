package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.interfaces.StarCallback
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
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.Collections
import java.util.Date

@OptIn(UnstableApi::class)
class AlbumBottomSheetViewModel(
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
    private val favoriteRepository: FavoriteRepository,
    private val sharingRepository: SharingRepository,
) : ViewModel() {
    sealed interface Action {
        data class RequestDownload(val songs: List<Child>) : Action
    }

    private var album: AlbumID3? = null
    private val instantMix = MutableLiveData<List<Child>?>(null)
    private val _actions = Channel<Action>(Channel.BUFFERED)
    val actions: LiveData<Action> = _actions.receiveAsFlow().asLiveData()

    fun getAlbum(): AlbumID3? = album

    fun setAlbum(album: AlbumID3?) {
        this.album = album
    }

    fun getArtist(): LiveData<ArtistID3> {
        return artistRepository.getArtist(album?.artistId ?: "")
    }

    fun getAlbumTracks(): MutableLiveData<List<Child>> {
        return albumRepository.getAlbumTracks(album?.id ?: "")
    }

    fun setFavorite() {
        val currentAlbum = album ?: return
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

    fun shareAlbum(): MutableLiveData<Share?> {
        val currentAlbum = album ?: return MutableLiveData()
        val albumId = currentAlbum.id ?: return MutableLiveData()
        return sharingRepository.createShare(albumId, currentAlbum.name, null)
    }

    private fun removeFavoriteOffline(album: AlbumID3) {
        val albumId = album.id ?: return
        favoriteRepository.starLater(null, albumId, null, false)
        album.starred = null
    }

    private fun removeFavoriteOnline(album: AlbumID3) {
        val albumId = album.id ?: return
        favoriteRepository.unstar(null, albumId, null, object : StarCallback {
            override fun onError() {
                favoriteRepository.starLater(null, albumId, null, false)
            }
        })

        album.starred = null
    }

    private fun setFavoriteOffline(album: AlbumID3) {
        val albumId = album.id ?: return
        favoriteRepository.starLater(null, albumId, null, true)
        album.starred = Date()
    }

    private fun setFavoriteOnline(album: AlbumID3) {
        val albumId = album.id ?: return
        favoriteRepository.star(null, albumId, null, object : StarCallback {
            override fun onError() {
                favoriteRepository.starLater(null, albumId, null, true)
            }
        })

        album.starred = Date()
        if (Preferences.isStarredAlbumsSyncEnabled()) {
            val albumTracks = albumRepository.getAlbumTracks(albumId)
            val albumTracksObserver = object : Observer<List<Child>> {
                override fun onChanged(songs: List<Child>) {
                    if (songs.isNotEmpty()) {
                        sendAction(_actions, Action.RequestDownload(songs))
                    }
                    albumTracks.removeObserver(this)
                }
            }
            albumTracks.observeForever(albumTracksObserver)
        }
    }

    fun getAlbumInstantMix(owner: LifecycleOwner, album: AlbumID3): LiveData<List<Child>?> {
        instantMix.value = Collections.emptyList()
        albumRepository.getInstantMix(album, 30).observe(owner) { instantMix.postValue(it) }
        return instantMix
    }
}
