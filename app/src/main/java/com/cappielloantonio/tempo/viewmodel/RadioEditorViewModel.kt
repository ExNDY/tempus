package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.repository.RadioRepository
import com.cappielloantonio.tempo.subsonic.models.InternetRadioStation
import com.cappielloantonio.tempo.subsonic.models.SubsonicResponse
import kotlinx.coroutines.launch

@UnstableApi
class RadioEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val radioRepository = RadioRepository()
    var radioToEdit: InternetRadioStation? = null
        private set
    var isLocal = false
        private set

    private val _isSuccess = MutableLiveData(false)
    val isSuccess: LiveData<Boolean> = _isSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun clearError() {
        _errorMessage.value = null
    }

    fun setRadioToEdit(internetRadioStation: InternetRadioStation?) {
        this.radioToEdit = internetRadioStation
        this.isLocal = internetRadioStation != null && radioRepository.isLocalStation(internetRadioStation.id)
    }

    fun setLocal(local: Boolean) {
        isLocal = local
    }

    fun isEditing(): Boolean = radioToEdit != null

    fun createRadio(name: String, streamURL: String, homepageURL: String?, coverArtUrl: String?) {
        _errorMessage.value = null
        _isSuccess.value = false

        if (isLocal) {
            createLocalRadio(name, streamURL, homepageURL, coverArtUrl)
        } else {
            createServerRadio(name, streamURL, homepageURL)
        }
    }

    private fun createLocalRadio(name: String, streamURL: String, homepageURL: String?, coverArtUrl: String?) {
        radioRepository.createLocalStation(name, streamURL, homepageURL, coverArtUrl) {
            _isSuccess.postValue(true)
        }
    }

    private fun createServerRadio(name: String, streamURL: String, homepageURL: String?) {
        viewModelScope.launch {
            val response = radioRepository.createInternetRadioStation(name, streamURL, homepageURL)
            handleServerResponse(response)
        }
    }

    fun updateRadio(name: String, streamURL: String, homepageURL: String?, coverArtUrl: String?) {
        val id = radioToEdit?.id ?: return

        _errorMessage.value = null
        _isSuccess.value = false

        if (isLocal) {
            updateLocalRadio(name, streamURL, homepageURL, coverArtUrl)
        } else {
            updateServerRadio(name, streamURL, homepageURL)
        }
    }

    private fun updateLocalRadio(name: String, streamURL: String, homepageURL: String?, coverArtUrl: String?) {
        radioToEdit?.id?.let { id ->
            radioRepository.updateLocalStation(id, name, streamURL, homepageURL, coverArtUrl) {
                _isSuccess.postValue(true)
            }
        }
    }

    private fun updateServerRadio(name: String, streamURL: String, homepageURL: String?) {
        val id = radioToEdit?.id ?: return
        viewModelScope.launch {
            val response = radioRepository.updateInternetRadioStation(id, name, streamURL, homepageURL)
            handleServerResponse(response)
        }
    }

    fun deleteRadio() {
        val id = radioToEdit?.id ?: return

        _errorMessage.value = null
        _isSuccess.value = false

        if (isLocal) {
            deleteLocalRadio()
        } else {
            deleteServerRadio()
        }
    }

    private fun deleteLocalRadio() {
        radioToEdit?.id?.let { id ->
            radioRepository.deleteLocalStation(id) { _isSuccess.postValue(true) }
        }
    }

    private fun deleteServerRadio() {
        val id = radioToEdit?.id ?: return
        viewModelScope.launch {
            val response = radioRepository.deleteInternetRadioStation(id)
            handleServerResponse(response)
        }
    }

    private fun handleServerResponse(response: SubsonicResponse?) {
        if (response == null) {
            _errorMessage.postValue("Network error")
            return
        }

        if (response.status == "ok") {
            _isSuccess.postValue(true)
        } else {
            val errorMsg = response.error?.message ?: "Unknown server error"
            if (errorMsg == "Not implemented") {
                _errorMessage.postValue(getApplication<Application>().getString(R.string.radio_dialog_not_supported_snackbar))
            } else {
                _errorMessage.postValue(errorMsg)
            }
        }
    }
}
