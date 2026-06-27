package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.repository.PodcastRepository
import com.cappielloantonio.tempo.subsonic.models.PodcastEpisode
import kotlinx.coroutines.launch

@UnstableApi
class PodcastEpisodeBottomSheetViewModel(
    private val podcastRepository: PodcastRepository,
) : androidx.lifecycle.ViewModel() {
    var podcastEpisode: PodcastEpisode? = null

    fun requestPodcastEpisodeDownload() {
        val id = podcastEpisode?.id ?: return
        viewModelScope.launch {
            podcastRepository.downloadPodcastEpisode(id)
        }
    }

    fun deletePodcastEpisode() {
        val id = podcastEpisode?.id ?: return
        viewModelScope.launch {
            podcastRepository.deletePodcastEpisode(id)
        }
    }
}
