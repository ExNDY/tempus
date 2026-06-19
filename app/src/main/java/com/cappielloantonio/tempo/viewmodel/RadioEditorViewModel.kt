package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.ui.state.UiText
import dev.icerock.moko.mvvm.viewmodel.ViewModel
import com.cappielloantonio.tempo.repository.RadioRepository
import com.cappielloantonio.tempo.subsonic.models.InternetRadioStation
import com.cappielloantonio.tempo.subsonic.models.SubsonicResponse
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@UnstableApi
class RadioEditorViewModel(
    private val radioRepository: RadioRepository
) : ViewModel() {
    sealed interface Action {
        data class Saved(val isNew: Boolean) : Action
        data class ShowMessage(val message: UiText) : Action
        data object CloseDialog : Action
    }

    var radioToEdit: InternetRadioStation? = null
        private set
    var isLocal = false
        private set

    private val _actions = Channel<Action>(Channel.BUFFERED)
    val actions: LiveData<Action> = _actions.receiveAsFlow().asLiveData()

    fun setRadioToEdit(internetRadioStation: InternetRadioStation?) {
        this.radioToEdit = internetRadioStation
        this.isLocal = internetRadioStation != null && radioRepository.isLocalStation(internetRadioStation.id)
    }

    fun setLocal(local: Boolean) {
        isLocal = local
    }

    fun isEditing(): Boolean = radioToEdit != null

    fun createRadio(name: String, streamURL: String, homepageURL: String?, coverArtUrl: String?) {
        if (isLocal) {
            createLocalRadio(name, streamURL, homepageURL, coverArtUrl)
        } else {
            createServerRadio(name, streamURL, homepageURL)
        }
    }

    private fun createLocalRadio(name: String, streamURL: String, homepageURL: String?, coverArtUrl: String?) {
        viewModelScope.launch {
            radioRepository.createLocalStation(name, streamURL, homepageURL, coverArtUrl)
            sendAction(_actions, Action.Saved(radioToEdit == null))
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

        if (isLocal) {
            updateLocalRadio(name, streamURL, homepageURL, coverArtUrl)
        } else {
            updateServerRadio(name, streamURL, homepageURL)
        }
    }

    private fun updateLocalRadio(name: String, streamURL: String, homepageURL: String?, coverArtUrl: String?) {
        radioToEdit?.id?.let { id ->
            viewModelScope.launch {
                radioRepository.updateLocalStation(id, name, streamURL, homepageURL, coverArtUrl)
                sendAction(_actions, Action.Saved(false))
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

        if (isLocal) {
            deleteLocalRadio()
        } else {
            deleteServerRadio()
        }
    }

    private fun deleteLocalRadio() {
        radioToEdit?.id?.let { id ->
            viewModelScope.launch {
                radioRepository.deleteLocalStation(id)
                sendAction(_actions, Action.CloseDialog)
            }
        }
    }

    private fun deleteServerRadio() {
        val id = radioToEdit?.id ?: return
        viewModelScope.launch {
            val response = radioRepository.deleteInternetRadioStation(id)
            handleDeleteResponse(response)
        }
    }

    private fun handleServerResponse(response: SubsonicResponse?) {
        if (response == null) {
            postError("Network error")
            return
        }

        if (response.status == "ok") {
            if (radioToEdit == null) {
                sendAction(_actions, Action.Saved(true))
            } else {
                sendAction(_actions, Action.Saved(false))
            }
        } else {
            val errorMsg = response.error?.message ?: "Unknown server error"
            if (errorMsg == "Not implemented") {
                postError("Internet radio management are not supported by this server.")
            } else {
                postError(errorMsg)
            }
        }
    }

    private fun handleDeleteResponse(response: SubsonicResponse?) {
        if (response == null) {
            postError("Network error")
            return
        }

        if (response.status == "ok") {
            sendAction(_actions, Action.CloseDialog)
        } else {
            val errorMsg = response.error?.message ?: "Unknown server error"
            postError(errorMsg)
        }
    }

    private fun postError(message: String) {
        sendAction(_actions, Action.ShowMessage(UiText.raw(message)))
    }
}
