package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child

class StarredArtistsSyncViewModel(
    private val artistRepository: ArtistRepository
) : ViewModel() {
    private val collectedSongs = MutableLiveData<List<Child>?>(null)
    fun getAllStarredArtistSongs(): LiveData<List<Child>?> = collectedSongs
    fun getStarredArtistSongs(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<Child>?> {
        return collectedSongs
    }

    fun syncStarredArtists(artists: List<ArtistID3>) {
        val allSongs = mutableListOf<Child>()
        var count = 0
        if (artists.isEmpty()) {
            collectedSongs.postValue(emptyList())
            return
        }
        for (artist in artists) {
            artistRepository.getArtistAllSongs(artist.id ?: "") { songs ->
                synchronized(allSongs) {
                    allSongs.addAll(songs)
                }
                count++
                if (count == artists.size) {
                    collectedSongs.postValue(allSongs)
                }
            }
        }
    }
}
