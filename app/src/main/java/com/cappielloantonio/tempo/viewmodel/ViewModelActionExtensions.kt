package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.ViewModel as AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

fun <T> AndroidViewModel.sendAction(
    actions: Channel<T>,
    action: T,
) {
    viewModelScope.launch {
        actions.send(action)
    }
}

fun <T> dev.icerock.moko.mvvm.viewmodel.ViewModel.sendAction(
    actions: Channel<T>,
    action: T,
) {
    viewModelScope.launch {
        actions.send(action)
    }
}

