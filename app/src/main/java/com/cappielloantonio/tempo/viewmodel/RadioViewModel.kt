package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cappielloantonio.tempo.repository.RadioRepository
import com.cappielloantonio.tempo.subsonic.models.InternetRadioStation
import kotlinx.coroutines.launch

class RadioViewModel(
    private val radioRepository: RadioRepository
) : ViewModel() {

    private val internetRadioStations = MutableLiveData<List<InternetRadioStation>?>(null)

    fun getInternetRadioStations(): LiveData<List<InternetRadioStation>?> {
        if (internetRadioStations.value == null) {
            loadInternetRadioStations()
        }
        return internetRadioStations
    }

    fun refreshInternetRadioStations() {
        loadInternetRadioStations()
    }

    private fun loadInternetRadioStations() {
        viewModelScope.launch {
            internetRadioStations.postValue(radioRepository.getInternetRadioStations())
        }
    }
}
