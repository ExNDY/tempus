package com.cappielloantonio.tempo.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cappielloantonio.tempo.subsonic.models.Playlist

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlist")
    fun getAll(): LiveData<List<Playlist>>

    @Query("SELECT * FROM playlist")
    fun getAllSync(): List<Playlist>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(playlist: Playlist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(playlists: List<Playlist>)

    @Delete
    fun delete(playlist: Playlist)

    @Query("DELETE FROM playlist WHERE id = :playlistId")
    fun deleteById(playlistId: String)

    @Query("DELETE FROM playlist")
    fun deleteAll()

    @Query("SELECT coverArt FROM playlist WHERE id = :playlistId")
    fun getPlaylistCoverArtId(playlistId: String): String?

    @Query("UPDATE playlist SET name = :newName WHERE id = :playlistId")
    fun updateName(playlistId: String, newName: String)

    /**
     * Full list query used by the playlist catalogue route.
     */
    @Query("""
        SELECT p.*, (pp.playlistId IS NOT NULL) AS isPinned 
        FROM playlist p 
        LEFT JOIN pinned_playlist pp ON p.id = pp.playlistId 
        ORDER BY 
        CASE WHEN :sortMethod = 'ORDER_BY_RANDOM' THEN RANDOM() END ASC, 
        CASE WHEN :sortMethod = 'ORDER_BY_PINNED' THEN pp.playlistId IS NOT NULL END DESC, 
        CASE WHEN :sortMethod = 'ORDER_BY_NAME' THEN p.name END ASC, 
        CASE WHEN :sortMethod = 'ORDER_BY_DATE' THEN p.created END DESC, 
        CASE WHEN :sortMethod = 'ORDER_BY_SONGS' THEN p.songCount END DESC
    """)
    fun getSortedPlaylists(sortMethod: String): LiveData<List<Playlist>>

    /**
     * Preview query used by HomeViewModel.
     * Includes a LIMIT clause to only return a subset (e.g., 5 items).
     */
    @Query("""
        SELECT p.*, (pp.playlistId IS NOT NULL) AS isPinned 
        FROM playlist p 
        LEFT JOIN pinned_playlist pp ON p.id = pp.playlistId 
        ORDER BY 
        CASE WHEN :sortMethod = 'ORDER_BY_RANDOM' THEN RANDOM() END ASC, 
        CASE WHEN :sortMethod = 'ORDER_BY_PINNED' THEN pp.playlistId IS NOT NULL END DESC, 
        CASE WHEN :sortMethod = 'ORDER_BY_NAME' THEN p.name END ASC, 
        CASE WHEN :sortMethod = 'ORDER_BY_DATE' THEN p.created END DESC, 
        CASE WHEN :sortMethod = 'ORDER_BY_SONGS' THEN p.songCount END DESC 
        LIMIT :limit
    """)
    fun getSortedPlaylistsPreview(sortMethod: String, limit: Int): LiveData<List<Playlist>>
}
