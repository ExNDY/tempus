package com.cappielloantonio.tempo.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun startDI(appDeclaration: KoinAppDeclaration? = null): KoinApplication {
    return startKoin {
        appDeclaration?.invoke(this)
        modules(
            listOf(
                appModule,
                networkModule,
                repositoryModule,
                viewModelModule
            )
        )
    }
}
