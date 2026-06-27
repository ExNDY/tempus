package com.cappielloantonio.tempo.network

import kotlinx.coroutines.flow.Flow

data class ConnectionState(
    val isNetworkAvailable: Boolean = true,
    val isServerAvailable: Boolean = true
)

interface NetworkConnectivityService {
    val connectionState: Flow<ConnectionState>
    fun setSelectedServer(url: String?)
}
