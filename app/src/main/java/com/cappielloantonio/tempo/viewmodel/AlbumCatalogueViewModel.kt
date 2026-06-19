package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import kotlinx.coroutines.launch

@UnstableApi
class AlbumCatalogueViewModel(application: Application) : AndroidViewModel(application) {
    private val albumList = MutableLiveData<List<AlbumID3>>(ArrayList())
    private val loading = MutableLiveData(true)
    private val subsonicRepository: SubsonicRepository = App.get(SubsonicRepository::class.java)

    private var page = 0
    private var status = Status.STOPPED

    fun getAlbumList(): LiveData<List<AlbumID3>> = albumList
    fun getLoadingStatus(): LiveData<Boolean> = loading

    fun loadAlbums() {
        page = 0
        status = Status.RUNNING
        albumList.value = ArrayList()
        loadAlbumsInternal(500)
    }

    fun stopLoading() {
        status = Status.STOPPED
    }

    private fun loadAlbumsInternal(size: Int) {
        viewModelScope.launch {
            if (status == Status.STOPPED) {
                loading.postValue(false)
                return@launch
            }

            val response = subsonicRepository.getAlbumList2("alphabeticalByName", size, size * page++, null, null)
            val media = response?.albumList2?.albums ?: emptyList()

            if (status == Status.STOPPED) {
                loading.postValue(false)
                return@launch
            }

            val currentList = albumList.value?.toMutableList() ?: mutableListOf()
            currentList.addAll(media)
            albumList.postValue(currentList)

            if (media.size == size) {
                loadAlbumsInternal(size)
                loading.postValue(true)
            } else {
                status = Status.STOPPED
                loading.postValue(false)
            }
        }
    }

    private enum class Status {
        RUNNING, STOPPED
    }
}
