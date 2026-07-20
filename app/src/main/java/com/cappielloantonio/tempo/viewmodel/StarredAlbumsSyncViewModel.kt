package com.cappielloantonio.tempo.viewmodel

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.Child

class StarredAlbumsSyncViewModel(
    private val albumRepository: AlbumRepository
) : ViewModel() {

    private val starredAlbums = MutableLiveData<List<AlbumID3>?>(null)
    private val starredAlbumSongs = MutableLiveData<List<Child>?>(null)

    fun getStarredAlbums(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<AlbumID3>?> {
        if (starredAlbums.value == null) {
            loadStarredAlbums()
        }
        return starredAlbums
    }

    fun getAllStarredAlbumSongs(): LiveData<List<Child>?> {
        if (starredAlbumSongs.value == null) {
            loadAllStarredAlbumSongs()
        }
        return starredAlbumSongs
    }

    fun getStarredAlbumSongs(activity: Activity): LiveData<List<Child>?> {
        if (starredAlbumSongs.value == null) {
            loadAllStarredAlbumSongs()
        }
        return starredAlbumSongs
    }

    private fun loadStarredAlbums() {
        val source = albumRepository.getStarredAlbums(false, -1)
        source.observeForever(object : Observer<List<AlbumID3>> {
            override fun onChanged(value: List<AlbumID3>) {
                starredAlbums.postValue(value)
                source.removeObserver(this)
            }
        })
    }

    private fun loadAllStarredAlbumSongs() {
        val source = albumRepository.getStarredAlbums(false, -1)
        source.observeForever(object : Observer<List<AlbumID3>> {
            override fun onChanged(value: List<AlbumID3>) {
                if (value.isNotEmpty()) {
                    collectAllAlbumSongs(value)
                } else {
                    starredAlbumSongs.postValue(emptyList())
                }
                source.removeObserver(this)
            }
        })
    }

    private fun collectAllAlbumSongs(albums: List<AlbumID3>) {
        val allSongs = arrayListOf<Child>()
        var remaining = albums.size
        if (remaining == 0) {
            starredAlbumSongs.postValue(emptyList())
            return
        }

        for (album in albums) {
            val albumId = album.id
            if (albumId == null) {
                remaining--
                if (remaining == 0) {
                    starredAlbumSongs.postValue(allSongs)
                }
                continue
            }
            val albumTracks = albumRepository.getAlbumTracks(albumId)
            albumTracks.observeForever(object : Observer<List<Child>> {
                override fun onChanged(value: List<Child>) {
                    allSongs.addAll(value)
                    remaining--
                    albumTracks.removeObserver(this)
                    if (remaining == 0) {
                        starredAlbumSongs.postValue(allSongs)
                    }
                }
            })
        }
    }
}
