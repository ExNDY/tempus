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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class NetworkMonitor internal constructor(
    private val ioDispatcher: CoroutineDispatcher,
    private val systemNetworkFlow: Flow<Boolean>,
    private val retryIntervalMillis: Long,
    private val serverReachabilityChecker: suspend (String) -> Boolean,
) : NetworkConnectivityService {

    constructor(
        applicationContext: Context,
        httpClient: HttpClient,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : this(
        ioDispatcher = ioDispatcher,
        systemNetworkFlow = createSystemNetworkFlow(applicationContext),
        retryIntervalMillis = SERVER_UNAVAILABLE_RETRY_INTERVAL_MS,
        serverReachabilityChecker = { url: String ->
            checkServerReachability(
                httpClient = httpClient,
                urlStr = url
            )
        }
    )

    private val serverUrlFlow: MutableStateFlow<String?> = MutableStateFlow(null)
    private val refreshSignal = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override fun setSelectedServer(url: String?) {
        serverUrlFlow.value = url
    }

    override fun refresh() {
        refreshSignal.tryEmit(Unit)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val connectionState: Flow<ConnectionState> = combine(
        systemNetworkFlow,
        serverUrlFlow
    ) { isNetworkAvailable, serverUrl -> isNetworkAvailable to serverUrl }
        .distinctUntilChanged()
        .flatMapLatest { (isNetworkAvailable, serverUrl) ->
            when {
                !isNetworkAvailable -> flowOf(
                    ConnectionState(
                        isNetworkAvailable = false,
                        isServerAvailable = false
                    )
                )
                serverUrl.isNullOrBlank() -> flowOf(
                    ConnectionState(
                        isNetworkAvailable = true,
                        isServerAvailable = false
                    )
                )
                else -> retryingServerState(serverUrl)
            }
        }
        .distinctUntilChanged()
        .flowOn(ioDispatcher)

    private fun retryingServerState(serverUrl: String): Flow<ConnectionState> = flow {
        while (currentCoroutineContext().isActive) {
            val isServerAvailable = serverReachabilityChecker(serverUrl)
            emit(
                ConnectionState(
                    isNetworkAvailable = true,
                    isServerAvailable = isServerAvailable
                )
            )

            if (isServerAvailable) {
                refreshSignal.first()
            } else {
                withTimeoutOrNull(retryIntervalMillis) {
                    refreshSignal.first()
                }
            }
        }
    }

    companion object {
        private const val SERVER_UNAVAILABLE_RETRY_INTERVAL_MS = 30_000L

        private fun createSystemNetworkFlow(applicationContext: Context): Flow<Boolean> = callbackFlow {
            val connectivityManager: ConnectivityManager =
                applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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

        private suspend fun checkServerReachability(
            httpClient: HttpClient,
            urlStr: String
        ): Boolean {
            return try {
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
}
