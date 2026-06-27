package com.cappielloantonio.tempo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cappielloantonio.tempo.model.InternetRadioStationCache

@Dao
interface InternetRadioStationDao {
    @Query("SELECT * FROM internet_radio_station_cache")
    fun getAll(): List<InternetRadioStationCache>

    @Query("SELECT * FROM internet_radio_station_cache WHERE source = 'local'")
    fun getLocal(): List<InternetRadioStationCache>

    @Query("SELECT * FROM internet_radio_station_cache WHERE id = :id LIMIT 1")
    fun getById(id: String): InternetRadioStationCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(stations: List<InternetRadioStationCache>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(station: InternetRadioStationCache)

    @Update
    fun update(station: InternetRadioStationCache)

    @Query("DELETE FROM internet_radio_station_cache WHERE id = :id")
    fun deleteById(id: String)

    @Query("DELETE FROM internet_radio_station_cache WHERE source = 'subsonic'")
    fun deleteSubsonic()

    @Query("DELETE FROM internet_radio_station_cache")
    fun deleteAll()
}
