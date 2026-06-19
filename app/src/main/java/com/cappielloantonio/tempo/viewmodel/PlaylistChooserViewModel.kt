package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import android.app.Dialog
import android.os.Parcelable
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.repository.PlaylistRepository
import com.cappielloantonio.tempo.subsonic.models.Playlist
import java.util.ArrayList

@UnstableApi
class PlaylistChooserViewModel(application: Application) : AndroidViewModel(application) {
    private val playlistRepository = PlaylistRepository()
    private var _songIds = ArrayList<String>()
    private var _parcelableSongs = ArrayList<Parcelable>()
    var isPlaylistPublic = true

    fun setIsPlaylistPublic(isPublic: Boolean) {
        isPlaylistPublic = isPublic
    }

    fun setSongsToAdd(songs: ArrayList<Parcelable>?) {
        _songIds.clear()
        _parcelableSongs.clear()
        songs?.forEach { 
            _parcelableSongs.add(it)
            if (it is com.cappielloantonio.tempo.subsonic.models.Child) _songIds.add(it.id) 
        }
    }

    fun getSongsToAdd(): ArrayList<Parcelable> = _parcelableSongs

    fun getPlaylistList(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<Playlist>> {
        return playlistRepository.getAllPlaylists(owner)
    }

    fun addSongsToPlaylist(fragment: DialogFragment, dialog: Dialog?, playlistId: String) {
        playlistRepository.addSongToPlaylist(playlistId, _songIds, isPlaylistPublic, object : PlaylistRepository.AddToPlaylistCallback {
            override fun onSuccess() {
                fragment.dismiss()
            }
            override fun onFailure() {}
            override fun onAllSkipped() {}
        })
    }
}
