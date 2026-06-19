package com.cappielloantonio.tempo.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.database.AppDatabase
import com.cappielloantonio.tempo.model.Queue
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepository
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.PlayQueue
import kotlinx.coroutines.*
import java.util.concurrent.Executors

@UnstableApi
class QueueRepository {
    private val queueDao = AppDatabase.getInstance().queueDao()
    private val subsonicRepository: SubsonicRepository = App.get(SubsonicRepository::class.java)

    companion object {
        private const val TAG = "QueueRepository"
        private val dbExecutor = Executors.newSingleThreadExecutor()
    }

    fun getLiveQueue(): LiveData<List<Queue>> = queueDao.getAll()

    fun getMedia(): List<Child> {
        return runBlocking(Dispatchers.IO) {
            queueDao.getAllSimple().map { it as Child }
        }
    }

    fun getPlayQueue(): MutableLiveData<PlayQueue?> {
        val playQueue = MutableLiveData<PlayQueue?>()
        Log.d(TAG, "Getting play queue from server...")
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getPlayQueue()
            if (response?.playQueue != null) {
                Log.d(TAG, "Server returned play queue")
                playQueue.postValue(response.playQueue)
            } else {
                Log.d(TAG, "Server returned no play queue")
                playQueue.postValue(null)
            }
        }
        return playQueue
    }

    fun savePlayQueue(ids: List<String>, current: String?, position: Long) {
        Log.d(TAG, "Saving play queue to server...")
        CoroutineScope(Dispatchers.IO).launch {
            subsonicRepository.savePlayQueue(ids, current, position)
        }
    }

    fun insert(media: Child, reset: Boolean, afterIndex: Int) {
        dbExecutor.execute {
            var mediaList = if (reset) mutableListOf() else queueDao.getAllSimple().toMutableList()
            mediaList.add(afterIndex, Queue(media))
            mediaList.forEachIndexed { i, item -> item.trackOrder = i }
            queueDao.replaceQueue(mediaList)
        }
    }

    private fun isMediaInQueue(queue: List<Queue>?, media: Child?): Boolean {
        if (queue == null || media == null) return false
        return queue.any { it.id == media.id }
    }

    fun insertAll(toAdd: List<Child>, reset: Boolean, afterIndex: Int) {
        dbExecutor.execute {
            val mediaList = if (reset) mutableListOf() else queueDao.getAllSimple().toMutableList()
            val filteredToAdd = toAdd.filter { !isMediaInQueue(mediaList, it) }
            filteredToAdd.forEachIndexed { i, child ->
                mediaList.add(afterIndex + i, Queue(child))
            }
            mediaList.forEachIndexed { i, item -> item.trackOrder = i }
            queueDao.replaceQueue(mediaList)
        }
    }

    fun delete(position: Int) {
        dbExecutor.execute { queueDao.delete(position) }
    }

    fun deleteAll() {
        dbExecutor.execute { queueDao.deleteAll() }
    }

    fun count(): Int {
        return runBlocking(Dispatchers.IO) {
            queueDao.count()
        }
    }

    fun setLastPlayedTimestamp(id: String) {
        dbExecutor.execute { queueDao.setLastPlay(id, System.currentTimeMillis()) }
    }

    fun setPlayingPausedTimestamp(id: String, ms: Long) {
        dbExecutor.execute { queueDao.setPlayingChanged(id, ms) }
    }

    fun getLastPlayedMediaIndex(): Int {
        return runBlocking(Dispatchers.IO) {
            queueDao.getLastPlayed()?.trackOrder ?: 0
        }
    }

    fun getLastPlayedMediaTimestamp(): Long {
        return runBlocking(Dispatchers.IO) {
            queueDao.getLastPlayed()?.playingChanged ?: 0
        }
    }

    fun deleteRange(fromIndex: Int, toIndex: Int) {
        dbExecutor.execute {
            val mediaList = queueDao.getAllSimple().toMutableList()
            if (fromIndex < 0 || toIndex > mediaList.size || fromIndex >= toIndex) return@execute
            mediaList.subList(fromIndex, toIndex).clear()
            mediaList.forEachIndexed { i, item -> item.trackOrder = i }
            queueDao.replaceQueue(mediaList)
        }
    }
}
