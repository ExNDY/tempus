package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cappielloantonio.tempo.repository.PodcastRepository
import com.cappielloantonio.tempo.subsonic.models.PodcastChannel

class PodcastChannelCatalogueViewModel(
    private val podcastRepository: PodcastRepository
) : ViewModel() {

    private val podcastChannels = MutableLiveData<List<PodcastChannel>?>(null)

    fun getPodcastChannels(): LiveData<List<PodcastChannel>?> {
        if (podcastChannels.value == null) {
            loadPodcastChannels()
        }
        return podcastChannels
    }

    fun refreshPodcastChannels() {
        loadPodcastChannels()
    }

    private fun loadPodcastChannels() {
        val source = podcastRepository.getPodcastChannels(false, null)
        source.observeForever(object : Observer<List<PodcastChannel>> {
            override fun onChanged(value: List<PodcastChannel>) {
                podcastChannels.postValue(value)
                source.removeObserver(this)
            }
        })
    }
}
