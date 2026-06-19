package com.cappielloantonio.tempo.di

import com.cappielloantonio.tempo.viewmodel.*
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { MainViewModel(androidApplication(), get()) }
    viewModel { LoginViewModel(androidApplication(), get()) }
    viewModel { AlbumCatalogueViewModel(androidApplication()) }
    viewModel { ArtistCatalogueViewModel(androidApplication()) }
    viewModel { RadioEditorViewModel(androidApplication()) }
    viewModel { PodcastChannelEditorViewModel(androidApplication()) }
    viewModel { PodcastChannelBottomSheetViewModel(androidApplication()) }
    viewModel { PodcastEpisodeBottomSheetViewModel(androidApplication()) }
    viewModel { StarredArtistsSyncViewModel(androidApplication()) }
    viewModel { ArtistPageViewModel(androidApplication()) }
    viewModel { ArtistBottomSheetViewModel(androidApplication()) }
    viewModel { PlaylistChooserViewModel(androidApplication()) }
}
