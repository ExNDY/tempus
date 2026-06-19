package com.cappielloantonio.tempo.repository

import androidx.lifecycle.LiveData
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.database.AppDatabase
import com.cappielloantonio.tempo.model.Download
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@UnstableApi
class DownloadRepository {
    private val downloadDao = AppDatabase.getInstance().downloadDao()

    fun getLiveDownload(): LiveData<List<Download>> = downloadDao.all

    fun getAllDownloads(): List<Download> {
        return runBlocking(Dispatchers.IO) {
            downloadDao.allSync
        }
    }

    fun getDownload(id: String): Download? {
        return runBlocking(Dispatchers.IO) {
            downloadDao.getOne(id)
        }
    }

    fun insert(download: Download) {
        CoroutineScope(Dispatchers.IO).launch {
            downloadDao.insert(download)
        }
    }

    fun update(id: String) {
        CoroutineScope(Dispatchers.IO).launch {
            downloadDao.update(id)
        }
    }

    fun insertAll(downloads: List<Download>) {
        CoroutineScope(Dispatchers.IO).launch {
            downloadDao.insertAll(downloads)
        }
    }

    fun deleteAll() {
        CoroutineScope(Dispatchers.IO).launch {
            downloadDao.deleteAll()
        }
    }

    fun delete(id: String) {
        CoroutineScope(Dispatchers.IO).launch {
            downloadDao.delete(id)
        }
    }

    fun delete(ids: List<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            downloadDao.deleteByIds(ids)
        }
    }
}
