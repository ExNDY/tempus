package com.cappielloantonio.tempo.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import org.koin.core.Koin
import org.koin.core.context.GlobalContext

@Composable
inline fun <reified VM : ViewModel> getViewModel(
    noinline initializer: Koin.() -> VM,
): VM {
    val koin = remember<Koin> { GlobalContext.get() }
    val factory = remember(koin) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return initializer(koin) as T
            }
        }
    }

    return viewModel(factory = factory)
}
