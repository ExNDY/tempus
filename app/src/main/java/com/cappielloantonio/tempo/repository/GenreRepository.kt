package com.cappielloantonio.tempo.repository

import androidx.lifecycle.MutableLiveData
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepository
import com.cappielloantonio.tempo.subsonic.models.Genre
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@UnstableApi
class GenreRepository {
    private val subsonicRepository: SubsonicRepository = App.get(SubsonicRepository::class.java)

    fun getGenres(random: Boolean, size: Int): MutableLiveData<List<Genre>> {
        val genres = MutableLiveData<List<Genre>>()

        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getGenres()
            val genreList = response?.genres?.genres?.toMutableList() ?: mutableListOf()

            if (genreList.isEmpty()) {
                genres.postValue(emptyList())
                return@launch
            }

            if (random) {
                genreList.shuffle()
            }

            val result = if (size != -1) {
                genreList.take(size)
            } else {
                genreList.sortedBy { it.genre }
            }
            genres.postValue(result)
        }

        return genres
    }
}
