package com.cappielloantonio.tempo.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.koin.core.component.KoinComponent

class KoinViewModelFactory : ViewModelProvider.Factory, KoinComponent {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return getKoin().get(modelClass.kotlin, null, null)
    }
}
