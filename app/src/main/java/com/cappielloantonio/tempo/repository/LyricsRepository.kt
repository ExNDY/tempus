package com.cappielloantonio.tempo.repository

import androidx.lifecycle.LiveData
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.database.AppDatabase
import com.cappielloantonio.tempo.model.LyricsCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@UnstableApi
class LyricsRepository {
    private val lyricsDao = AppDatabase.getInstance().lyricsDao()

    fun getLyrics(songId: String): LyricsCache? {
        return runBlocking(Dispatchers.IO) {
            lyricsDao.getOne(songId)
        }
    }

    fun observeLyrics(songId: String): LiveData<LyricsCache?> {
        return lyricsDao.observeOne(songId) as LiveData<LyricsCache?>
    }

    fun insert(lyricsCache: LyricsCache) {
        CoroutineScope(Dispatchers.IO).launch {
            lyricsDao.insert(lyricsCache)
        }
    }

    fun delete(songId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            lyricsDao.delete(songId)
        }
    }
}
