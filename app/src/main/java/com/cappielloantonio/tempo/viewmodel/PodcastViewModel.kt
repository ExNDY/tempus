package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cappielloantonio.tempo.repository.PodcastRepository
import com.cappielloantonio.tempo.subsonic.models.PodcastChannel
import com.cappielloantonio.tempo.subsonic.models.PodcastEpisode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PodcastUiState(
    val channels: List<PodcastChannel> = emptyList(),
    val newestEpisodes: List<PodcastEpisode> = emptyList(),
    val isLoading: Boolean = true
)

class PodcastViewModel(
    private val podcastRepository: PodcastRepository
) : ViewModel() {
    private var started = false
    private var refreshJob: Job? = null

    private val _channels = MutableStateFlow<List<PodcastChannel>>(emptyList())
    private val _newestEpisodes = MutableStateFlow<List<PodcastEpisode>>(emptyList())
    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<PodcastUiState> = combine(
        _channels, _newestEpisodes, _isLoading
    ) { channels, episodes, loading ->
        PodcastUiState(channels, episodes, loading)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PodcastUiState()
    )

    fun onStart() {
        if (started) return
        started = true
        refresh()
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _isLoading.value = true
            loadChannels()
            loadNewestEpisodes()
            _isLoading.value = false
        }
    }

    fun requestEpisodeDownload(episode: PodcastEpisode) {
        val episodeId = episode.id ?: return
        viewModelScope.launch {
            podcastRepository.downloadPodcastEpisode(episodeId)
            refresh()
        }
    }

    private suspend fun loadChannels() {
        _channels.value = podcastRepository.fetchPodcastChannels(false, null)
    }

    private suspend fun loadNewestEpisodes() {
        _newestEpisodes.value = podcastRepository.fetchNewestPodcastEpisodes(20)
    }
}
