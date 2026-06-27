package com.cappielloantonio.tempo.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.head
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class NetworkMonitor(
    applicationContext: Context,
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : NetworkConnectivityService {

    private val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val serverUrlFlow = MutableStateFlow<String?>(null)

    override fun setSelectedServer(url: String?) {
        serverUrlFlow.value = url
    }

    private val systemNetworkFlow: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                trySend(hasInternet && isValidated)
            }
            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)
        
        // Initial state
        val currentNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(currentNetwork)
        trySend(caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true)

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    override val connectionState: Flow<ConnectionState> = combine(
        systemNetworkFlow,
        serverUrlFlow
    ) { isNetworkAvailable, serverUrl ->
        if (!isNetworkAvailable) return@combine ConnectionState(false, false)
        if (serverUrl.isNullOrBlank()) return@combine ConnectionState(true, false)

        val isServerAvailable = checkServerReachability(serverUrl)
        ConnectionState(isNetworkAvailable = true, isServerAvailable = isServerAvailable)
    }.flowOn(ioDispatcher)

    private suspend fun checkServerReachability(urlStr: String): Boolean = withContext(ioDispatcher) {
        try {
            val response = withTimeoutOrNull(3000L) { httpClient.head(urlStr) }
            response?.status?.value in 200..399
        } catch (e: Exception) {
            Log.e("NetworkMonitor", "Reachability check failed: ${e.message}")
            false
        }
    }
}
