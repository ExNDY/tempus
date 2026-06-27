package com.cappielloantonio.tempo.di

import androidx.media3.common.util.UnstableApi
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
import com.cappielloantonio.tempo.viewmodel.PodcastChannelCatalogueViewModel
import com.cappielloantonio.tempo.viewmodel.PodcastChannelPageViewModel
import com.cappielloantonio.tempo.viewmodel.PodcastViewModel
import com.cappielloantonio.tempo.viewmodel.RadioEditorViewModel
import com.cappielloantonio.tempo.viewmodel.RadioViewModel
import com.cappielloantonio.tempo.viewmodel.RatingViewModel
import com.cappielloantonio.tempo.viewmodel.SearchViewModel
import com.cappielloantonio.tempo.viewmodel.SongBottomSheetViewModel
import com.cappielloantonio.tempo.viewmodel.SongListPageViewModel
import org.koin.core.Koin

@UnstableApi
fun Koin.getHomeViewModel(): HomeViewModel = get()

@UnstableApi
fun Koin.getLibraryViewModel(): LibraryViewModel = get()

@UnstableApi
fun Koin.getIndexViewModel(): IndexViewModel = get()

@UnstableApi
fun Koin.getFilterViewModel(): FilterViewModel = get()

@UnstableApi
fun Koin.getLoginViewModel(): LoginViewModel = get()

@UnstableApi
fun Koin.getRadioEditorViewModel(): RadioEditorViewModel = get()

@UnstableApi
fun Koin.getRadioViewModel(): RadioViewModel = get()

@UnstableApi
fun Koin.getPodcastViewModel(): PodcastViewModel = get()

@UnstableApi
fun Koin.getPlaybackViewModel(): PlaybackViewModel = get()

@UnstableApi
fun Koin.getRatingViewModel(): RatingViewModel = get()

@UnstableApi
fun Koin.getPlayerBottomSheetViewModel(): PlayerBottomSheetViewModel = get()

@UnstableApi
fun Koin.getSearchViewModel(): SearchViewModel = get()

@UnstableApi
fun Koin.getSongBottomSheetViewModel(): SongBottomSheetViewModel = get()

@UnstableApi
fun Koin.getAlbumBottomSheetViewModel(): AlbumBottomSheetViewModel = get()

@UnstableApi
fun Koin.getArtistBottomSheetViewModel(): ArtistBottomSheetViewModel = get()

@UnstableApi
fun Koin.getPlaylistBottomSheetViewModel(): PlaylistBottomSheetViewModel = get()

@UnstableApi
fun Koin.getAlbumPageViewModel(): AlbumPageViewModel = get()

@UnstableApi
fun Koin.getAlbumListPageViewModel(): AlbumListPageViewModel = get()

@UnstableApi
fun Koin.getAlbumCatalogueViewModel(): AlbumCatalogueViewModel = get()

@UnstableApi
fun Koin.getArtistPageViewModel(): ArtistPageViewModel = get()

@UnstableApi
fun Koin.getArtistListPageViewModel(): ArtistListPageViewModel = get()

@UnstableApi
fun Koin.getArtistCatalogueViewModel(): ArtistCatalogueViewModel = get()

@UnstableApi
fun Koin.getGenreCatalogueViewModel(): GenreCatalogueViewModel = get()

@UnstableApi
fun Koin.getPlaylistCatalogueViewModel(): PlaylistCatalogueViewModel = get()

@UnstableApi
fun Koin.getPlaylistPageViewModel(): PlaylistPageViewModel = get()

@UnstableApi
fun Koin.getSongListPageViewModel(): SongListPageViewModel = get()

@UnstableApi
fun Koin.getPodcastChannelCatalogueViewModel(): PodcastChannelCatalogueViewModel = get()

@UnstableApi
fun Koin.getPodcastChannelPageViewModel(): PodcastChannelPageViewModel = get()

@UnstableApi
fun Koin.getDirectoryViewModel(): DirectoryViewModel = get()

@UnstableApi
fun Koin.getDownloadViewModel(): DownloadViewModel = get()

@UnstableApi
fun Koin.getDownloadedBottomSheetViewModel(): DownloadedBottomSheetViewModel = get()
