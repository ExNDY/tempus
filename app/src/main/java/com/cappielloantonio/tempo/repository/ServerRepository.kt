package com.cappielloantonio.tempo.repository

import androidx.lifecycle.LiveData
import com.cappielloantonio.tempo.database.dao.ServerDao
import com.cappielloantonio.tempo.model.Server
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.util.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@UnstableApi
class ServerRepository(
    private val serverDao: ServerDao,
    private val preferences: Preferences
) {

    fun getLiveServer(): LiveData<List<Server>> {
        return serverDao.getAll()
    }

    fun insert(server: Server) {
        CoroutineScope(Dispatchers.IO).launch {
            serverDao.insert(server)
        }
    }

    fun delete(server: Server) {
        CoroutineScope(Dispatchers.IO).launch {
            serverDao.delete(server)
        }
    }

    fun selectServer(server: Server) {
        preferences.setServerId(server.serverId)
        preferences.setServer(server.address)
        preferences.setUser(server.username)
        preferences.setPassword(server.password)
        preferences.setLocalAddress(server.localAddress)
        preferences.setLowSecurity(server.isLowSecurity)
        preferences.setClientCert(server.clientCert)
    }
}
