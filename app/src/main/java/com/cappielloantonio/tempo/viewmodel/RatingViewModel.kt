package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.SongRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child

class RatingViewModel(
    private val songRepository: SongRepository,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository
) : ViewModel() {

    private var song: Child? = null
    private var album: AlbumID3? = null
    private var artist: ArtistID3? = null

    fun getSong(): Child? = song

    fun getLiveSong(): LiveData<Child?> {
        val currentSong = requireNotNull(song) { "Song must be set before observing rating" }
        val songId = requireNotNull(currentSong.id) { "Song id must not be null" }
        return songRepository.getSong(songId)
    }

    fun setSong(song: Child?) {
        this.song = song
        this.album = null
        this.artist = null
    }

    fun getAlbum(): AlbumID3? = album

    fun getLiveAlbum(): LiveData<AlbumID3?> {
        val currentAlbum = requireNotNull(album) { "Album must be set before observing rating" }
        val albumId = requireNotNull(currentAlbum.id) { "Album id must not be null" }
        return albumRepository.getAlbum(albumId)
    }

    fun setAlbum(album: AlbumID3?) {
        this.song = null
        this.album = album
        this.artist = null
    }

    fun getArtist(): ArtistID3? = artist

    fun getLiveArtist(): LiveData<ArtistID3?> {
        val currentArtist = requireNotNull(artist) { "Artist must be set before observing rating" }
        val artistId = requireNotNull(currentArtist.id) { "Artist id must not be null" }
        return artistRepository.getArtist(artistId)
    }

    fun setArtist(artist: ArtistID3?) {
        this.song = null
        this.album = null
        this.artist = artist
    }

    fun rate(star: Int) {
        when {
            song?.id != null -> songRepository.setRating(requireNotNull(song?.id), star)
            album?.id != null -> albumRepository.setRating(requireNotNull(album?.id), star)
            artist?.id != null -> artistRepository.setRating(requireNotNull(artist?.id), star)
        }
    }
}
