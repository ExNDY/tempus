package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cappielloantonio.tempo.interfaces.StarCallback
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.FavoriteRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.AlbumInfo
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.NetworkUtil
import java.util.Date

class AlbumPageViewModel(
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
    private val favoriteRepository: FavoriteRepository,
) : ViewModel() {
    private var albumId: String? = null
    private var artistId: String? = null
    private val album = MutableLiveData<AlbumID3?>(null)

    fun getAlbumSongLiveList(): LiveData<List<Child>> {
        return albumRepository.getAlbumTracks(albumId ?: "")
    }

    fun getAlbum(): MutableLiveData<AlbumID3?> = album

    fun setAlbum(owner: androidx.lifecycle.LifecycleOwner, album: AlbumID3) {
        albumId = album.id
        this.album.postValue(album)
        artistId = album.artistId

        albumRepository.getAlbum(album.id ?: "").observe(owner) { fetchedAlbum ->
            if (fetchedAlbum != null) this.album.value = fetchedAlbum
        }
    }

    fun setFavorite() {
        val currentAlbum = album.value ?: return

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
        album.starred = null
        this.album.postValue(album)
    }

    private fun removeFavoriteOnline(album: AlbumID3) {
        val albumId = album.id ?: return
        favoriteRepository.unstar(null, albumId, null, object : StarCallback {
            override fun onError() {
                favoriteRepository.starLater(null, albumId, null, false)
            }
        })

        album.starred = null
        this.album.postValue(album)
    }

    private fun setFavoriteOffline(album: AlbumID3) {
        val albumId = album.id ?: return
        favoriteRepository.starLater(null, albumId, null, true)
        album.starred = Date()
        this.album.postValue(album)
    }

    private fun setFavoriteOnline(album: AlbumID3) {
        val albumId = album.id ?: return
        favoriteRepository.star(null, albumId, null, object : StarCallback {
            override fun onError() {
                favoriteRepository.starLater(null, albumId, null, true)
            }
        })

        album.starred = Date()
        this.album.postValue(album)
    }

    fun getArtist(): LiveData<ArtistID3> {
        return artistRepository.getArtistInfo(artistId ?: "")
    }

    fun getAlbumInfo(): LiveData<AlbumInfo> {
        return albumRepository.getAlbumInfo(albumId ?: "")
    }
}
