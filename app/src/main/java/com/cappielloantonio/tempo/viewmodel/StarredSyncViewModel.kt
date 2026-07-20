package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.cappielloantonio.tempo.repository.SongRepository
import com.cappielloantonio.tempo.subsonic.models.Child

class StarredSyncViewModel(
    private val songRepository: SongRepository
) : ViewModel() {

    private val starredTracks = MutableLiveData<List<Child>?>(null)

    fun getStarredTracks(): LiveData<List<Child>?> {
        if (starredTracks.value == null) {
            loadStarredTracks()
        }
        return starredTracks
    }

    private fun loadStarredTracks() {
        val source = songRepository.getStarredSongs(false, -1)
        source.observeForever(object : Observer<List<Child>> {
            override fun onChanged(value: List<Child>) {
                starredTracks.postValue(value)
                source.removeObserver(this)
            }
        })
    }
}
