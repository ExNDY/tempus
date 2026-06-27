package com.cappielloantonio.tempo.network

import kotlinx.coroutines.flow.Flow

data class ConnectionState(
    val isNetworkAvailable: Boolean = false,
    val isServerAvailable: Boolean = false
)

interface NetworkConnectivityService {
    val connectionState: Flow<ConnectionState>
    fun setSelectedServer(url: String?)
}
