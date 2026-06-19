package com.cappielloantonio.tempo.di

import com.cappielloantonio.tempo.radiobrowser.RadioBrowserRepository
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepository
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepositoryImpl
import com.cappielloantonio.tempo.subsonic.Subsonic
import com.cappielloantonio.tempo.subsonic.SubsonicPreferences
import com.cappielloantonio.tempo.util.Preferences
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.gson.*
import org.koin.dsl.module

val networkModule = module {
    single {
        HttpClient(Android) {
            install(ContentNegotiation) {
                gson {
                    setLenient()
                }
            }
            install(Logging) {
                level = LogLevel.INFO
            }
        }
    }

    single<SubsonicRepository> { SubsonicRepositoryImpl(get()) }
    single { RadioBrowserRepository(get()) }

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
