package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepository
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import kotlinx.coroutines.launch

@UnstableApi
class ArtistCatalogueViewModel(
    private val subsonicRepository: SubsonicRepository
) : ViewModel() {

    private val artistList = MutableLiveData<List<ArtistID3>>(ArrayList())

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
