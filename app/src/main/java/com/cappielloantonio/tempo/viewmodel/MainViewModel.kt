package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cappielloantonio.tempo.github.models.LatestRelease
import com.cappielloantonio.tempo.network.ConnectionState
import com.cappielloantonio.tempo.network.NetworkConnectivityService
import com.cappielloantonio.tempo.repository.QueueRepository
import com.cappielloantonio.tempo.repository.SystemRepository
import com.cappielloantonio.tempo.subsonic.models.OpenSubsonicExtension
import com.cappielloantonio.tempo.subsonic.models.SubsonicResponse
import com.cappielloantonio.tempo.util.Preferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val systemRepository: SystemRepository,
    private val queueRepository: QueueRepository,
    val networkService: NetworkConnectivityService
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = networkService.connectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionState())

    private val pingResult = MutableLiveData<SubsonicResponse?>(null)
    private val openSubsonicExtensions = MutableLiveData<List<OpenSubsonicExtension>?>(null)
    private val tempoUpdate = MutableLiveData<LatestRelease?>(null)

    fun isQueueLoaded(): Boolean {
        return queueRepository.count() != 0
    }

    fun syncSelectedServerFromPreferences() {
        networkService.setSelectedServer(Preferences.getInUseServerAddress())
    }

    fun refreshConnectionState() {
        networkService.refresh()
    }

    fun ping(): LiveData<SubsonicResponse?> {
        if (pingResult.value == null) {
            viewModelScope.launch {
                pingResult.postValue(systemRepository.ping())
            }
        }
        return pingResult
    }

    fun getOpenSubsonicExtensions(): LiveData<List<OpenSubsonicExtension>?> {
        if (openSubsonicExtensions.value == null) {
            viewModelScope.launch {
                openSubsonicExtensions.postValue(systemRepository.getOpenSubsonicExtensions())
            }
        }
        return openSubsonicExtensions
    }

    fun checkTempoUpdate(): LiveData<LatestRelease?> = tempoUpdate
}
