package com.cappielloantonio.tempo.viewmodel

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.model.DownloadStack
import com.cappielloantonio.tempo.repository.DownloadRepository
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.ExternalAudioReader
import com.cappielloantonio.tempo.util.Preferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.core.net.toUri

data class DownloadUiState(
    val songs: List<Child> = emptyList(),
    val viewStack: List<DownloadStack> = emptyList(),
    val isLoading: Boolean = true,
)

class DownloadViewModel(
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState = _uiState.asStateFlow()

    private val _refreshResults = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val refreshResults: SharedFlow<Int> = _refreshResults.asSharedFlow()

    private var started = false

    fun onStart() {
        if (started) return
        started = true

        _uiState.value = DownloadUiState(
            songs = emptyList(),
            viewStack = listOf(
                DownloadStack(
                    id = Preferences.getDefaultDownloadViewType(),
                    view = null,
                )
            ),
            isLoading = true,
        )

        observeDownloads()
    }

    fun setRootView(downloadType: String) {
        Preferences.setDefaultDownloadViewType(downloadType)
        _uiState.update {
            it.copy(
                viewStack = listOf(DownloadStack(id = downloadType, view = null)),
            )
        }
    }

    fun pushViewStack(level: DownloadStack) {
        _uiState.update { state ->
            state.copy(viewStack = state.viewStack + level)
        }
    }

    fun popViewStack() {
        _uiState.update { state ->
            if (state.viewStack.size <= 1) {
                state
            } else {
                state.copy(viewStack = state.viewStack.dropLast(1))
            }
        }
    }

    fun canPopViewStack(): Boolean = _uiState.value.viewStack.size > 1

    private fun observeDownloads() {
        viewModelScope.launch {
            downloadRepository.getLiveDownload().asFlow().collectLatest { downloads ->
                _uiState.update {
                    it.copy(
                        songs = downloads.map { download -> download as Child },
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun refreshExternalDownloads() {
        viewModelScope.launch {
            val directoryUri = Preferences.getDownloadDirectoryUri()
            if (directoryUri.isNullOrBlank()) {
                _refreshResults.emit(-1)
                return@launch
            }

            val downloads = downloadRepository.getAllDownloads()
            if (downloads.isEmpty()) {
                _refreshResults.emit(0)
                return@launch
            }

            val toRemove = downloads.filter { download ->
                shouldRemoveExternalDownload(download)
            }

            if (toRemove.isEmpty()) {
                _refreshResults.emit(0)
                return@launch
            }

            val ids = toRemove.map { it.id }
            toRemove.forEach(ExternalAudioReader::removeMetadata)
            downloadRepository.delete(ids)
            ExternalAudioReader.refreshCache()
            _refreshResults.emit(ids.size)
        }
    }

    private fun shouldRemoveExternalDownload(download: Download): Boolean {
        val uriString = download.downloadUri
        if (uriString.isNullOrBlank()) {
            return false
        }

        val uri = uriString.toUri()
        if (!uri.scheme.equals("content", ignoreCase = true)) {
            return false
        }

        val file = try {
            DocumentFile.fromSingleUri(App.getContext(), uri)
        } catch (_: SecurityException) {
            null
        }

        return file == null || !file.exists()
    }
}
