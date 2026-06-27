package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.cappielloantonio.tempo.repository.GenreRepository
import com.cappielloantonio.tempo.subsonic.models.Genre
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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

    fun onStart() {
        if (started) return
        started = true
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            genreRepository.getGenres(false, -1).asFlow().collectLatest { genres ->
                _uiState.update { it.copy(genres = genres ?: emptyList(), isLoading = false) }
            }
        }
    }
}
