package com.cappielloantonio.tempo.subsonic.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.cappielloantonio.tempo.App
import okhttp3.Interceptor
import okhttp3.Response

class CacheUtil(private val maxAge: Int, private val maxStale: Int) {
    val onlineInterceptor = Interceptor { chain ->
        val response: Response = chain.proceed(chain.request())
        response.newBuilder()
            .header("Cache-Control", "public, max-age=$maxAge")
            .removeHeader("Pragma")
            .build()
    }

    val offlineInterceptor = Interceptor { chain ->
        var request = chain.request()
        if (!isConnected) {
            request = request.newBuilder()
                .header("Cache-Control", "public, only-if-cached, max-stale=$maxStale")
                .removeHeader("Pragma")
                .build()
        }
        chain.proceed(request)
    }

    private val isConnected: Boolean
        get() {
            val connectivityManager = App.getContext().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            if (!hasInternet) {
                return false
            }
            val hasAppropriateTransport = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)
            return hasAppropriateTransport
        }
}
