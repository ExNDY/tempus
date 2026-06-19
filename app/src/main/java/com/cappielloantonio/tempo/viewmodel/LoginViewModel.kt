package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.model.Server
import com.cappielloantonio.tempo.repository.ServerRepository

@UnstableApi
class LoginViewModel(
    application: Application,
    private val serverRepository: ServerRepository
) : AndroidViewModel(application) {

    var serverToEdit: Server? = null

    fun getServerList(): LiveData<List<Server>> {
        return serverRepository.getLiveServer()
    }

    fun addServer(server: Server) {
        serverRepository.insert(server)
    }

    fun deleteServer(server: Server) {
        serverRepository.delete(server)
    }

    fun selectServer(server: Server) {
        serverRepository.selectServer(server)
    }
}
