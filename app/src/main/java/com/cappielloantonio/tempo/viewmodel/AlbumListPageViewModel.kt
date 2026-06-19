package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.util.Constants
import java.util.Calendar
import java.util.Comparator

class AlbumListPageViewModel(
    private val albumRepository: AlbumRepository
) : ViewModel() {

    @JvmField
    var title: String? = null
    @JvmField
    var artist: ArtistID3? = null
    @JvmField
    var albums: List<AlbumID3>? = null

    private var albumList = MutableLiveData<List<AlbumID3>>(arrayListOf())
    @JvmField
    var maxNumber: Int = 500

    fun getAlbumList(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<AlbumID3>> {
        if (albums != null) {
            return MutableLiveData(albums)
        }
        albumList = MutableLiveData(arrayListOf())

        when (title) {
            Constants.ALBUM_RECENTLY_PLAYED -> albumRepository.getAlbums("recent", maxNumber, null, null).observe(owner) { albums ->
                albumList.value = albums
            }
            Constants.ALBUM_MOST_PLAYED -> albumRepository.getAlbums("frequent", maxNumber, null, null).observe(owner) { albums ->
                albumList.value = albums
            }
            Constants.ALBUM_RECENTLY_ADDED -> albumRepository.getAlbums("newest", maxNumber, null, null).observe(owner) { albums ->
                albumList.value = albums
            }
            Constants.ALBUM_STARRED -> albumList = albumRepository.getStarredAlbums(false, -1)
            Constants.ALBUM_NEW_RELEASES -> {
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                albumRepository.getAlbums("byYear", maxNumber, currentYear, currentYear).observe(owner) { albums ->
                    if (albums != null) {
                        val sorted = albums.sortedWith(Comparator.comparing<AlbumID3, java.util.Date?> { it.created }.reversed())
                        albumList.postValue(sorted.take(20))
                    }
                }
            }
        }

        return albumList
    }
}
