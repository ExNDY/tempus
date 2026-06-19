package com.cappielloantonio.tempo.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepository
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.Constants.SeedType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

@UnstableApi
class SongRepository @JvmOverloads constructor(
    private val subsonicRepository: SubsonicRepository = App.get(SubsonicRepository::class.java)
) {

    companion object {
        private const val TAG = "SongRepository"
    }

    fun interface MediaCallbackInternal {
        fun onSongsAvailable(songs: List<Child>)
    }

    fun getStarredSongs(random: Boolean, size: Int): MutableLiveData<List<Child>> {
        val starredSongs = MutableLiveData<List<Child>>(emptyList())
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getStarred2()
            val songs = response?.starred2?.songs ?: emptyList()
            if (songs.isNotEmpty()) {
                if (!random) {
                    starredSongs.postValue(songs)
                } else {
                    val mutableSongs = songs.toMutableList()
                    mutableSongs.shuffle()
                    starredSongs.postValue(mutableSongs.take(size))
                }
            }
        }
        return starredSongs
    }

    fun getInstantMix(id: String, type: SeedType, count: Int): MutableLiveData<List<Child>> {
        val instantMix = MutableLiveData<List<Child>>(mutableListOf())
        val trackIds = mutableSetOf<String>()

        getInstantMix(id, type, count) { songs ->
            val current = instantMix.value?.toMutableList() ?: mutableListOf()
            for (s in songs) {
                if (!trackIds.contains(s.id)) {
                    current.add(s)
                    trackIds.add(s.id)
                }
            }

            if (current.size < count / 2) {
                CoroutineScope(Dispatchers.IO).launch {
                    val remainder = subsonicRepository.getSimilarSongs(id, count)
                    val remainderSongs = remainder?.similarSongs?.songs ?: emptyList()
                    for (r in remainderSongs) {
                        if (!trackIds.contains(r.id)) {
                            current.add(r)
                            trackIds.add(r.id)
                        }
                    }
                    instantMix.postValue(current)
                }
            } else {
                instantMix.postValue(current)
            }
        }

        return instantMix
    }

    fun getInstantMix(id: String, type: SeedType, count: Int, callback: MediaCallbackInternal) {
        MediaCallbackAccumulator(callback, count).start(id, type)
    }

    private inner class MediaCallbackAccumulator(
        private val originalCallback: MediaCallbackInternal,
        private val targetCount: Int
    ) {
        private val accumulatedSongs = mutableListOf<Child>()
        private val trackIds = mutableSetOf<String>()
        private var isComplete = false

        fun start(id: String, type: SeedType) {
            performSmartMix(id, type, targetCount) { batch ->
                onBatchReceived(batch)
            }
        }

        private fun onBatchReceived(batch: List<Child>) {
            if (isComplete || batch.isEmpty()) return

            for (song in batch) {
                if (!trackIds.contains(song.id) && accumulatedSongs.size < targetCount) {
                    trackIds.add(song.id)
                    accumulatedSongs.add(song)
                }
            }

            if (accumulatedSongs.size >= targetCount) {
                originalCallback.onSongsAvailable(ArrayList(accumulatedSongs))
                isComplete = true
            }
        }
    }

    private fun performSmartMix(id: String, type: SeedType, count: Int, callback: MediaCallbackInternal) {
        when (type) {
            SeedType.ARTIST -> fetchSimilarByArtist(id, count, callback)
            SeedType.ALBUM -> fetchAlbumSongs(id, count, callback)
            SeedType.TRACK -> fetchSingleTrackThenSimilar(id, count, callback)
        }
    }

    private fun fetchAlbumSongs(albumId: String, count: Int, callback: MediaCallbackInternal) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getAlbum(albumId)
            val albumSongs = response?.album?.songs ?: emptyList()
            if (albumSongs.isNotEmpty()) {
                callback.onSongsAvailable(albumSongs.take(count))
            }
        }
    }

    private fun fetchSimilarByArtist(artistId: String, count: Int, callback: MediaCallbackInternal) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getSimilarSongs2(artistId, count)
            val similar = response?.similarSongs2?.songs ?: emptyList()
            if (similar.isNotEmpty()) {
                callback.onSongsAvailable(similar.take(count))
            }
        }
    }

    private fun fetchSingleTrackThenSimilar(trackId: String, count: Int, callback: MediaCallbackInternal) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getSong(trackId)
            response?.song?.let {
                callback.onSongsAvailable(listOf(it))
            }
        }
    }

    fun getContinuousMix(id: String, count: Int): MutableLiveData<List<Child>?> {
        val instantMix = MutableLiveData<List<Child>?>()
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getSimilarSongs(id, count)
            instantMix.postValue(response?.similarSongs?.songs)
        }
        return instantMix
    }

    fun getRandomSample(number: Int, fromYear: Int?, toYear: Int?): MutableLiveData<List<Child>> {
        val randomSongsSample = MutableLiveData<List<Child>>()
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getRandomSongs(number, fromYear, toYear, null)
            randomSongsSample.postValue(response?.randomSongs?.songs ?: emptyList())
        }
        return randomSongsSample
    }

    fun getRandomSampleWithGenre(number: Int, fromYear: Int?, toYear: Int?, genre: String): MutableLiveData<List<Child>> {
        val randomSongsSample = MutableLiveData<List<Child>>()
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getRandomSongs(number, fromYear, toYear, genre)
            randomSongsSample.postValue(response?.randomSongs?.songs ?: emptyList())
        }
        return randomSongsSample
    }

    fun scrobble(id: String, submission: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            subsonicRepository.scrobble(id, submission)
        }
    }

    fun setRating(id: String, rating: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            subsonicRepository.setRating(id, rating)
        }
    }

    fun getSongsByGenre(id: String, page: Int): MutableLiveData<List<Child>> {
        val songsByGenre = MutableLiveData<List<Child>>()
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getSongsByGenre(id, 100, 100 * page)
            songsByGenre.postValue(response?.songsByGenre?.songs ?: emptyList())
        }
        return songsByGenre
    }

    fun getSongsByGenres(genresId: ArrayList<String>): MutableLiveData<List<Child>> {
        val songsByGenre = MutableLiveData<List<Child>>()
        CoroutineScope(Dispatchers.IO).launch {
            val allSongs = mutableListOf<Child>()
            for (id in genresId) {
                val response = subsonicRepository.getSongsByGenre(id, 500, 0)
                response?.songsByGenre?.songs?.let { allSongs.addAll(it) }
            }
            songsByGenre.postValue(allSongs)
        }
        return songsByGenre
    }

    fun getSong(id: String): MutableLiveData<Child?> {
        val song = MutableLiveData<Child?>()
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getSong(id)
            song.postValue(response?.song)
        }
        return song
    }

    fun getSongLyrics(song: Child): MutableLiveData<String?> {
        val lyrics = MutableLiveData<String?>(null)
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getLyrics(song.artist ?: "", song.title ?: "")
            lyrics.postValue(response?.lyrics?.value)
        }
        return lyrics
    }
}
