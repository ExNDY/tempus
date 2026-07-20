package com.cappielloantonio.tempo.di

import com.cappielloantonio.tempo.viewmodel.AlbumBottomSheetViewModel
import com.cappielloantonio.tempo.viewmodel.AlbumCatalogueViewModel
import com.cappielloantonio.tempo.viewmodel.AlbumListPageViewModel
import com.cappielloantonio.tempo.viewmodel.AlbumPageViewModel
import com.cappielloantonio.tempo.viewmodel.ArtistBottomSheetViewModel
import com.cappielloantonio.tempo.viewmodel.ArtistCatalogueViewModel
import com.cappielloantonio.tempo.viewmodel.ArtistListPageViewModel
import com.cappielloantonio.tempo.viewmodel.ArtistPageViewModel
import com.cappielloantonio.tempo.viewmodel.DirectoryViewModel
import com.cappielloantonio.tempo.viewmodel.DownloadViewModel
import com.cappielloantonio.tempo.viewmodel.DownloadedBottomSheetViewModel
import com.cappielloantonio.tempo.viewmodel.FilterViewModel
import com.cappielloantonio.tempo.viewmodel.GenreCatalogueViewModel
import com.cappielloantonio.tempo.viewmodel.HomeRearrangementViewModel
import com.cappielloantonio.tempo.viewmodel.HomeViewModel
import com.cappielloantonio.tempo.viewmodel.IndexViewModel
import com.cappielloantonio.tempo.viewmodel.LibraryViewModel
import com.cappielloantonio.tempo.viewmodel.LoginViewModel
import com.cappielloantonio.tempo.viewmodel.MainViewModel
import com.cappielloantonio.tempo.viewmodel.PlaybackViewModel
import com.cappielloantonio.tempo.viewmodel.PlayerBottomSheetViewModel
import com.cappielloantonio.tempo.viewmodel.PlaylistBottomSheetViewModel
import com.cappielloantonio.tempo.viewmodel.PlaylistCatalogueViewModel
import com.cappielloantonio.tempo.viewmodel.PlaylistChooserViewModel
import com.cappielloantonio.tempo.viewmodel.PlaylistEditorViewModel
import com.cappielloantonio.tempo.viewmodel.PlaylistPageViewModel
import com.cappielloantonio.tempo.viewmodel.RatingViewModel
import com.cappielloantonio.tempo.viewmodel.SearchViewModel
import com.cappielloantonio.tempo.viewmodel.SettingViewModel
import com.cappielloantonio.tempo.viewmodel.ShareBottomSheetViewModel
import com.cappielloantonio.tempo.viewmodel.SongBottomSheetViewModel
import com.cappielloantonio.tempo.viewmodel.SongListPageViewModel
import com.cappielloantonio.tempo.viewmodel.StarredAlbumsSyncViewModel
import com.cappielloantonio.tempo.viewmodel.StarredArtistsSyncViewModel
import com.cappielloantonio.tempo.viewmodel.StarredSyncViewModel
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
    viewModel { StarredSyncViewModel(get()) }
    viewModel { StarredAlbumsSyncViewModel(get()) }
    viewModel { SettingViewModel(get()) }
    viewModel { FilterViewModel(get()) }
    viewModel { GenreCatalogueViewModel(get()) }
    viewModel { IndexViewModel(get()) }
    viewModel { ShareBottomSheetViewModel(get()) }
    viewModel { PlaybackViewModel(get()) }
    viewModel { RatingViewModel(get(), get(), get()) }
    viewModel { PlaylistCatalogueViewModel(get(), get()) }
    viewModel { PlaylistPageViewModel(get(), get()) }
    viewModel { PlaylistBottomSheetViewModel(get(), get()) }
    viewModel { StarredArtistsSyncViewModel(get()) }
    viewModel { ArtistPageViewModel(get(), get(), get(), get()) }
    viewModel { ArtistBottomSheetViewModel(get(), get(), get()) }
    viewModel { HomeRearrangementViewModel() }
    viewModel { PlaylistChooserViewModel(get()) }
    viewModel { SongListPageViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { PlaylistEditorViewModel(get(), get()) }
    viewModel { DownloadViewModel(get()) }
    viewModel { DownloadedBottomSheetViewModel(get()) }
    viewModel { LibraryViewModel(get(), get(), get(), get(), get()) }
    viewModel { AlbumPageViewModel(get(), get(), get(), get()) }
    viewModel { AlbumBottomSheetViewModel(get(), get(), get(), get()) }
    viewModel { SongBottomSheetViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { PlayerBottomSheetViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
}
