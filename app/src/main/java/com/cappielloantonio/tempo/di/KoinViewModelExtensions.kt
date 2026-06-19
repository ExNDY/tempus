package com.cappielloantonio.tempo.di

import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.viewmodel.LoginViewModel
import com.cappielloantonio.tempo.viewmodel.RadioEditorViewModel
import com.cappielloantonio.tempo.viewmodel.SearchViewModel
import org.koin.core.Koin

@UnstableApi
fun Koin.getLoginViewModel(): LoginViewModel = get()

@UnstableApi
fun Koin.getRadioEditorViewModel(): RadioEditorViewModel = get()

@UnstableApi
fun Koin.getSearchViewModel(): SearchViewModel = get()
