package com.cappielloantonio.tempo.di

import com.cappielloantonio.tempo.viewmodel.*
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { MainViewModel(get(), get(), get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { AlbumCatalogueViewModel(get()) }
    viewModel { ArtistCatalogueViewModel(get(), get()) }
    viewModel { AlbumListPageViewModel(get(), get()) }
    viewModel { ArtistListPageViewModel(get(), get()) }
    viewModel { DirectoryViewModel(get()) }
    viewModel { RadioViewModel(get()) }
    viewModel { StarredSyncViewModel(get()) }
    viewModel { StarredAlbumsSyncViewModel(get()) }
    viewModel { RadioEditorViewModel(get()) }
    viewModel { SettingViewModel(get()) }
    viewModel { FilterViewModel(get()) }
    viewModel { GenreCatalogueViewModel(get()) }
    viewModel { IndexViewModel(get()) }
    viewModel { ShareBottomSheetViewModel(get()) }
    viewModel { PlaybackViewModel() }
    viewModel { RatingViewModel(get(), get(), get()) }
    viewModel { PodcastChannelCatalogueViewModel(get()) }
    viewModel { PodcastViewModel(get()) }
    viewModel { PlaylistCatalogueViewModel(get(), get()) }
    viewModel { PlaylistPageViewModel(get()) }
    viewModel { PlaylistBottomSheetViewModel(get(), get()) }
    viewModel { PodcastChannelEditorViewModel(get()) }
    viewModel { PodcastChannelBottomSheetViewModel(get()) }
    viewModel { PodcastEpisodeBottomSheetViewModel(get()) }
    viewModel { StarredArtistsSyncViewModel(get()) }
    viewModel { ArtistPageViewModel(get(), get(), get()) }
    viewModel { ArtistBottomSheetViewModel(get(), get(), get()) }
    viewModel { HomeRearrangementViewModel() }
    viewModel { PlaylistChooserViewModel(get()) }
    viewModel { SongListPageViewModel(get(), get(), get(), get(), get()) }
    viewModel { PlaylistEditorViewModel(get(), get()) }
    viewModel { DownloadViewModel(get()) }
    viewModel { DownloadedBottomSheetViewModel() }
    viewModel { LibraryViewModel(get(), get(), get(), get(), get()) }
    viewModel { AlbumPageViewModel(get(), get(), get()) }
    viewModel { AlbumBottomSheetViewModel(get(), get(), get(), get()) }
    viewModel { SongBottomSheetViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { PlayerBottomSheetViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { PodcastChannelPageViewModel(get()) }
}
