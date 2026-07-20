package com.cappielloantonio.tempo.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cappielloantonio.tempo.model.Queue

data class QueueRestoreRows(
    val queue: List<Queue>,
    val lastPlayed: Queue?,
)

@Dao
interface QueueDao {
    @Query("SELECT * FROM queue ORDER BY track_order ASC")
    fun getAll(): LiveData<List<Queue>>

    @Query("SELECT * FROM queue ORDER BY track_order ASC")
    fun getAllSimple(): List<Queue>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(songQueueObject: Queue)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(songQueueObjects: List<Queue>)

    @Query("DELETE FROM queue WHERE queue.track_order=:position")
    fun delete(position: Int)

    @Query("DELETE FROM queue")
    fun deleteAll()

    @Query("SELECT COUNT(*) FROM queue")
    fun count(): Int

    @Query("UPDATE queue SET last_play=:timestamp WHERE id=:id")
    fun setLastPlay(id: String, timestamp: Long)

    @Query("UPDATE queue SET playing_changed=:timestamp WHERE id=:id")
    fun setPlayingChanged(id: String, timestamp: Long)

    @Query("SELECT * FROM queue ORDER BY last_play DESC LIMIT 1")
    fun getLastPlayed(): Queue?

    @Transaction
    fun getRestoreRows(): QueueRestoreRows = QueueRestoreRows(
        queue = getAllSimple(),
        lastPlayed = getLastPlayed(),
    )

    @Transaction
    fun replaceQueue(newQueue: List<Queue>) {
        deleteAll()
        insertAll(newQueue)
    }
}
