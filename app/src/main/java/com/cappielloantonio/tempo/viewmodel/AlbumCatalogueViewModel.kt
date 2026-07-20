package com.cappielloantonio.tempo.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
data class AlbumCatalogueUiState(
    val albums: List<AlbumID3> = emptyList(),
    val isLoading: Boolean = true,
)
class AlbumCatalogueViewModel(
    private val subsonicRepository: SubsonicRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AlbumCatalogueUiState())
    val uiState: StateFlow<AlbumCatalogueUiState> = _uiState.asStateFlow()
    private var started = false
    private var page = 0
    private var isAllLoaded = false
    fun onStart() {
        if (started) return
        started = true
        refresh()
    }
    fun refresh() {
        page = 0
        isAllLoaded = false
        _uiState.value = AlbumCatalogueUiState(isLoading = true)
        loadNextPage()
    }
    fun loadNextPage() {
        if (isAllLoaded) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val size = 500
            val response = subsonicRepository.getAlbumList2("alphabeticalByName", size, size * page++, null, null)
            val media = response?.albumList2?.albums ?: emptyList()
            _uiState.update { it.copy(albums = it.albums + media, isLoading = false) }
            if (media.size < size) isAllLoaded = true
        }
    }
}
