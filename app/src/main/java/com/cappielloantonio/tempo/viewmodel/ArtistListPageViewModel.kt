package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.DownloadRepository
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.util.Constants
import java.util.ArrayList
import java.util.Comparator
import java.util.TreeSet
import java.util.stream.Collectors

class ArtistListPageViewModel(
    private val artistRepository: ArtistRepository,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    @JvmField
    var title: String? = null

    private var artistList = MutableLiveData<List<ArtistID3>>(arrayListOf())

    fun getArtistList(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<ArtistID3>> {
        artistList = MutableLiveData(arrayListOf())

        when (title) {
            Constants.ARTIST_STARRED -> artistList = artistRepository.getStarredArtists(false, -1)
            Constants.ARTIST_DOWNLOADED -> {
                downloadRepository.getLiveDownload().observe(owner) { downloads ->
                    artistList.postValue(emptyList())
                }
            }
        }

        return artistList
    }
}
