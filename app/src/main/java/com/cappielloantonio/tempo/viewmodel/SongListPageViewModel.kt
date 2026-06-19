package com.cappielloantonio.tempo.viewmodel

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.SongRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Genre
import com.cappielloantonio.tempo.util.Constants
import java.util.ArrayList

class SongListPageViewModel(
    private val songRepository: SongRepository,
    private val artistRepository: ArtistRepository
) : ViewModel() {

    @JvmField
    var title: String? = null
    @JvmField
    var toolbarTitle: String? = null
    @JvmField
    var genre: Genre? = null
    @JvmField
    var artist: ArtistID3? = null
    @JvmField
    var album: AlbumID3? = null

    private var songList = MutableLiveData<List<Child>>(arrayListOf())

    @JvmField
    var filters: ArrayList<String> = arrayListOf()
    @JvmField
    var filterNames: ArrayList<String> = arrayListOf()

    @JvmField
    var year = 0
    @JvmField
    var maxNumberByYear = 500
    @JvmField
    var maxNumberByGenre = 500

    fun getSongList(): LiveData<List<Child>> {
        songList = MutableLiveData(arrayListOf())

        when (title) {
            Constants.MEDIA_BY_GENRE -> songList = songRepository.getRandomSampleWithGenre(maxNumberByGenre, 0, 3000, genre?.genre.orEmpty())
            Constants.MEDIA_BY_ARTIST -> songList = artistRepository.getTopSongs(artist?.name.orEmpty(), 50)
            Constants.MEDIA_BY_GENRES -> songList = songRepository.getSongsByGenres(filters)
            Constants.MEDIA_BY_YEAR -> songList = songRepository.getRandomSample(maxNumberByYear, year, year + 10)
            Constants.MEDIA_STARRED -> songList = songRepository.getStarredSongs(false, -1)
        }

        return songList
    }

    fun getSongsByPage(owner: androidx.lifecycle.LifecycleOwner) {
        when (title) {
            Constants.MEDIA_BY_GENRE -> {
                val songCount = songList.value?.size ?: 0
                if (songCount > 0 && songCount % maxNumberByGenre != 0) return

                val page = songCount / maxNumberByGenre
                songRepository.getSongsByGenre(genre?.genre.orEmpty(), page).observe(owner) { children ->
                    if (!children.isNullOrEmpty()) {
                        val currentMedia = songList.value?.toMutableList() ?: mutableListOf()
                        currentMedia.addAll(children)
                        songList.value = currentMedia
                    }
                }
            }
            Constants.MEDIA_BY_ARTIST,
            Constants.MEDIA_BY_GENRES,
            Constants.MEDIA_BY_YEAR,
            Constants.MEDIA_STARRED -> Unit
        }
    }

    fun getFiltersTitle(): String {
        return TextUtils.join(", ", filterNames)
    }
}
