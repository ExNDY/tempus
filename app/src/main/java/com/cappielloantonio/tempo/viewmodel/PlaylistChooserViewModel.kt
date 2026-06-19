package com.cappielloantonio.tempo.viewmodel

import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.cappielloantonio.tempo.repository.PlaylistRepository
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Playlist
import java.util.ArrayList

class PlaylistChooserViewModel(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val songIds = ArrayList<String>()
    private val parcelableSongs = ArrayList<Child>()
    var isPlaylistPublic = true
        private set

    fun setIsPlaylistPublic(isPublic: Boolean) {
        isPlaylistPublic = isPublic
    }

    fun setSongsToAdd(songs: ArrayList<Child>?) {
        songIds.clear()
        parcelableSongs.clear()
        songs?.forEach {
            parcelableSongs.add(it)
            songIds.add(it.id)
        }
    }

    fun getSongsToAdd(): ArrayList<Child> = parcelableSongs

    fun getPlaylistList(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<Playlist>> {
        return playlistRepository.getAllPlaylists(owner)
    }

    fun addSongsToPlaylist(fragment: DialogFragment, dialog: android.app.Dialog?, playlistId: String) {
        playlistRepository.addSongToPlaylist(
            playlistId,
            songIds,
            isPlaylistPublic,
            object : PlaylistRepository.AddToPlaylistCallback {
                override fun onSuccess() {
                    fragment.dismiss()
                }

                override fun onFailure() {}
                override fun onAllSkipped() {}
            }
        )
    }
}
