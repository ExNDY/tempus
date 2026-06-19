package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.cappielloantonio.tempo.repository.DirectoryRepository
import com.cappielloantonio.tempo.subsonic.models.Indexes
import com.cappielloantonio.tempo.subsonic.models.MusicFolder

class IndexViewModel(
    private val directoryRepository: DirectoryRepository
) : ViewModel() {

    private var musicFolder: MusicFolder? = null

    fun getIndexes(musicFolderId: String?): LiveData<Indexes?> {
        return directoryRepository.getIndexes(musicFolderId, null)
    }

    fun getMusicFolderName(): String {
        return musicFolder?.name.orEmpty()
    }

    fun setMusicFolder(musicFolder: MusicFolder?) {
        this.musicFolder = musicFolder
    }
}
