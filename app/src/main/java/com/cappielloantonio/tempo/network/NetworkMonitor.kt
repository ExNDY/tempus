package com.cappielloantonio.tempo.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest.Builder
import android.util.Log
import com.cappielloantonio.tempo.subsonic.utils.StringUtil
import com.cappielloantonio.tempo.util.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class NetworkMonitor(
    applicationContext: Context,
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : NetworkConnectivityService {

    private val connectivityManager: ConnectivityManager =
        applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val serverUrlFlow: MutableStateFlow<String?> = MutableStateFlow(null)

    override fun setSelectedServer(url: String?) {
        serverUrlFlow.value = url
    }

    private val systemNetworkFlow: Flow<Boolean> = callbackFlow {
        val callback = object : NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                val hasInternet =
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val isValidated =
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                trySend(hasInternet && isValidated)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        val request = Builder()
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
        if (!isNetworkAvailable) {
            return@combine ConnectionState(
                isNetworkAvailable = false,
                isServerAvailable = false
            )
        }
        if (serverUrl.isNullOrBlank()) {
            return@combine ConnectionState(
                isNetworkAvailable = true,
                isServerAvailable = false
            )
        }

        val isServerAvailable = checkServerReachability(urlStr = serverUrl)
        ConnectionState(
            isNetworkAvailable = true,
            isServerAvailable = isServerAvailable
        )
    }.flowOn(ioDispatcher)

    private suspend fun checkServerReachability(urlStr: String): Boolean =
        withContext(ioDispatcher) {
            try {
                val response = withTimeoutOrNull(timeout = 3000L.milliseconds) {
                    httpClient.get(urlString = buildPingUrl(urlStr)) {
                        parameter("u", Preferences.getUser())

                        val password = Preferences.getPassword()
                        if (Preferences.isLowSecurity()) {
                            parameter("p", password)
                        } else if (!password.isNullOrBlank()) {
                            val salt = UUID.randomUUID().toString()
                            parameter("t", StringUtil.tokenize(password + salt))
                            parameter("s", salt)
                        }

                        parameter("v", "1.16.1")
                        parameter("c", "Tempus")
                        parameter("f", "json")
                    }
                }
                response?.status?.value in 200..399
            } catch (e: Exception) {
                Log.e("NetworkMonitor", "Reachability check failed: ${e.message}")
                false
            }
        }

    private fun buildPingUrl(serverUrl: String): String {
        val normalizedUrl = when {
            serverUrl.startsWith("http://") || serverUrl.startsWith("https://") -> serverUrl
            else -> "http://$serverUrl"
        }.trimEnd('/')

        return "$normalizedUrl/rest/ping.view"
    }
}
