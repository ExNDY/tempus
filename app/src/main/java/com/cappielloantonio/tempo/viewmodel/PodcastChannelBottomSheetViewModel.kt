package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.repository.PodcastRepository
import com.cappielloantonio.tempo.subsonic.models.PodcastChannel
import com.cappielloantonio.tempo.subsonic.models.SubsonicResponse
import kotlinx.coroutines.launch

@UnstableApi
class PodcastChannelBottomSheetViewModel(application: Application) : AndroidViewModel(application) {
    private val podcastRepository = PodcastRepository()
    var podcastChannel: PodcastChannel? = null

    fun deletePodcastChannel() {
        val channelId = podcastChannel?.id ?: return
        viewModelScope.launch {
            val response = podcastRepository.deletePodcastChannel(channelId)
            handleResponse(response)
        }
    }

    private fun handleResponse(response: SubsonicResponse?) {
        if (response == null) {
            Toast.makeText(getApplication(), "Network error", Toast.LENGTH_LONG).show()
            return
        }

        if (response.status == "ok") {
            Toast.makeText(getApplication(), "Podcast channel deleted", Toast.LENGTH_SHORT).show()
        } else {
            val errorMsg = response.error?.message ?: "Unknown server error"
            Toast.makeText(getApplication(), errorMsg, Toast.LENGTH_LONG).show()
        }
    }
}
