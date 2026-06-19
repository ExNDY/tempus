package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cappielloantonio.tempo.repository.PlaylistRepository
import com.cappielloantonio.tempo.repository.SharingRepository
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.subsonic.models.Share

class PlaylistEditorViewModel(
    private val playlistRepository: PlaylistRepository,
    private val sharingRepository: SharingRepository,
) : ViewModel() {

    private var toAdd: ArrayList<Child>? = null
    private var toEdit: Playlist? = null
    private var songLiveList = MutableLiveData<List<Child>?>()

    fun createPlaylist(name: String, callback: PlaylistRepository.PlaylistActionCallback?) {
        playlistRepository.createPlaylist(null, name, ArrayList(toAdd.orEmpty().map { it.id }), callback)
    }

    fun updatePlaylist(name: String, callback: PlaylistRepository.PlaylistActionCallback?) {
        val playlist = toEdit ?: return
        playlistRepository.updatePlaylist(playlist.id, name, getPlaylistSongIds(), callback)
    }

    fun deletePlaylist(callback: PlaylistRepository.PlaylistActionCallback?) {
        toEdit?.let { playlistRepository.deletePlaylist(it.id, callback) }
    }

    fun setSongsToAdd(songs: ArrayList<Child>?) {
        toAdd = songs
    }

    fun getSongsToAdd(): ArrayList<Child>? = toAdd

    fun getPlaylistToEdit(): Playlist? = toEdit

    fun setPlaylistToEdit(playlist: Playlist?) {
        toEdit = playlist
        songLiveList = if (playlist != null) {
            playlistRepository.getPlaylistSongs(playlist.id)
        } else {
            MutableLiveData()
        }
    }

    fun getPlaylistSongLiveList(): LiveData<List<Child>?> = songLiveList

    fun removeFromPlaylistSongLiveList(position: Int) {
        val songs = songLiveList.value?.toMutableList() ?: return
        songs.removeAt(position)
        songLiveList.postValue(songs)
    }

    fun orderPlaylistSongLiveListAfterSwap(songs: List<Child>) {
        songLiveList.postValue(songs)
    }

    fun sharePlaylist(): MutableLiveData<Share?> {
        val playlist = toEdit ?: return MutableLiveData()
        return sharingRepository.createShare(playlist.id, playlist.name, null)
    }

    private fun getPlaylistSongIds(): ArrayList<String> {
        val songs = songLiveList.value.orEmpty()
        val ids = ArrayList<String>(songs.size)

        for (song in songs) {
            ids.add(song.id)
        }

        return ids
    }
}
