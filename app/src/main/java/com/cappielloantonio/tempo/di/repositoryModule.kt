package com.cappielloantonio.tempo.di

import com.cappielloantonio.tempo.repository.*
import org.koin.dsl.module

val repositoryModule = module {
    single { SongRepository(get()) }
    single { SystemRepository(get()) }
    single { ServerRepository(get(), get()) }
    single { AlbumRepository() }
    single { ArtistRepository() }
    single { GenreRepository() }
    single { PlaylistRepository() }
    single { SearchingRepository() }
    single { PodcastRepository() }
    single { RadioRepository() }
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
