package com.cappielloantonio.tempo.di

import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.database.AppDatabase
import com.cappielloantonio.tempo.util.Preferences
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext

@OptIn(UnstableApi::class)
val appModule = module {
    single { Preferences }
    single { AppDatabase.getInstance() }
    single { get<AppDatabase>().serverDao() }
    single { get<AppDatabase>().queueDao() }
}
