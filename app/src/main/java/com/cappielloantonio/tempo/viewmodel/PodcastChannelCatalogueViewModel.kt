package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cappielloantonio.tempo.repository.PodcastRepository
import com.cappielloantonio.tempo.subsonic.models.PodcastChannel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PodcastChannelCatalogueUiState(
    val channels: List<PodcastChannel> = emptyList(),
    val isLoading: Boolean = true,
    val isUnsupported: Boolean = false,
)

class PodcastChannelCatalogueViewModel(
    private val podcastRepository: PodcastRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PodcastChannelCatalogueUiState())
    val uiState: StateFlow<PodcastChannelCatalogueUiState> = _uiState.asStateFlow()
    private var started = false
    private var refreshJob: Job? = null

    fun onStart() {
        if (started) return
        started = true
        refresh()
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val response = podcastRepository.getPodcastChannelsResponse(false, null)
            _uiState.value = PodcastChannelCatalogueUiState(
                channels = response?.podcasts?.channels.orEmpty(),
                isLoading = false,
                isUnsupported = response?.error != null && response.podcasts == null,
            )
        }
    }
}
