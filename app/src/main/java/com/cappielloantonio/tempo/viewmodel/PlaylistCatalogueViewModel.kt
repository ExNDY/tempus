package com.cappielloantonio.tempo.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.repository.PlaylistRepository
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.Preferences

@UnstableApi
class PlaylistCatalogueViewModel(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private var type: String? = null
    private val sortOrder = MutableLiveData<String>()
    private val playlistList = MutableLiveData<List<Playlist>?>(null)
    private val sortedPlaylistList = MediatorLiveData<List<Playlist>>()
    private var sortedPlaylistSource: LiveData<List<Playlist>>? = null

    init {
        sortOrder.value = Preferences.getHomeSortPlaylists()
        updateSortedPlaylistList()
    }

    fun getPlaylistList(): LiveData<List<Playlist>?> {
        if (playlistList.value == null) {
            loadPlaylistList()
        }
        return playlistList
    }

    fun setSortOrder(order: String) {
        Log.d("TempusLog", "ViewModel setSortOrder called with: $order")
        Preferences.setHomeSortPlaylists(order)
        sortOrder.value = order
        updateSortedPlaylistList()
    }

    fun getSortedPlaylistList(): LiveData<List<Playlist>> = sortedPlaylistList

    fun setType(type: String?) {
        this.type = type
    }

    fun getType(): String? = type

    private fun loadPlaylistList() {
        val source = playlistRepository.getPlaylists(false, -1)
        source.observeForever(object : Observer<List<Playlist>> {
            override fun onChanged(value: List<Playlist>) {
                playlistList.postValue(value)
                source.removeObserver(this)
            }
        })
    }

    private fun updateSortedPlaylistList() {
        val order = sortOrder.value ?: return
        sortedPlaylistSource?.let { sortedPlaylistList.removeSource(it) }
        val source = playlistRepository.getSortedPlaylists(order)
        sortedPlaylistSource = source
        sortedPlaylistList.addSource(source) { playlists ->
            sortedPlaylistList.value = playlists
        }
    }
}
