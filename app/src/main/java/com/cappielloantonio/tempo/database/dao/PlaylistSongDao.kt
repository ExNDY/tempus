package com.cappielloantonio.tempo.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cappielloantonio.tempo.model.PlaylistSong

@Dao
interface PlaylistSongDao {
    @Query("SELECT * FROM playlist_song WHERE playlist_id = :playlistId")
    fun getSongsForPlaylist(playlistId: String): LiveData<List<PlaylistSong>>

    @Query("SELECT * FROM playlist_song WHERE playlist_id = :playlistId")
    fun getSongsForPlaylistSync(playlistId: String): List<PlaylistSong>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(playlistSongs: List<PlaylistSong>)

    @Query("DELETE FROM playlist_song WHERE playlist_id = :playlistId")
    fun deleteForPlaylist(playlistId: String)

    @Query("DELETE FROM playlist_song")
    fun deleteAll()
}
