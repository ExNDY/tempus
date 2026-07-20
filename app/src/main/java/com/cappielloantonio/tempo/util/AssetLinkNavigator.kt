package com.cappielloantonio.tempo.util

import android.widget.Toast
import androidx.lifecycle.Observer
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.PlaylistRepository
import com.cappielloantonio.tempo.repository.SongRepository
import com.cappielloantonio.tempo.subsonic.models.Genre
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.viewmodel.SongListPageArgs

class AssetLinkNavigator(private val activity: MainActivity) {
    private val songRepository = SongRepository()
    private val albumRepository = AlbumRepository()
    private val artistRepository = ArtistRepository()
    private val playlistRepository = PlaylistRepository()

    fun open(assetLink: AssetLinkUtil.AssetLink?) {
        if (assetLink == null) {
            return
        }
        when (assetLink.type) {
            AssetLinkUtil.TYPE_SONG -> openSong(assetLink.id)
            AssetLinkUtil.TYPE_ALBUM -> openAlbum(assetLink.id)
            AssetLinkUtil.TYPE_ARTIST -> openArtist(assetLink.id)
            AssetLinkUtil.TYPE_PLAYLIST -> openPlaylist(assetLink.id)
            AssetLinkUtil.TYPE_GENRE -> openGenre(assetLink.id)
            AssetLinkUtil.TYPE_YEAR -> openYear(assetLink.id)
            else -> Toast.makeText(activity, R.string.asset_link_error_unsupported, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSong(id: String) {
        val liveData = songRepository.getSong(id)
        val observer = object : Observer<com.cappielloantonio.tempo.subsonic.models.Child?> {
            override fun onChanged(value: com.cappielloantonio.tempo.subsonic.models.Child?) {
                liveData.removeObserver(this)
                if (value == null) {
                    Toast.makeText(activity, R.string.asset_link_error_song, Toast.LENGTH_SHORT).show()
                    return
                }
                activity.runOnUiThread {
                    activity.openSongBottomSheetRoute(value)
                }
            }
        }
        liveData.observe(activity, observer)
    }

    private fun openAlbum(id: String) {
        val liveData = albumRepository.getAlbum(id)
        val observer = object : Observer<com.cappielloantonio.tempo.subsonic.models.AlbumID3?> {
            override fun onChanged(value: com.cappielloantonio.tempo.subsonic.models.AlbumID3?) {
                liveData.removeObserver(this)
                if (value == null) {
                    Toast.makeText(activity, R.string.asset_link_error_album, Toast.LENGTH_SHORT).show()
                    return
                }
                activity.openAlbumRoute(value.id.orEmpty())
            }
        }
        liveData.observe(activity, observer)
    }

    private fun openArtist(id: String) {
        val liveData = artistRepository.getArtist(id)
        val observer = object : Observer<com.cappielloantonio.tempo.subsonic.models.ArtistID3?> {
            override fun onChanged(value: com.cappielloantonio.tempo.subsonic.models.ArtistID3?) {
                liveData.removeObserver(this)
                if (value == null) {
                    Toast.makeText(activity, R.string.asset_link_error_artist, Toast.LENGTH_SHORT).show()
                    return
                }
                activity.openArtistRoute(value.id.orEmpty())
            }
        }
        liveData.observe(activity, observer)
    }

    private fun openPlaylist(id: String) {
        val liveData = playlistRepository.getPlaylist(id)
        val observer = object : Observer<com.cappielloantonio.tempo.subsonic.models.Playlist?> {
            override fun onChanged(value: com.cappielloantonio.tempo.subsonic.models.Playlist?) {
                liveData.removeObserver(this)
                if (value == null) {
                    Toast.makeText(activity, R.string.asset_link_error_playlist, Toast.LENGTH_SHORT).show()
                    return
                }
                activity.openPlaylistRoute(value.id)
            }
        }
        liveData.observe(activity, observer)
    }

    private fun openGenre(genreName: String) {
        val trimmed = genreName.trim()
        if (trimmed.isEmpty()) {
            Toast.makeText(activity, R.string.asset_link_error_unsupported, Toast.LENGTH_SHORT).show()
            return
        }

        val genre = Genre()
        genre.genre = trimmed
        genre.songCount = 0
        genre.albumCount = 0
        activity.runOnUiThread {
            activity.openSongListRoute(
                SongListPageArgs(
                    type = Constants.MEDIA_BY_GENRE,
                    genre = genre,
                ),
            )
        }
    }

    private fun openYear(yearValue: String) {
        try {
            val year = yearValue.trim().toInt()
            activity.runOnUiThread {
                activity.openSongListRoute(
                    SongListPageArgs(
                        type = Constants.MEDIA_BY_YEAR,
                        year = year,
                    ),
                )
            }
        } catch (_: NumberFormatException) {
            Toast.makeText(activity, R.string.asset_link_error_unsupported, Toast.LENGTH_SHORT).show()
        }
    }
}
