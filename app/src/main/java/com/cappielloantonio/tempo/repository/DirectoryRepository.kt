package com.cappielloantonio.tempo.repository
import androidx.lifecycle.MutableLiveData
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepository
import com.cappielloantonio.tempo.subsonic.models.Directory
import com.cappielloantonio.tempo.subsonic.models.Indexes
import com.cappielloantonio.tempo.subsonic.models.MusicFolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
class DirectoryRepository {
    private val subsonicRepository: SubsonicRepository = App.get(SubsonicRepository::class.java)
    fun getMusicFolders(): MutableLiveData<List<MusicFolder>> {
        val liveMusicFolders = MutableLiveData<List<MusicFolder>>()
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getMusicFolders()
            liveMusicFolders.postValue(response?.musicFolders?.musicFolders ?: emptyList())
        }
        return liveMusicFolders
    }
    fun getIndexes(musicFolderId: String?, ifModifiedSince: Long?): MutableLiveData<Indexes?> {
        val liveIndexes = MutableLiveData<Indexes?>()
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getIndexes(musicFolderId, ifModifiedSince)
            liveIndexes.postValue(response?.indexes)
        }
        return liveIndexes
    }
    fun getMusicDirectory(id: String): MutableLiveData<Directory?> {
        val liveMusicDirectory = MutableLiveData<Directory?>()
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getMusicDirectory(id)
            liveMusicDirectory.postValue(response?.directory)
        }
        return liveMusicDirectory
    }
}
