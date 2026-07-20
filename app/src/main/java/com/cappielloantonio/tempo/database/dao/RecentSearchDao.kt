package com.cappielloantonio.tempo.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cappielloantonio.tempo.model.RecentSearch

@Dao
interface RecentSearchDao {
    @Query("SELECT search FROM recent_search ORDER BY timestamp DESC")
    fun getRecent(): List<String>

    @Query("SELECT search FROM recent_search ORDER BY search DESC")
    fun getAlpha(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(search: RecentSearch)

    @Delete
    fun delete(search: RecentSearch)
}
