package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.repository.PodcastRepository
import com.cappielloantonio.tempo.subsonic.models.PodcastChannel
import com.cappielloantonio.tempo.subsonic.models.PodcastEpisode
import kotlinx.coroutines.launch

@UnstableApi
class PodcastChannelPageViewModel(application: Application) : AndroidViewModel(application) {
    private val podcastRepository = PodcastRepository()
    private val podcastChannel = MutableLiveData<PodcastChannel>()

    fun getPodcastChannel(): LiveData<PodcastChannel> = podcastChannel

    fun setPodcastChannel(channel: PodcastChannel) {
        podcastChannel.postValue(channel)
    }

    fun getPodcastChannelEpisodes(): LiveData<List<PodcastEpisode>> {
        val result = MutableLiveData<List<PodcastEpisode>>()
        viewModelScope.launch {
            val id = podcastChannel.value?.id ?: return@launch
            val response = podcastRepository.getPodcastChannels(true, id)
            result.postValue(response.value?.firstOrNull()?.episodes ?: emptyList())
        }
        return result
    }

    fun requestPodcastEpisodeDownload(episode: PodcastEpisode) {
        val id = episode.id ?: return
        viewModelScope.launch {
            podcastRepository.downloadPodcastEpisode(id)
        }
    }
}
