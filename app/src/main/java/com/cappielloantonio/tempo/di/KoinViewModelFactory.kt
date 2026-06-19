package com.cappielloantonio.tempo.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.koin.java.KoinJavaComponent

class KoinViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return KoinJavaComponent.get(modelClass) as T
    }
}
