package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cappielloantonio.tempo.repository.RadioRepository
import com.cappielloantonio.tempo.subsonic.models.InternetRadioStation
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RadioUiState(
    val stations: List<InternetRadioStation> = emptyList(),
    val isLoading: Boolean = true
)

class RadioViewModel(
    private val radioRepository: RadioRepository
) : ViewModel() {
    private var started = false

    private val _stations = MutableStateFlow<List<InternetRadioStation>>(emptyList())
    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<RadioUiState> = combine(
        _stations, _isLoading
    ) { stations, loading ->
        RadioUiState(stations, loading)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RadioUiState()
    )

    fun onStart() {
        if (started) return
        started = true
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _stations.value = radioRepository.getInternetRadioStations() ?: emptyList()
            _isLoading.value = false
        }
    }
}
