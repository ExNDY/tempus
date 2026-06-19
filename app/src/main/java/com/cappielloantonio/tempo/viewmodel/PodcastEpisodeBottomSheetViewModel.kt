package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.repository.PodcastRepository
import com.cappielloantonio.tempo.subsonic.models.PodcastEpisode
import kotlinx.coroutines.launch

@UnstableApi
class PodcastEpisodeBottomSheetViewModel(application: Application) : AndroidViewModel(application) {
    private val podcastRepository = PodcastRepository()
    var podcastEpisode: PodcastEpisode? = null

    fun deletePodcastEpisode() {
        val id = podcastEpisode?.id ?: return
        viewModelScope.launch {
            podcastRepository.deletePodcastEpisode(id)
        }
    }
}
