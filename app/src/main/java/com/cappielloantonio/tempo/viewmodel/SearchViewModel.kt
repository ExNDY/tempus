package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.model.RecentSearch
import com.cappielloantonio.tempo.repository.SearchingRepository
import com.cappielloantonio.tempo.subsonic.models.SearchResult3
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val results: SearchResult3? = null,
    val recentSearches: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val isLoading: Boolean = false
)

@UnstableApi
class SearchViewModel(
    private val searchingRepository: SearchingRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()
    private var searchJob: Job? = null

    init {
        loadRecentSearches()
    }

    private fun loadRecentSearches() {
        viewModelScope.launch {
            val recents = searchingRepository.getRecentSearchSuggestion()
            _uiState.update { it.copy(recentSearches = recents) }
        }
    }

    fun search(query: String, saveToRecents: Boolean = false) {
        if (query.length < 3) {
            searchJob?.cancel()
            _uiState.update { it.copy(results = null, isLoading = false, suggestions = emptyList()) }
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = searchingRepository.search3Result(query)
            _uiState.update { it.copy(results = result, isLoading = false) }

            if (saveToRecents) {
                insertNewSearch(query)
            }
        }
    }

    fun getSuggestions(query: String) {
        viewModelScope.launch {
            val suggestions = searchingRepository.searchSuggestions(query)
            _uiState.update { it.copy(suggestions = suggestions) }
        }
    }

    private fun insertNewSearch(search: String) {
        searchingRepository.insert(RecentSearch(search, System.currentTimeMillis() / 1000L))
        loadRecentSearches()
    }

    fun deleteRecentSearch(search: String) {
        searchingRepository.delete(RecentSearch(search, 0))
        loadRecentSearches()
    }
}
