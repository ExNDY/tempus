package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.repository.PodcastRepository
import com.cappielloantonio.tempo.subsonic.models.PodcastChannel
import com.cappielloantonio.tempo.subsonic.models.PodcastEpisode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PodcastChannelPageArgs(val channel: PodcastChannel)

data class PodcastChannelPageUiState(
    val channel: PodcastChannel? = null,
    val episodes: List<PodcastEpisode> = emptyList(),
    val isLoading: Boolean = true,
)

@UnstableApi
class PodcastChannelPageViewModel(
    private val podcastRepository: PodcastRepository,
) : androidx.lifecycle.ViewModel() {
    private val _uiState = MutableStateFlow(PodcastChannelPageUiState())
    val uiState: StateFlow<PodcastChannelPageUiState> = _uiState.asStateFlow()
    private var startedChannelId: String? = null
    private var refreshJob: Job? = null

    fun onStart(args: PodcastChannelPageArgs) {
        if (startedChannelId == args.channel.id) return
        startedChannelId = args.channel.id
        _uiState.value = PodcastChannelPageUiState(channel = args.channel, isLoading = true)
        refresh()
    }

    fun refresh() {
        val channelId = _uiState.value.channel?.id ?: return
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val refreshedChannel = podcastRepository.fetchPodcastChannels(true, channelId).firstOrNull()
            _uiState.update { current ->
                current.copy(
                    channel = refreshedChannel ?: current.channel,
                    episodes = refreshedChannel?.episodes.orEmpty(),
                    isLoading = false,
                )
            }
        }
    }

    fun requestPodcastEpisodeDownload(episode: PodcastEpisode) {
        val id = episode.id ?: return
        viewModelScope.launch {
            podcastRepository.downloadPodcastEpisode(id)
            refresh()
        }
    }
}
