package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cappielloantonio.tempo.repository.DirectoryRepository
import com.cappielloantonio.tempo.subsonic.models.Index
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class IndexUiState(
    val indices: List<Index> = emptyList(),
    val isLoading: Boolean = true,
    val musicFolderId: String? = null,
)

class IndexViewModel(
    private val directoryRepository: DirectoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(IndexUiState())
    val uiState = _uiState.asStateFlow()

    fun onStart(musicFolderId: String? = null) {
        if (_uiState.value.musicFolderId == musicFolderId && !_uiState.value.isLoading) {
            return
        }
        loadIndexes(musicFolderId)
    }

    private fun loadIndexes(musicFolderId: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, musicFolderId = musicFolderId) }
            directoryRepository.getIndexes(musicFolderId, null).observeForever { indexes ->
                _uiState.update {
                    it.copy(
                        indices = indexes?.indices ?: emptyList(),
                        isLoading = false,
                    )
                }
            }
        }
    }
}
