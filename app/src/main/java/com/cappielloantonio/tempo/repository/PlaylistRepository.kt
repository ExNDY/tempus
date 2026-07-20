package com.cappielloantonio.tempo.repository
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.database.AppDatabase
import com.cappielloantonio.tempo.model.PinnedPlaylist
import com.cappielloantonio.tempo.model.PlaylistSong
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepository
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Playlist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
class PlaylistRepository {
    private val pinnedPlaylistDao = AppDatabase.getInstance().pinnedPlaylistDao()
    private val playlistDao = AppDatabase.getInstance().playlistDao()
    private val playlistSongDao = AppDatabase.getInstance().playlistSongDao()
    private val subsonicRepository: SubsonicRepository = App.get(SubsonicRepository::class.java)
    companion object {
        private val playlistUpdateTrigger = MutableLiveData<Boolean>()
        private val allPlaylistsLiveData = MutableLiveData<List<Playlist>>()
    }
    fun getPlaylistUpdateTrigger(): LiveData<Boolean> = playlistUpdateTrigger
    fun notifyPlaylistChanged() {
        playlistUpdateTrigger.postValue(true)
        refreshAllPlaylists()
    }
    private fun handleMissingPlaylist(id: String, onMissing: Runnable?) {
        CoroutineScope(Dispatchers.IO).launch {
            playlistSongDao.deleteForPlaylist(id)
            pinnedPlaylistDao.unpin(id)
            playlistDao.deleteById(id)
            onMissing?.let { Handler(Looper.getMainLooper()).post(it) }
            refreshAllPlaylists()
        }
    }
    fun getAllPlaylists(owner: LifecycleOwner): LiveData<List<Playlist>> {
        refreshAllPlaylists()
        return allPlaylistsLiveData
    }
    fun refreshAllPlaylists() {
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getPlaylists()
            response?.playlists?.playlists?.let {
                allPlaylistsLiveData.postValue(it)
                cacheAllPlaylists(it)
            }
        }
    }
    private fun cacheAllPlaylists(playlists: List<Playlist>) {
        CoroutineScope(Dispatchers.IO).launch {
            val cachedPlaylists = playlistDao.getAllSync()
            val remoteIds = playlists.map { it.id }.toSet()
            cachedPlaylists.forEach { cached ->
                if (!remoteIds.contains(cached.id)) {
                    playlistSongDao.deleteForPlaylist(cached.id)
                    pinnedPlaylistDao.unpin(cached.id)
                    playlistDao.delete(cached)
                    Log.d("PlaylistRepository", "Removed orphaned playlist ${cached.id} from local DB.")
                }
            }
            playlistDao.insertAll(playlists)
            Log.d("PlaylistRepository", "Cached ${playlists.size} playlists to local DB.")
        }
    }
    fun getPlaylists(random: Boolean, size: Int): MutableLiveData<List<Playlist>> {
        val listLivePlaylists = MutableLiveData<List<Playlist>>(ArrayList())
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getPlaylists()
            val playlists = response?.playlists?.playlists?.toMutableList() ?: mutableListOf()
            if (playlists.isNotEmpty()) {
                cacheAllPlaylists(playlists)
                if (random) {
                    playlists.shuffle()
                    listLivePlaylists.postValue(playlists.take(size))
                } else {
                    listLivePlaylists.postValue(playlists)
                }
            }
        }
        return listLivePlaylists
    }
    fun getSortedPlaylists(sortOrder: String): LiveData<List<Playlist>> {
        return playlistDao.getSortedPlaylists(sortOrder)
    }
    fun getSortedPlaylistsPreview(sortOrder: String, limit: Int): LiveData<List<Playlist>> {
        return playlistDao.getSortedPlaylistsPreview(sortOrder, limit)
    }
    fun getPlaylistSongs(id: String): MutableLiveData<List<Child>?> {
        val listLivePlaylistSongs = MutableLiveData<List<Child>?>()
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getPlaylist(id)
            if (response != null) {
                if (response.playlist != null) {
                    val songs = response.playlist?.entries ?: emptyList()
                    listLivePlaylistSongs.postValue(songs)
                    cachePlaylistSongs(id, songs)
                } else if (response.error?.code == 70) {
                    handleMissingPlaylist(id, null)
                    listLivePlaylistSongs.postValue(null)
                } else {
                    listLivePlaylistSongs.postValue(null)
                }
            } else {
                fetchCachedPlaylistSongs(id, listLivePlaylistSongs)
            }
        }
        return listLivePlaylistSongs
    }
    private fun cachePlaylistSongs(playlistId: String, songs: List<Child>) {
        CoroutineScope(Dispatchers.IO).launch {
            if (songs.isEmpty()) {
                playlistSongDao.deleteForPlaylist(playlistId)
                return@launch
            }
            val playlistSongs = songs.map { PlaylistSong(playlistId, it) }
            playlistSongDao.deleteForPlaylist(playlistId)
            playlistSongDao.insertAll(playlistSongs)
        }
    }
    private fun fetchCachedPlaylistSongs(playlistId: String, liveData: MutableLiveData<List<Child>?>) {
        CoroutineScope(Dispatchers.IO).launch {
            val cached = playlistSongDao.getSongsForPlaylistSync(playlistId)
            if (!cached.isNullOrEmpty()) {
                val songs = cached.map { ps ->
                    Child(ps.id).apply {
                        title = ps.title
                        artist = ps.artist
                        album = ps.album
                        track = ps.track
                        coverArtId = ps.coverArtId
                        duration = ps.duration
                        albumId = ps.albumId
                        artistId = ps.artistId
                    }
                }
                liveData.postValue(songs)
            }
        }
    }
    fun getPlaylist(id: String): MutableLiveData<Playlist?> {
        val playlistLiveData = MutableLiveData<Playlist?>()
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getPlaylist(id)
            if (response != null) {
                if (response.playlist != null) {
                    playlistLiveData.postValue(response.playlist)
                } else if (response.error?.code == 70) {
                    handleMissingPlaylist(id, null)
                    playlistLiveData.postValue(null)
                } else {
                    playlistLiveData.postValue(null)
                }
            } else {
                playlistLiveData.postValue(null)
            }
        }
        return playlistLiveData
    }
    interface AddToPlaylistCallback {
        fun onSuccess()
        fun onFailure()
        fun onAllSkipped()
    }
    fun addSongToPlaylist(playlistId: String, songsId: ArrayList<String>, playlistVisibilityIsPublic: Boolean?, callback: AddToPlaylistCallback?) {
        if (songsId.isEmpty()) {
            callback?.onAllSkipped()
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.updatePlaylist(playlistId, null, playlistVisibilityIsPublic, songsId, null)
            if (response != null && response.error == null) {
                notifyPlaylistChanged()
                callback?.onSuccess()
            } else {
                callback?.onFailure()
            }
        }
    }
    fun removeSongFromPlaylist(playlistId: String, index: Int, callback: AddToPlaylistCallback?) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.updatePlaylist(playlistId, null, true, null, listOf(index))
            if (response != null && response.error == null) {
                notifyPlaylistChanged()
                callback?.onSuccess()
            } else {
                callback?.onFailure()
            }
        }
    }
    fun createPlaylist(playlistId: String?, name: String?, songsId: ArrayList<String>, callback: PlaylistActionCallback?) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.createPlaylist(playlistId, name, songsId)
            if (response != null && response.error == null) {
                notifyPlaylistChanged()
                callback?.onSuccess()
            } else {
                callback?.onFailure()
            }
        }
    }
    fun updatePlaylist(playlistId: String, name: String?, songsId: ArrayList<String>, callback: PlaylistActionCallback?) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.updatePlaylist(playlistId, name, true, songsId, null)
            if (response != null && response.error == null) {
                playlistDao.updateName(playlistId, name ?: "")
                notifyPlaylistChanged()
                callback?.onSuccess()
            } else {
                callback?.onFailure()
            }
        }
    }
    fun pin(id: String) {
        CoroutineScope(Dispatchers.IO).launch {
            pinnedPlaylistDao.pin(id)
        }
    }
    fun unpin(id: String) {
        CoroutineScope(Dispatchers.IO).launch {
            pinnedPlaylistDao.unpin(id)
        }
    }
    interface PlaylistActionCallback {
        fun onSuccess()
        fun onFailure()
    }
    fun deletePlaylist(playlistId: String, callback: PlaylistActionCallback?) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.deletePlaylist(playlistId)
            if (response != null && response.error == null) {
                playlistSongDao.deleteForPlaylist(playlistId)
                playlistDao.deleteById(playlistId)
                notifyPlaylistChanged()
                callback?.onSuccess()
            } else {
                callback?.onFailure()
            }
        }
    }
    fun getPinnedPlaylists(): LiveData<List<PinnedPlaylist>> = pinnedPlaylistDao.getAllPinnedIds()
    fun insert(playlist: Playlist) {
        CoroutineScope(Dispatchers.IO).launch {
            playlistDao.insert(playlist)
        }
    }
    fun delete(playlist: Playlist) {
        CoroutineScope(Dispatchers.IO).launch {
            playlistDao.delete(playlist)
        }
    }
    fun updatePinnedPlaylists(forceIds: List<String>? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            val pinned = playlistDao.getAllSync()
            if (!pinned.isNullOrEmpty()) {
                val response = subsonicRepository.getPlaylists()
                val remotes = response?.playlists?.playlists ?: return@launch
                pinned.forEach { p ->
                    remotes.find { it.id == p.id }?.let { r ->
                        p.name = r.name
                        p.songCount = r.songCount
                        p.duration = r.duration
                        p.coverArtId = r.coverArtId
                        playlistDao.insert(p)
                    }
                }
            }
        }
    }
}
private object Log {
    fun d(tag: String, msg: String) {
        android.util.Log.d(tag, msg)
    }
}
