package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LiveData
import androidx.media3.common.util.UnstableApi
import dev.icerock.moko.mvvm.viewmodel.ViewModel
import com.cappielloantonio.tempo.model.Server
import com.cappielloantonio.tempo.repository.ServerRepository

@UnstableApi
class LoginViewModel(
    private val serverRepository: ServerRepository
) : ViewModel() {

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
