package com.cappielloantonio.tempo.di

import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepository
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepositoryImpl
import com.cappielloantonio.tempo.subsonic.Subsonic
import com.cappielloantonio.tempo.subsonic.SubsonicPreferences
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.network.NetworkConnectivityService
import com.cappielloantonio.tempo.network.NetworkMonitor
import com.google.gson.Strictness
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.gson.*
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val networkModule = module {
    single {
        HttpClient(Android) {
            install(ContentNegotiation) {
                gson {
                    setStrictness(Strictness.LENIENT)
                }
            }
            install(Logging) {
                level = LogLevel.INFO
            }
        }
    }

    single<NetworkConnectivityService> {
        NetworkMonitor(
            applicationContext = androidContext(),
            httpClient = get<HttpClient>()
        ).apply {
            setSelectedServer(get<Preferences>().getInUseServerAddress())
        }
    }

    single<SubsonicRepository> { SubsonicRepositoryImpl(get()) }

    factory {
        val prefs = get<Preferences>()
        val subsonicPrefs = SubsonicPreferences()
        subsonicPrefs.serverUrl = prefs.getInUseServerAddress()
        subsonicPrefs.username = prefs.getUser()
        subsonicPrefs.setAuthentication(
            prefs.getPassword(),
            prefs.getToken(),
            prefs.getSalt(),
            prefs.isLowSecurity()
        )
        Subsonic(subsonicPrefs)
    }
}
