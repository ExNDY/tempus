package com.cappielloantonio.tempo.di

import com.cappielloantonio.tempo.repository.*
import org.koin.dsl.module

val repositoryModule = module {
    single { SongRepository(subsonicRepository = get()) }
    single { SystemRepository(subsonicRepository = get()) }
    single { ServerRepository(serverDao = get(), preferences = get()) }
    single { AlbumRepository() }
    single { ArtistRepository() }
    single { GenreRepository() }
    single { PlaylistRepository() }
    single { SearchingRepository(
        recentSearchDao = get(),
        subsonicRepository = get(),
        preferences = get()
    ) }
    single { DirectoryRepository() }
    single { FavoriteRepository() }
    single { DownloadRepository() }
    single { ChronologyRepository() }
    single { LyricsRepository() }
    single { OpenRepository() }
    single { ScanRepository() }
    single { SharingRepository() }
    single { QueueRepository() }
}
