package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepository
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArtistCatalogueUiState(
    val artists: List<ArtistID3> = emptyList(),
    val isLoading: Boolean = true,
)

@UnstableApi
class ArtistCatalogueViewModel(
    private val subsonicRepository: SubsonicRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ArtistCatalogueUiState())
    val uiState: StateFlow<ArtistCatalogueUiState> = _uiState.asStateFlow()
    private var started = false

    fun onStart() {
        if (started) return
        started = true
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val response = subsonicRepository.getArtists()
            val artists = mutableListOf<ArtistID3>()
            response?.artists?.indices?.forEach { index -> index.artists?.let(artists::addAll) }
            _uiState.update { it.copy(artists = artists, isLoading = false) }
        }
    }
}
