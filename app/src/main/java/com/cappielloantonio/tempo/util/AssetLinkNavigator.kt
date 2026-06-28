package com.cappielloantonio.tempo.util

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.Observer
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.PlaylistRepository
import com.cappielloantonio.tempo.repository.SongRepository
import com.cappielloantonio.tempo.subsonic.models.Genre
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.fragment.bottomsheetdialog.SongBottomSheetDialog

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
                val dialog = SongBottomSheetDialog()
                val args = Bundle()
                args.putSerializable(Constants.TRACK_OBJECT, value)
                dialog.arguments = args
                dialog.show(activity.supportFragmentManager, null)
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
                val args = Bundle()
                args.putSerializable(Constants.ALBUM_OBJECT, value)
                navigateSafely(R.id.albumPageFragment, args)
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
                val args = Bundle()
                args.putSerializable(Constants.ARTIST_OBJECT, value)
                navigateSafely(R.id.artistPageFragment, args)
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
                val args = Bundle()
                args.putSerializable(Constants.PLAYLIST_OBJECT, value)
                navigateSafely(R.id.playlistPageFragment, args)
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
        val args = Bundle()
        args.putSerializable(Constants.GENRE_OBJECT, genre)
        args.putString(Constants.MEDIA_BY_GENRE, Constants.MEDIA_BY_GENRE)
        navigateSafely(R.id.songListPageFragment, args)
    }

    private fun openYear(yearValue: String) {
        try {
            val year = yearValue.trim().toInt()
            val args = Bundle()
            args.putInt("year_object", year)
            args.putString(Constants.MEDIA_BY_YEAR, Constants.MEDIA_BY_YEAR)
            navigateSafely(R.id.songListPageFragment, args)
        } catch (ex: NumberFormatException) {
            Toast.makeText(activity, R.string.asset_link_error_unsupported, Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateSafely(destinationId: Int, args: Bundle?) {
        activity.runOnUiThread {
            val navController = activity.navController
            if (navController == null) {
                return@runOnUiThread
            }
            if (navController.currentDestination?.id == destinationId) {
                navController.navigate(
                    destinationId,
                    args,
                    androidx.navigation.NavOptions.Builder().setLaunchSingleTop(true).build()
                )
            } else {
                navController.navigate(destinationId, args)
            }
        }
    }
}
