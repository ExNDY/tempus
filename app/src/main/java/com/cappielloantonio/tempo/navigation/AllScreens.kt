package com.cappielloantonio.tempo.navigation

import com.cappielloantonio.tempo.ui.album.AlbumBottomSheetRouteScreen
import com.cappielloantonio.tempo.ui.album.AlbumCatalogueRouteScreen
import com.cappielloantonio.tempo.ui.album.AlbumListPageRouteScreen
import com.cappielloantonio.tempo.ui.album.AlbumPageRouteScreen
import com.cappielloantonio.tempo.ui.artist.ArtistBottomSheetRouteScreen
import com.cappielloantonio.tempo.ui.artist.ArtistCatalogueRouteScreen
import com.cappielloantonio.tempo.ui.artist.ArtistListPageRouteScreen
import com.cappielloantonio.tempo.ui.artist.ArtistPageRouteScreen
import com.cappielloantonio.tempo.ui.download.DownloadRouteScreen
import com.cappielloantonio.tempo.ui.download.DownloadedBottomSheetRouteScreen
import com.cappielloantonio.tempo.ui.dialog.BottomSheetRouteScreen
import com.cappielloantonio.tempo.ui.dialog.DialogRouteScreen
import com.cappielloantonio.tempo.ui.equalizer.EqualizerRouteScreen
import com.cappielloantonio.tempo.ui.filter.FilterRouteScreen
import com.cappielloantonio.tempo.ui.folder.DirectoryRouteScreen
import com.cappielloantonio.tempo.ui.folder.IndexRouteScreen
import com.cappielloantonio.tempo.ui.auth.LandingRouteScreen
import com.cappielloantonio.tempo.ui.auth.LoginRouteScreen
import com.cappielloantonio.tempo.ui.genre.GenreCatalogueRouteScreen
import com.cappielloantonio.tempo.ui.home.HomeRouteScreen
import com.cappielloantonio.tempo.ui.home.LibraryRouteScreen
import com.cappielloantonio.tempo.ui.player.PlayerArtistChooserRouteScreen
import com.cappielloantonio.tempo.ui.player.PlayerRouteScreen
import com.cappielloantonio.tempo.ui.playlist.PlaylistBottomSheetRouteScreen
import com.cappielloantonio.tempo.ui.playlist.PlaylistCatalogueRouteScreen
import com.cappielloantonio.tempo.ui.playlist.PlaylistPageRouteScreen
import com.cappielloantonio.tempo.ui.search.SearchRouteScreen
import com.cappielloantonio.tempo.ui.settings.SettingsRouteScreen
import com.cappielloantonio.tempo.ui.song.SongBottomSheetRouteScreen
import com.cappielloantonio.tempo.ui.song.SongListPageRouteScreen

internal val authScreens: List<Screen> = listOf(
    LandingRouteScreen,
    LoginRouteScreen,
)

internal val mainScreens: List<Screen> = listOf(
    HomeRouteScreen,
    LibraryRouteScreen,
    DownloadRouteScreen,
    SearchRouteScreen,
    EqualizerRouteScreen,
    SettingsRouteScreen,
    ArtistCatalogueRouteScreen,
    AlbumCatalogueRouteScreen,
    ArtistListPageRouteScreen,
    ArtistPageRouteScreen,
    AlbumListPageRouteScreen,
    AlbumPageRouteScreen,
    GenreCatalogueRouteScreen,
    PlaylistCatalogueRouteScreen,
    PlaylistPageRouteScreen,
    SongListPageRouteScreen,
    IndexRouteScreen,
    DirectoryRouteScreen,
    FilterRouteScreen,
    SongBottomSheetRouteScreen,
    AlbumBottomSheetRouteScreen,
    ArtistBottomSheetRouteScreen,
    PlaylistBottomSheetRouteScreen,
    DownloadedBottomSheetRouteScreen,
    DialogRouteScreen,
    BottomSheetRouteScreen,
    PlayerArtistChooserRouteScreen,
    PlayerRouteScreen,
)
