package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cappielloantonio.tempo.repository.DownloadRepository
import com.cappielloantonio.tempo.ui.download.downloadedGroupTitle
import com.cappielloantonio.tempo.ui.download.filterDownloadedSongs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.cappielloantonio.tempo.subsonic.models.Child

data class DownloadedBottomSheetUiState(
    val songs: List<Child> = emptyList(),
    val title: String = "",
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
) {
    val hasMultipleSongs: Boolean
        get() = songs.size > 1
}

class DownloadedBottomSheetViewModel(
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadedBottomSheetUiState())
    val uiState = _uiState.asStateFlow()

    private var startedKey: String? = null
    private var loadJob: Job? = null

    fun onStart(groupType: String, groupValue: String, force: Boolean = false) {
        val key = "$groupType|$groupValue"
        if (!force && startedKey == key && (_uiState.value.songs.isNotEmpty() || _uiState.value.isLoading)) return
        startedKey = key
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = DownloadedBottomSheetUiState(isLoading = true)
            if (groupType.isBlank() || groupValue.isBlank()) {
                _uiState.value = DownloadedBottomSheetUiState(isLoading = false, hasError = true)
                return@launch
            }

            val songs = runCatching {
                filterDownloadedSongs(
                    groupType = groupType,
                    groupValue = groupValue,
                    songs = downloadRepository.getAllDownloadsSnapshot(),
                )
            }.getOrDefault(emptyList())

            _uiState.value = DownloadedBottomSheetUiState(
                songs = songs,
                title = downloadedGroupTitle(groupType, groupValue, songs),
                isLoading = false,
                hasError = songs.isEmpty(),
            )
        }
    }

    fun retry(groupType: String, groupValue: String) = onStart(groupType, groupValue, force = true)
}
