package com.cappielloantonio.tempo.repository

import androidx.lifecycle.MutableLiveData
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepository
import com.cappielloantonio.tempo.subsonic.models.LyricsList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@UnstableApi
class OpenRepository {
    private val subsonicRepository: SubsonicRepository = App.get(SubsonicRepository::class.java)

    fun getLyricsBySongId(id: String): MutableLiveData<LyricsList?> {
        val lyricsList = MutableLiveData<LyricsList?>()
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getLyricsBySongId(id)
            lyricsList.postValue(response?.lyricsList)
        }
        return lyricsList
    }
}
