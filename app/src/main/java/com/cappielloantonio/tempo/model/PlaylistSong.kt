package com.cappielloantonio.tempo.model

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Playlist

@Keep
@Entity(
    tableName = "playlist_song",
    primaryKeys = ["playlist_id", "id"],
    foreignKeys = [ForeignKey(
        entity = Playlist::class,
        parentColumns = ["id"],
        childColumns = ["playlist_id"]
    )],
    indices = [Index("playlist_id")]
)
data class PlaylistSong(
    @ColumnInfo(name = "playlist_id")
    var playlistId: String,
    @ColumnInfo(name = "id")
    var id: String,
    @ColumnInfo(name = "title")
    var title: String? = null,
    @ColumnInfo(name = "artist")
    var artist: String? = null,
    @ColumnInfo(name = "album")
    var album: String? = null,
    @ColumnInfo(name = "track")
    var track: Int? = null,
    @ColumnInfo(name = "cover_art_id")
    var coverArtId: String? = null,
    @ColumnInfo(name = "duration")
    var duration: Int? = null,
    @ColumnInfo(name = "album_id")
    var albumId: String? = null,
    @ColumnInfo(name = "artist_id")
    var artistId: String? = null
) {
    constructor(playlistId: String, child: Child) : this(
        playlistId = playlistId,
        id = child.id,
        title = child.title,
        artist = child.artist,
        album = child.album,
        track = child.track,
        coverArtId = child.coverArtId,
        duration = child.duration,
        albumId = child.albumId,
        artistId = child.artistId
    )
}
