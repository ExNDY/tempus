package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.asFlow
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cappielloantonio.tempo.repository.GenreRepository
import com.cappielloantonio.tempo.subsonic.models.Genre
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FilterUiState(
    val genres: List<Genre> = emptyList(),
    val selectedFilterIds: List<String> = emptyList(),
    val selectedFilterNames: List<String> = emptyList(),
    val isLoading: Boolean = true,
)

class FilterViewModel(
    private val genreRepository: GenreRepository
) : ViewModel() {

    private val selectedFiltersID = arrayListOf<String>()
    private val selectedFilters = arrayListOf<String>()
    private val _uiState = MutableStateFlow(FilterUiState())
    val uiState: StateFlow<FilterUiState> = _uiState.asStateFlow()
    private var started = false

    fun onStart() {
        if (started) return
        started = true
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    selectedFilterIds = selectedFiltersID.toList(),
                    selectedFilterNames = selectedFilters.toList(),
                )
            }
            genreRepository.getGenres(false, -1).asFlow().collectLatest { genres ->
                _uiState.update {
                    it.copy(
                        genres = genres ?: emptyList(),
                        selectedFilterIds = selectedFiltersID.toList(),
                        selectedFilterNames = selectedFilters.toList(),
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun getGenreList(): LiveData<List<Genre>> = genreRepository.getGenres(false, -1)

    fun addFilter(filterID: String, filterName: String) {
        if (!selectedFiltersID.contains(filterID)) {
            selectedFiltersID.add(filterID)
            selectedFilters.add(filterName)
            syncSelection()
        }
    }

    fun removeFilter(filterID: String, filterName: String) {
        selectedFiltersID.remove(filterID)
        selectedFilters.remove(filterName)
        syncSelection()
    }

    fun getFilters(): ArrayList<String> = selectedFiltersID

    fun getFilterNames(): ArrayList<String> = selectedFilters

    private fun syncSelection() {
        _uiState.update {
            it.copy(
                selectedFilterIds = selectedFiltersID.toList(),
                selectedFilterNames = selectedFilters.toList(),
            )
        }
    }
}
