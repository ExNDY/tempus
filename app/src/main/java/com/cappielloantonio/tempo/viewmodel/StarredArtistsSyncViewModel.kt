package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child

@UnstableApi
class StarredArtistsSyncViewModel(application: Application) : AndroidViewModel(application) {
    private val artistRepository = ArtistRepository()
    private val _collectedSongs = MutableLiveData<List<Child>>()

    fun getAllStarredArtistSongs(): LiveData<List<Child>> = _collectedSongs

    fun getStarredArtistSongs(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<Child>> {
        // Implementation might have triggered the sync before
        return _collectedSongs
    }

    fun syncStarredArtists(artists: List<ArtistID3>) {
        val allSongs = mutableListOf<Child>()
        var count = 0
        if (artists.isEmpty()) {
            _collectedSongs.postValue(emptyList())
            return
        }

        for (artist in artists) {
            artistRepository.getArtistAllSongs(artist.id ?: "") { songs ->
                synchronized(allSongs) {
                    allSongs.addAll(songs)
                }
                count++
                if (count == artists.size) {
                    _collectedSongs.postValue(allSongs)
                }
            }
        }
    }
}
