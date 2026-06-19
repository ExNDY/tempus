package com.cappielloantonio.tempo.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.ui.state.UiEvent
import com.cappielloantonio.tempo.ui.state.UiText
import com.cappielloantonio.tempo.repository.PodcastRepository
import com.cappielloantonio.tempo.subsonic.models.SubsonicResponse
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@UnstableApi
class PodcastChannelEditorViewModel(
    private val podcastRepository: PodcastRepository,
) : androidx.lifecycle.ViewModel() {
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: LiveData<UiEvent> = _events.receiveAsFlow().asLiveData()

    fun clearError() {
        _errorMessage.value = null
    }

    fun createChannel(url: String) {
        _errorMessage.value = null

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
            sendAction(_events, UiEvent.CloseDialog)
        } else {
            val errorMsg = response.error?.message ?: "Unknown server error"
            showError(errorMsg)
        }
    }

    private fun showError(message: String) {
        _errorMessage.postValue(message)
        Log.e("PodcastChannelEditorVM", "Error: $message")
        sendAction(_events, UiEvent.ShowMessage(UiText.raw(message)))
    }
}
