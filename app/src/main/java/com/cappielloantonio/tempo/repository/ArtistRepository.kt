package com.cappielloantonio.tempo.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.ArtistInfo2
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.Constants.SeedType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

@UnstableApi
class ArtistRepository {
    private val subsonicRepository: SubsonicRepository = App.get(SubsonicRepository::class.java)

    fun getArtistAllSongs(artistId: String, callback: ArtistSongsCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getArtist(artistId)
            val albums = response?.artist?.albums
            if (!albums.isNullOrEmpty()) {
                fetchAllAlbumSongsWithCallback(albums, callback)
            } else {
                withContext(Dispatchers.Main) {
                    callback.onSongsCollected(emptyList())
                }
            }
        }
    }

    private fun fetchAllAlbumSongsWithCallback(albums: List<AlbumID3>, callback: ArtistSongsCallback) {
        val allSongs = mutableListOf<Child>()
        val remainingAlbums = AtomicInteger(albums.size)
        
        for (album in albums) {
            CoroutineScope(Dispatchers.IO).launch {
                val response = album.id?.let { subsonicRepository.getAlbum(it) }
                val tracks = response?.album?.songs ?: emptyList()
                synchronized(allSongs) {
                    allSongs.addAll(tracks)
                }
                if (remainingAlbums.decrementAndGet() == 0) {
                    withContext(Dispatchers.Main) {
                        callback.onSongsCollected(allSongs)
                    }
                }
            }
        }
    }

    fun interface ArtistSongsCallback {
        fun onSongsCollected(songs: List<Child>)
    }

    fun getStarredArtists(random: Boolean, size: Int): MutableLiveData<List<ArtistID3>> {
        val starredArtists = MutableLiveData<List<ArtistID3>>()

        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getStarred2()
            val artists = response?.starred2?.artists
            if (!artists.isNullOrEmpty()) {
                val mutableArtists = artists.toMutableList()
                if (random) {
                    mutableArtists.shuffle()
                }
                val targetList = if (random) mutableArtists.take(size) else mutableArtists
                fetchArtistInfo(targetList, starredArtists)
            }
        }

        return starredArtists
    }

    fun getArtists(random: Boolean, size: Int): MutableLiveData<List<ArtistID3>> {
        val listLiveArtists = MutableLiveData<List<ArtistID3>>()

        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getArtists()
            val artists = mutableListOf<ArtistID3>()
            response?.artists?.indices?.forEach { index ->
                index.artists?.let { artists.addAll(it) }
            }

            if (random) {
                artists.shuffle()
                fetchArtistInfo(artists.take(size), listLiveArtists)
            } else {
                listLiveArtists.postValue(artists)
            }
        }

        return listLiveArtists
    }

    private fun fetchArtistInfo(artists: List<ArtistID3>, list: MutableLiveData<List<ArtistID3>>) {
        val currentList = list.value?.toMutableList() ?: mutableListOf()
        val remaining = AtomicInteger(artists.size)

        for (artist in artists) {
            CoroutineScope(Dispatchers.IO).launch {
                val response = artist.id?.let { subsonicRepository.getArtist(it) }
                response?.artist?.let {
                    synchronized(currentList) {
                        currentList.add(it)
                    }
                }
                if (remaining.decrementAndGet() == 0) {
                    list.postValue(currentList)
                }
            }
        }
    }

    fun getArtistInfo(id: String): MutableLiveData<ArtistID3> {
        val artist = MutableLiveData<ArtistID3>()
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getArtist(id)
            artist.postValue(response?.artist)
        }
        return artist
    }

    fun getArtistFullInfo(id: String): MutableLiveData<ArtistInfo2?> {
        val artistFullInfo = MutableLiveData<ArtistInfo2?>()
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getArtistInfo2(id)
            artistFullInfo.postValue(response?.artistInfo2)
        }
        return artistFullInfo
    }

    fun setRating(id: String, rating: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            subsonicRepository.setRating(id, rating)
        }
    }

    fun getArtist(id: String): MutableLiveData<ArtistID3> {
        return getArtistInfo(id)
    }

    fun getInstantMix(artist: ArtistID3, count: Int): MutableLiveData<List<Child>> {
        return SongRepository().getInstantMix(artist.id ?: "", SeedType.ARTIST, count)
    }

    fun getRandomSong(artist: ArtistID3, count: Int): MutableLiveData<List<Child>> {
        val randomSongs = MutableLiveData<List<Child>>()
        CoroutineScope(Dispatchers.IO).launch {
            val response = artist.id?.let { subsonicRepository.getArtist(it) }
            val albums = response?.artist?.albums?.toMutableList() ?: mutableListOf()
            if (albums.isEmpty()) return@launch

            albums.shuffle()
            fetchAllAlbumSongsWithCallback(albums) { songs ->
                val shuffledSongs = songs.shuffled()
                randomSongs.postValue(shuffledSongs.take(count))
            }
        }
        return randomSongs
    }

    fun getTopSongs(artistName: String, count: Int): MutableLiveData<List<Child>> {
        val topSongs = MutableLiveData<List<Child>>()
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getTopSongs(artistName, count)
            topSongs.postValue(response?.topSongs?.songs ?: emptyList())
        }
        return topSongs
    }
}
