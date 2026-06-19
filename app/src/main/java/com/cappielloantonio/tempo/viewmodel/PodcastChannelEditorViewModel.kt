package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.repository.PodcastRepository
import com.cappielloantonio.tempo.subsonic.models.SubsonicResponse
import kotlinx.coroutines.launch

@UnstableApi
class PodcastChannelEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val podcastRepository = PodcastRepository()
    private val _isSuccess = MutableLiveData(false)
    val isSuccess: LiveData<Boolean> = _isSuccess
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun clearError() {
        _errorMessage.value = null
    }

    fun createChannel(url: String) {
        _errorMessage.value = null
        _isSuccess.value = false

        viewModelScope.launch {
            val response = podcastRepository.createPodcastChannel(url)
            handleResponse(response)
        }
    }

    private fun handleResponse(response: SubsonicResponse?) {
        if (response == null) {
            showError("Network error")
            return
        }

        if (response.status == "ok") {
            _isSuccess.postValue(true)
        } else {
            val errorMsg = response.error?.message ?: "Unknown server error"
            showError(errorMsg)
        }
    }

    private fun showError(message: String) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_LONG).show()
        _errorMessage.postValue(message)
        Log.e("PodcastChannelEditorVM", "Error: $message")
    }
}
