package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.asFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cappielloantonio.tempo.repository.DirectoryRepository
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Index
import com.cappielloantonio.tempo.subsonic.models.Indexes
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IndexUiState(
    val indices: List<Index> = emptyList(),
    val children: List<Child> = emptyList(),
    val isLoading: Boolean = true,
    val musicFolderId: String? = null,
)

class IndexViewModel(
    private val directoryRepository: DirectoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(IndexUiState())
    val uiState = _uiState.asStateFlow()
    private var loadJob: Job? = null

    fun onStart(musicFolderId: String? = null) {
        if (_uiState.value.musicFolderId == musicFolderId && !_uiState.value.isLoading) {
            return
        }
        loadIndexes(musicFolderId)
    }

    private fun loadIndexes(musicFolderId: String? = null) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, musicFolderId = musicFolderId) }
            val indexes = directoryRepository.getIndexes(musicFolderId, null).asFlow().first()
            _uiState.update {
                indexes.toIndexUiState(musicFolderId)
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

internal fun Indexes?.toIndexUiState(musicFolderId: String?): IndexUiState {
    return IndexUiState(
        indices = this?.indices ?: emptyList(),
        children = this?.children ?: emptyList(),
        isLoading = false,
        musicFolderId = musicFolderId,
    )
}
