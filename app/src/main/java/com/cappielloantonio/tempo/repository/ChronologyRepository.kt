package com.cappielloantonio.tempo.repository
import androidx.lifecycle.LiveData
import com.cappielloantonio.tempo.database.AppDatabase
import com.cappielloantonio.tempo.model.Chronology
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
class ChronologyRepository {
    private val chronologyDao = AppDatabase.getInstance().chronologyDao()
    fun getChronology(server: String, start: Long, end: Long): LiveData<List<Chronology>> {
        return chronologyDao.getAllFrom(start, end, server)
    }
    fun insert(item: Chronology) {
        CoroutineScope(Dispatchers.IO).launch {
            chronologyDao.insert(item)
        }
    }
}
