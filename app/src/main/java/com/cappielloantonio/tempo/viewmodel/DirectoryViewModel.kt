package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.cappielloantonio.tempo.repository.DirectoryRepository
import com.cappielloantonio.tempo.subsonic.models.Directory

class DirectoryViewModel(
    private val directoryRepository: DirectoryRepository
) : ViewModel() {

    fun loadMusicDirectory(id: String): LiveData<Directory?> {
        return directoryRepository.getMusicDirectory(id)
    }
}
