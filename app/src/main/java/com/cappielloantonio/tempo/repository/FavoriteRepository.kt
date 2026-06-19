package com.cappielloantonio.tempo.repository

import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.database.AppDatabase
import com.cappielloantonio.tempo.interfaces.StarCallback
import com.cappielloantonio.tempo.model.Favorite
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepository
import kotlinx.coroutines.*

@UnstableApi
class FavoriteRepository {
    private val favoriteDao = AppDatabase.getInstance().favoriteDao()
    private val subsonicRepository: SubsonicRepository = App.get(SubsonicRepository::class.java)

    fun star(id: String?, albumId: String?, artistId: String?, starCallback: StarCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.star(id, albumId, artistId)
            withContext(Dispatchers.Main) {
                if (response != null && response.error == null) {
                    starCallback.onSuccess()
                } else {
                    starCallback.onError()
                }
            }
        }
    }

    fun unstar(id: String?, albumId: String?, artistId: String?, starCallback: StarCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.unstar(id, albumId, artistId)
            withContext(Dispatchers.Main) {
                if (response != null && response.error == null) {
                    starCallback.onSuccess()
                } else {
                    starCallback.onError()
                }
            }
        }
    }

    fun getFavorites(): List<Favorite> {
        return runBlocking(Dispatchers.IO) {
            favoriteDao.all
        }
    }

    fun starLater(id: String?, albumId: String?, artistId: String?, toStar: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            favoriteDao.insert(Favorite(System.currentTimeMillis(), id, albumId, artistId, toStar))
        }
    }

    fun delete(favorite: Favorite) {
        CoroutineScope(Dispatchers.IO).launch {
            favoriteDao.delete(favorite)
        }
    }
}
