package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepository
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import kotlinx.coroutines.launch

@UnstableApi
class ArtistCatalogueViewModel(application: Application) : AndroidViewModel(application) {
    private val artistList = MutableLiveData<List<ArtistID3>>(ArrayList())
    private val subsonicRepository: SubsonicRepository = App.get(SubsonicRepository::class.java)

    fun getArtistList(): LiveData<List<ArtistID3>> = artistList

    fun loadArtists() {
        viewModelScope.launch {
            val response = subsonicRepository.getArtists()
            val artists = mutableListOf<ArtistID3>()
            response?.artists?.indices?.forEach { index ->
                index.artists?.let { artists.addAll(it) }
            }
            artistList.postValue(artists)
        }
    }
}
