package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.model.PinnedPlaylist
import com.cappielloantonio.tempo.repository.PlaylistRepository
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Playlist

@UnstableApi
class PlaylistPageViewModel(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private var playlist: Playlist? = null
    private var isOffline = false

    private val songLiveList = MutableLiveData<List<Child>?>(null)
    private val playlistMissingEvent = MutableLiveData<Boolean>()

    init {
        playlistRepository.getPlaylistUpdateTrigger().observeForever { needsRefresh ->
            if (needsRefresh == true && playlist != null) {
                refreshSongs()
            }
        }
    }

    fun getPlaylistMissingEvent(): LiveData<Boolean> = playlistMissingEvent

    fun clearPlaylistMissingEvent() {
        playlistMissingEvent.value = false
    }

    fun getPlaylistSongLiveList(): LiveData<List<Child>?> {
        if (songLiveList.value == null && playlist != null) {
            refreshSongs()
        }
        return songLiveList
    }

    private fun refreshSongs() {
        val currentPlaylist = playlist ?: return
        val remoteData = playlistRepository.getPlaylistSongs(currentPlaylist.id)
        remoteData.observeForever(object : Observer<List<Child>?> {
            override fun onChanged(songs: List<Child>?) {
                if (songs.isNullOrEmpty()) {
                    playlistMissingEvent.postValue(true)
                } else {
                    songLiveList.postValue(songs)
                }
                remoteData.removeObserver(this)
            }
        })
    }

    fun getPlaylist(): Playlist? = playlist

    fun setPlaylist(playlist: Playlist) {
        if (this.playlist == null || this.playlist?.id != playlist.id) {
            this.playlist = playlist
            songLiveList.value = null
        }
    }

    @UnstableApi
    fun isPinned(owner: androidx.lifecycle.LifecycleOwner): LiveData<Boolean> {
        val isPinnedLive = MutableLiveData<Boolean>()
        playlistRepository.getPinnedPlaylists().observe(owner) { playlists ->
            isPinnedLive.postValue(playlists.any { it.playlistId == playlist?.id })
        }
        return isPinnedLive
    }

    @UnstableApi
    fun setPinned(isNowPinned: Boolean) {
        val currentPlaylist = playlist ?: return
        playlistRepository.insert(currentPlaylist)
        if (isNowPinned) {
            playlistRepository.pin(currentPlaylist.id)
        } else {
            playlistRepository.unpin(currentPlaylist.id)
        }
    }
}
