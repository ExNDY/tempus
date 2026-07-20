package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.cappielloantonio.tempo.repository.DirectoryRepository
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Directory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DirectoryUiState(
    val directory: Directory? = null,
    val children: List<Child> = emptyList(),
    val isLoading: Boolean = false,
)

class DirectoryViewModel(
    private val directoryRepository: DirectoryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DirectoryUiState())
    val uiState: StateFlow<DirectoryUiState> = _uiState.asStateFlow()
    private var startedId: String? = null
    private var loadJob: Job? = null

    fun onStart(id: String) {
        if (startedId == id) return
        startedId = id
        loadDirectory(id)
    }

    fun loadDirectory(id: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val directory = directoryRepository.getMusicDirectory(id).asFlow().first()
            _uiState.update {
                it.copy(
                    directory = directory,
                    children = directory?.children ?: emptyList(),
                    isLoading = false,
                )
            }
        }
    }

    suspend fun collectDirectorySongs(directoryId: String): List<Child> {
        val collectedSongs = linkedSetOf<Child>()
        collectDirectorySongsRecursive(directoryId, collectedSongs)
        return collectedSongs.toList()
    }

    private suspend fun collectDirectorySongsRecursive(
        directoryId: String,
        collectedSongs: LinkedHashSet<Child>,
    ) {
        val directory = directoryRepository.getMusicDirectory(directoryId).asFlow().first()
        val children = directory?.children.orEmpty()

        children.forEach { child ->
            if (!child.isDir && !child.isVideo) {
                collectedSongs.add(child)
            }
        }

        children
            .filter { it.isDir && it.id.isNotBlank() }
            .forEach { child ->
                collectDirectorySongsRecursive(child.id, collectedSongs)
            }
    }
}
