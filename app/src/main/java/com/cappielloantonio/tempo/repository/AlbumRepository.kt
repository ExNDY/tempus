package com.cappielloantonio.tempo.repository

import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.interfaces.DecadesCallback
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.AlbumInfo
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.Constants.SeedType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

@UnstableApi
class AlbumRepository {
    private val subsonicRepository: SubsonicRepository = App.get(SubsonicRepository::class.java)

    fun getAlbums(type: String, size: Int, fromYear: Int?, toYear: Int?): MutableLiveData<List<AlbumID3>> {
        val listLiveAlbums = MutableLiveData<List<AlbumID3>>(ArrayList())

        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getAlbumList2(type, size, 0, fromYear, toYear)
            if (response?.albumList2?.albums != null) {
                listLiveAlbums.postValue(response.albumList2?.albums)
            } else {
                Log.e("AlbumRepository", "API Error on getAlbums")
                listLiveAlbums.postValue(ArrayList())
            }
        }

        return listLiveAlbums
    }

    fun getStarredAlbums(random: Boolean, size: Int): MutableLiveData<List<AlbumID3>> {
        val starredAlbums = MutableLiveData<List<AlbumID3>>(ArrayList())

        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getStarred2()
            if (response?.starred2?.albums != null) {
                val albums = response.starred2?.albums?.toMutableList() ?: mutableListOf()
                if (random) {
                    albums.shuffle()
                    starredAlbums.postValue(albums.subList(0, Math.min(size, albums.size)))
                } else {
                    starredAlbums.postValue(albums)
                }
            }
        }

        return starredAlbums
    }

    fun setRating(id: String, rating: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            subsonicRepository.setRating(id, rating)
        }
    }

    fun getAlbumTracks(id: String): MutableLiveData<List<Child>> {
        val albumTracks = MutableLiveData<List<Child>>()

        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getAlbum(id)
            val tracks = response?.album?.songs ?: emptyList()
            albumTracks.postValue(tracks)
        }

        return albumTracks
    }

    fun getArtistAlbums(id: String): MutableLiveData<List<AlbumID3>> {
        val artistsAlbum = MutableLiveData<List<AlbumID3>>(ArrayList())

        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getArtist(id)
            if (response?.artist?.albums != null) {
                val albums = response.artist?.albums?.toMutableList() ?: mutableListOf()
                albums.sortWith(compareBy { it.year })
                albums.reverse()
                artistsAlbum.postValue(albums)
            }
        }

        return artistsAlbum
    }

    fun getAlbum(id: String): MutableLiveData<AlbumID3> {
        val album = MutableLiveData<AlbumID3>()

        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getAlbum(id)
            album.postValue(response?.album)
        }

        return album
    }

    fun getAlbumInfo(id: String): MutableLiveData<AlbumInfo> {
        val albumInfo = MutableLiveData<AlbumInfo>()

        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getAlbumInfo2(id)
            albumInfo.postValue(response?.albumInfo)
        }

        return albumInfo
    }

    fun getInstantMix(album: AlbumID3, count: Int): MutableLiveData<List<Child>> {
        return SongRepository().getInstantMix(album.id ?: "", SeedType.TRACK, count)
    }

    fun getDecades(): MutableLiveData<List<Int>> {
        val decades = MutableLiveData<List<Int>>()

        getFirstAlbum(object : DecadesCallback {
            override fun onLoadYear(first: Int) {
                getLastAlbum(object : DecadesCallback {
                    override fun onLoadYear(last: Int) {
                        if (first != -1 && last != -1) {
                            val decadeList = ArrayList<Int>()
                            var startDecade = first - first % 10
                            val lastDecade = last - last % 10

                            while (startDecade <= lastDecade) {
                                decadeList.add(startDecade)
                                startDecade += 10
                            }
                            decades.postValue(decadeList)
                        }
                    }
                })
            }
        })

        return decades
    }

    private fun getFirstAlbum(callback: DecadesCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getAlbumList2("byYear", 1, 0, 1900, Calendar.getInstance().get(Calendar.YEAR))
            val albums = response?.albumList2?.albums
            withContext(Dispatchers.Main) {
                if (!albums.isNullOrEmpty()) {
                    callback.onLoadYear(albums[0].year)
                } else {
                    callback.onLoadYear(-1)
                }
            }
        }
    }

    private fun getLastAlbum(callback: DecadesCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getAlbumList2("byYear", 1, 0, Calendar.getInstance().get(Calendar.YEAR), 1900)
            val albums = response?.albumList2?.albums
            withContext(Dispatchers.Main) {
                if (!albums.isNullOrEmpty()) {
                    callback.onLoadYear(albums[0].year)
                } else {
                    callback.onLoadYear(-1)
                }
            }
        }
    }
}
