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
import com.cappielloantonio.tempo.viewmodel.GenreCatalogueViewModel
import com.cappielloantonio.tempo.viewmodel.FilterViewModel
import com.cappielloantonio.tempo.viewmodel.HomeViewModel
import com.cappielloantonio.tempo.viewmodel.IndexViewModel
import com.cappielloantonio.tempo.viewmodel.LibraryViewModel
import com.cappielloantonio.tempo.viewmodel.LoginViewModel
import com.cappielloantonio.tempo.viewmodel.PlaybackViewModel
import com.cappielloantonio.tempo.viewmodel.PlaylistCatalogueViewModel
import com.cappielloantonio.tempo.viewmodel.PlaylistBottomSheetViewModel
import com.cappielloantonio.tempo.viewmodel.PlaylistPageViewModel
import com.cappielloantonio.tempo.viewmodel.PlayerBottomSheetViewModel
import com.cappielloantonio.tempo.viewmodel.RatingViewModel
import com.cappielloantonio.tempo.viewmodel.SearchViewModel
import com.cappielloantonio.tempo.viewmodel.SettingViewModel
import com.cappielloantonio.tempo.viewmodel.SongBottomSheetViewModel
import com.cappielloantonio.tempo.viewmodel.SongListPageViewModel
import org.koin.core.Koin

fun Koin.getHomeViewModel(): HomeViewModel = get()
fun Koin.getLibraryViewModel(): LibraryViewModel = get()
fun Koin.getIndexViewModel(): IndexViewModel = get()
fun Koin.getFilterViewModel(): FilterViewModel = get()
fun Koin.getLoginViewModel(): LoginViewModel = get()
fun Koin.getPlaybackViewModel(): PlaybackViewModel = get()
fun Koin.getRatingViewModel(): RatingViewModel = get()
fun Koin.getPlayerBottomSheetViewModel(): PlayerBottomSheetViewModel = get()
fun Koin.getSearchViewModel(): SearchViewModel = get()
fun Koin.getSettingViewModel(): SettingViewModel = get()
fun Koin.getSongBottomSheetViewModel(): SongBottomSheetViewModel = get()
fun Koin.getAlbumBottomSheetViewModel(): AlbumBottomSheetViewModel = get()
fun Koin.getArtistBottomSheetViewModel(): ArtistBottomSheetViewModel = get()
fun Koin.getPlaylistBottomSheetViewModel(): PlaylistBottomSheetViewModel = get()
fun Koin.getAlbumPageViewModel(): AlbumPageViewModel = get()
fun Koin.getAlbumListPageViewModel(): AlbumListPageViewModel = get()
fun Koin.getAlbumCatalogueViewModel(): AlbumCatalogueViewModel = get()
fun Koin.getArtistPageViewModel(): ArtistPageViewModel = get()
fun Koin.getArtistListPageViewModel(): ArtistListPageViewModel = get()
fun Koin.getArtistCatalogueViewModel(): ArtistCatalogueViewModel = get()
fun Koin.getGenreCatalogueViewModel(): GenreCatalogueViewModel = get()
fun Koin.getPlaylistCatalogueViewModel(): PlaylistCatalogueViewModel = get()
fun Koin.getPlaylistPageViewModel(): PlaylistPageViewModel = get()
fun Koin.getSongListPageViewModel(): SongListPageViewModel = get()
fun Koin.getDirectoryViewModel(): DirectoryViewModel = get()
fun Koin.getDownloadViewModel(): DownloadViewModel = get()
fun Koin.getDownloadedBottomSheetViewModel(): DownloadedBottomSheetViewModel = get()
