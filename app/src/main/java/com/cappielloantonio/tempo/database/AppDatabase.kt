package com.cappielloantonio.tempo.database
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.database.converter.DateConverters
import com.cappielloantonio.tempo.database.converter.StringListConverter
import com.cappielloantonio.tempo.database.dao.*
import com.cappielloantonio.tempo.model.*
import com.cappielloantonio.tempo.subsonic.models.Playlist
@Database(
    version = 21,
    entities = [
        Queue::class,
        Server::class,
        RecentSearch::class,
        Download::class,
        Chronology::class,
        Favorite::class,
        SessionMediaItem::class,
        Playlist::class,
        PinnedPlaylist::class,
        LyricsCache::class,
        PlaylistSong::class
    ],
    autoMigrations = [
        AutoMigration(from = 10, to = 11),
        AutoMigration(from = 11, to = 12),
        AutoMigration(from = 12, to = 13),
        AutoMigration(from = 13, to = 14),
        AutoMigration(from = 14, to = 15),
        AutoMigration(from = 15, to = 16),
        AutoMigration(from = 16, to = 17),
        AutoMigration(from = 17, to = 18),
        AutoMigration(from = 18, to = 19),
        AutoMigration(from = 19, to = 20)
    ]
)
@TypeConverters(DateConverters::class, StringListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun queueDao(): QueueDao
    abstract fun serverDao(): ServerDao
    abstract fun recentSearchDao(): RecentSearchDao
    abstract fun downloadDao(): DownloadDao
    abstract fun chronologyDao(): ChronologyDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun sessionMediaItemDao(): SessionMediaItemDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun pinnedPlaylistDao(): PinnedPlaylistDao
    abstract fun playlistSongDao(): PlaylistSongDao
    abstract fun lyricsDao(): LyricsDao
    companion object {
        private const val DB_NAME = "tempo_db"
        @Volatile
        private var instance: AppDatabase? = null
        fun getInstance(): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    App.getContext(),
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
