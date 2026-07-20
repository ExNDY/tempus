package com.cappielloantonio.tempo.di

import com.cappielloantonio.tempo.database.AppDatabase
import com.cappielloantonio.tempo.playback.PlaybackStateStore
import com.cappielloantonio.tempo.util.Preferences
import org.koin.dsl.module

val appModule = module {
    single { Preferences }
    single { PlaybackStateStore() }
    single { AppDatabase.getInstance() }
    single { get<AppDatabase>().serverDao() }
    single { get<AppDatabase>().queueDao() }
    single { get<AppDatabase>().recentSearchDao() }
}
