package com.cappielloantonio.tempo.repository

import androidx.lifecycle.MutableLiveData
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepository
import com.cappielloantonio.tempo.subsonic.models.PodcastChannel
import com.cappielloantonio.tempo.subsonic.models.PodcastEpisode
import com.cappielloantonio.tempo.subsonic.models.SubsonicResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@UnstableApi
class PodcastRepository {
    private val subsonicRepository: SubsonicRepository = App.get(SubsonicRepository::class.java)

    fun getPodcastChannels(includeEpisodes: Boolean, channelId: String?): MutableLiveData<List<PodcastChannel>> {
        val livePodcastChannel = MutableLiveData<List<PodcastChannel>>(ArrayList())
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getPodcasts(includeEpisodes, channelId)
            livePodcastChannel.postValue(response?.podcasts?.channels ?: emptyList())
        }
        return livePodcastChannel
    }

    fun getNewestPodcastEpisodes(count: Int): MutableLiveData<List<PodcastEpisode>> {
        val liveNewestPodcastEpisodes = MutableLiveData<List<PodcastEpisode>>(ArrayList())
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getNewestPodcasts(count)
            liveNewestPodcastEpisodes.postValue(response?.newestPodcasts?.episodes ?: emptyList())
        }
        return liveNewestPodcastEpisodes
    }

    suspend fun refreshPodcasts(): SubsonicResponse? = subsonicRepository.refreshPodcasts()

    suspend fun createPodcastChannel(url: String): SubsonicResponse? = subsonicRepository.createPodcastChannel(url)

    suspend fun deletePodcastChannel(channelId: String): SubsonicResponse? = subsonicRepository.deletePodcastChannel(channelId)

    suspend fun deletePodcastEpisode(episodeId: String): SubsonicResponse? = subsonicRepository.deletePodcastEpisode(episodeId)

    suspend fun downloadPodcastEpisode(episodeId: String): SubsonicResponse? = subsonicRepository.downloadPodcastEpisode(episodeId)
}
