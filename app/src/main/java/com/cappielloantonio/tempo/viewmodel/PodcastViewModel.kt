package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cappielloantonio.tempo.repository.PodcastRepository
import com.cappielloantonio.tempo.subsonic.models.PodcastChannel
import com.cappielloantonio.tempo.subsonic.models.PodcastEpisode

class PodcastViewModel(
    private val podcastRepository: PodcastRepository
) : ViewModel() {

    private val newestPodcastEpisodes = MutableLiveData<List<PodcastEpisode>?>(null)
    private val podcastChannels = MutableLiveData<List<PodcastChannel>?>(null)

    fun getNewestPodcastEpisodes(): LiveData<List<PodcastEpisode>?> {
        if (newestPodcastEpisodes.value == null) {
            loadNewestPodcastEpisodes()
        }
        return newestPodcastEpisodes
    }

    fun getPodcastChannels(): LiveData<List<PodcastChannel>?> {
        if (podcastChannels.value == null) {
            loadPodcastChannels()
        }
        return podcastChannels
    }

    fun refreshNewestPodcastEpisodes() {
        loadNewestPodcastEpisodes()
    }

    fun refreshPodcastChannels() {
        loadPodcastChannels()
    }

    private fun loadNewestPodcastEpisodes() {
        val source = podcastRepository.getNewestPodcastEpisodes(20)
        source.observeForever(object : Observer<List<PodcastEpisode>> {
            override fun onChanged(value: List<PodcastEpisode>) {
                newestPodcastEpisodes.postValue(value)
                source.removeObserver(this)
            }
        })
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
