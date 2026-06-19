package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.DirectoryRepository
import com.cappielloantonio.tempo.repository.GenreRepository
import com.cappielloantonio.tempo.repository.PlaylistRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Genre
import com.cappielloantonio.tempo.subsonic.models.Indexes
import com.cappielloantonio.tempo.subsonic.models.MusicFolder
import com.cappielloantonio.tempo.subsonic.models.Playlist

class LibraryViewModel(
    private val directoryRepository: DirectoryRepository,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
    private val genreRepository: GenreRepository,
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {

    private val musicFolders = MutableLiveData<List<MusicFolder>?>(null)
    private val indexes = MutableLiveData<Indexes?>(null)
    private val playlistSample = MutableLiveData<List<Playlist>?>(null)
    private val sampleAlbum = MutableLiveData<List<AlbumID3>?>(null)
    private val sampleArtist = MutableLiveData<List<ArtistID3>?>(null)
    private val sampleGenres = MutableLiveData<List<Genre>?>(null)

    fun getMusicFolders(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<MusicFolder>?> {
        if (musicFolders.value == null) {
            directoryRepository.getMusicFolders().observe(owner) { musicFolders.postValue(it) }
        }

        return musicFolders
    }

    fun getIndexes(owner: androidx.lifecycle.LifecycleOwner): LiveData<Indexes?> {
        if (indexes.value == null) {
            directoryRepository.getIndexes("0", null).observe(owner) { indexes.postValue(it) }
        }

        return indexes
    }

    fun getAlbumSample(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<AlbumID3>?> {
        if (sampleAlbum.value == null) {
            albumRepository.getAlbums("random", 10, null, null).observe(owner) { sampleAlbum.postValue(it) }
        }

        return sampleAlbum
    }

    fun getArtistSample(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<ArtistID3>?> {
        if (sampleArtist.value == null) {
            artistRepository.getArtists(true, 10).observe(owner) { sampleArtist.postValue(it) }
        }

        return sampleArtist
    }

    fun getGenreSample(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<Genre>?> {
        if (sampleGenres.value == null) {
            genreRepository.getGenres(true, 15).observe(owner) { sampleGenres.postValue(it) }
        }

        return sampleGenres
    }

    fun getPlaylistSample(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<Playlist>?> {
        if (playlistSample.value == null) {
            playlistRepository.getPlaylists(true, 10).observe(owner) { playlistSample.postValue(it) }
        }

        return playlistSample
    }

    fun refreshAlbumSample(owner: androidx.lifecycle.LifecycleOwner) {
        albumRepository.getAlbums("random", 10, null, null).observe(owner) { sampleAlbum.postValue(it) }
    }

    fun refreshArtistSample(owner: androidx.lifecycle.LifecycleOwner) {
        artistRepository.getArtists(true, 10).observe(owner) { sampleArtist.postValue(it) }
    }

    fun refreshGenreSample(owner: androidx.lifecycle.LifecycleOwner) {
        genreRepository.getGenres(true, 15).observe(owner) { sampleGenres.postValue(it) }
    }

    fun refreshPlaylistSample(owner: androidx.lifecycle.LifecycleOwner) {
        playlistRepository.getPlaylists(true, 10).observe(owner) { playlistSample.postValue(it) }
    }
}
