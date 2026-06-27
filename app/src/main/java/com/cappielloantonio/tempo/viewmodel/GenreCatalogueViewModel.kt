package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.cappielloantonio.tempo.repository.GenreRepository
import com.cappielloantonio.tempo.subsonic.models.Genre
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GenreCatalogueUiState(
    val genres: List<Genre> = emptyList(),
    val isLoading: Boolean = true,
)

class GenreCatalogueViewModel(
    private val genreRepository: GenreRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GenreCatalogueUiState())
    val uiState: StateFlow<GenreCatalogueUiState> = _uiState.asStateFlow()
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
            val genres = genreRepository.getGenres(false, -1).asFlow().first().orEmpty()
            _uiState.update { it.copy(genres = genres, isLoading = false) }
        }
    }
}
