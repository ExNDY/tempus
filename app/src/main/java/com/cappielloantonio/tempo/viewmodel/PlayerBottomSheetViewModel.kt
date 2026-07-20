package com.cappielloantonio.tempo.viewmodel
import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.cappielloantonio.tempo.interfaces.StarCallback
import com.cappielloantonio.tempo.model.LyricsCache
import com.cappielloantonio.tempo.model.Queue
import com.cappielloantonio.tempo.repository.*
import com.cappielloantonio.tempo.subsonic.models.*
import com.cappielloantonio.tempo.util.*
import com.google.gson.Gson
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
data class PlayerUiState(
    val currentSong: Child? = null,
    val currentAlbum: AlbumID3? = null,
    val currentArtist: ArtistID3? = null,
    val lyrics: String? = null,
    val lyricsList: LyricsList? = null,
    val isLyricsCached: Boolean = false,
    val description: String? = null,
    val isLyricsSynced: Boolean = true,
    val instantMix: List<Child> = emptyList(),
    val queue: List<Queue> = emptyList()
)
class PlayerBottomSheetViewModel(
    private val songRepository: SongRepository,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
    private val queueRepository: QueueRepository,
    private val favoriteRepository: FavoriteRepository,
    private val openRepository: OpenRepository,
    private val lyricsRepository: LyricsRepository,
) : ViewModel() {
    sealed interface Action {
        data class RequestDownload(val media: Child) : Action
    }
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()
    private val _actions = Channel<Action>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()
    private val gson = Gson()
    private var currentSongId: String? = null
    init {
        viewModelScope.launch {
            queueRepository.getLiveQueue().asFlow().collect { queue ->
                _uiState.update { it.copy(queue = queue) }
            }
        }
    }
    fun setFavorite(media: Child?) {
        if (media == null) return
        if (media.starred != null) {
            if (NetworkUtil.isOffline()) {
                removeFavoriteOffline(media)
            } else {
                removeFavoriteOnline(media)
            }
        } else {
            if (NetworkUtil.isOffline()) {
                setFavoriteOffline(media)
            } else {
                setFavoriteOnline(media)
            }
        }
    }
    private fun removeFavoriteOffline(media: Child) {
        favoriteRepository.starLater(media.id, null, null, false)
        media.starred = null
        _uiState.update { it.copy(currentSong = if (it.currentSong?.id == media.id) media else it.currentSong) }
    }
    private fun removeFavoriteOnline(media: Child) {
        favoriteRepository.unstar(media.id, null, null, object : StarCallback {
            override fun onSuccess() {}
            override fun onError() {
                favoriteRepository.starLater(media.id, null, null, false)
            }
        })
        media.starred = null
        _uiState.update { it.copy(currentSong = if (it.currentSong?.id == media.id) media else it.currentSong) }
    }
    private fun setFavoriteOffline(media: Child) {
        favoriteRepository.starLater(media.id, null, null, true)
        media.starred = Date()
        _uiState.update { it.copy(currentSong = if (it.currentSong?.id == media.id) media else it.currentSong) }
    }
    private fun setFavoriteOnline(media: Child) {
        favoriteRepository.star(media.id, null, null, object : StarCallback {
            override fun onSuccess() {}
            override fun onError() {
                favoriteRepository.starLater(media.id, null, null, true)
            }
        })
        media.starred = Date()
        _uiState.update { it.copy(currentSong = if (it.currentSong?.id == media.id) media else it.currentSong) }
        if (Preferences.isStarredSyncEnabled() && Preferences.getDownloadDirectoryUri() == null) {
            viewModelScope.launch { _actions.send(Action.RequestDownload(media)) }
        }
    }
    fun refreshMediaInfo(media: Child?) {
        val songId = media?.id ?: currentSongId
        if (TextUtils.isEmpty(songId)) return
        val resolvedSongId = songId ?: return
        currentSongId = resolvedSongId
        
        _uiState.update { it.copy(
            lyrics = null,
            lyricsList = null,
            isLyricsCached = false
        ) }
        val cachedLyrics = lyricsRepository.getLyrics(resolvedSongId)
        if (cachedLyrics != null) {
            onCachedLyricsChanged(cachedLyrics)
        }
        if (NetworkUtil.isOffline() || media == null) return
        if (OpenSubsonicExtensionsUtil.isSongLyricsExtensionAvailable()) {
            viewModelScope.launch {
                openRepository.getLyricsBySongId(media.id).asFlow().collect { lyricsList: LyricsList? ->
                    _uiState.update { it.copy(lyricsList = lyricsList, lyrics = null) }
                    if (shouldAutoDownloadLyrics() && hasStructuredLyrics(lyricsList)) {
                        saveLyricsToCache(media, null, lyricsList)
                    }
                }
            }
        } else {
            viewModelScope.launch {
                songRepository.getSongLyrics(media).asFlow().collect { lyrics: String? ->
                    _uiState.update { it.copy(lyrics = lyrics, lyricsList = null) }
                    if (shouldAutoDownloadLyrics() && !TextUtils.isEmpty(lyrics)) {
                        saveLyricsToCache(media, lyrics, null)
                    }
                }
            }
        }
    }
    fun setLiveMedia(mediaType: String?, mediaId: String?, placeholder: Child? = null) {
        currentSongId = mediaId
        if (TextUtils.isEmpty(mediaId)) {
            _uiState.update { it.copy(
                lyrics = null,
                lyricsList = null,
                isLyricsCached = false,
                currentSong = null
            ) }
            return
        }
        // Set placeholder immediately to avoid empty UI
        if (placeholder != null) {
            _uiState.update { it.copy(currentSong = placeholder) }
        }
        refreshMediaInfo(placeholder)
        if (mediaType == Constants.MEDIA_TYPE_MUSIC) {
            viewModelScope.launch {
                songRepository.getSong(mediaId ?: "").asFlow().collect { song: Child? ->
                    if (song != null) {
                        _uiState.update { it.copy(currentSong = song) }
                        refreshMediaInfo(song)
                    }
                }
            }
            _uiState.update { it.copy(description = null) }
        } else {
            if (placeholder == null) {
                _uiState.update { it.copy(currentSong = null) }
            }
        }
    }
    fun setLiveAlbum(mediaType: String?, albumId: String?) {
        if (mediaType != null) {
            when (mediaType) {
                Constants.MEDIA_TYPE_MUSIC -> viewModelScope.launch {
                    albumRepository.getAlbum(albumId ?: "").asFlow().collect { album: AlbumID3? ->
                        _uiState.update { state -> state.copy(currentAlbum = album) }
                    }
                }
                else -> _uiState.update { it.copy(currentAlbum = null) }
            }
        }
    }
    fun setLiveArtist(mediaType: String?, artistId: String?) {
        if (mediaType != null) {
            when (mediaType) {
                Constants.MEDIA_TYPE_MUSIC -> viewModelScope.launch {
                    artistRepository.getArtist(artistId ?: "").asFlow().collect { artist: ArtistID3? ->
                        _uiState.update { state -> state.copy(currentArtist = artist) }
                    }
                }
                else -> _uiState.update { it.copy(currentArtist = null) }
            }
        }
    }
    fun setLiveDescription(description: String?) {
        _uiState.update { it.copy(description = description) }
    }
    fun clearLiveMedia() {
        currentSongId = null
        _uiState.update {
            it.copy(
                currentSong = null,
                currentAlbum = null,
                currentArtist = null,
                lyrics = null,
                lyricsList = null,
                isLyricsCached = false,
                description = null
            )
        }
    }
    fun getMediaInstantMix(media: Child): Flow<List<Child>> {
        return songRepository.getInstantMix(media.id, Constants.SeedType.TRACK, 20).asFlow()
    }
    fun getPlayQueue(): LiveData<PlayQueue?> = queueRepository.getPlayQueue()
    fun savePlayQueue(): Boolean {
        val media = _uiState.value.currentSong
        val queue = queueRepository.getMedia()
        val ids = queue.map { it.id }
        return if (media != null) {
            Log.d(TAG, "Saving play queue - Current: ${media.id}, Items: ${ids.size}")
            queueRepository.savePlayQueue(ids, media.id, 0)
            true
        } else {
            false
        }
    }
    private fun onCachedLyricsChanged(lyricsCache: LyricsCache?) {
        if (lyricsCache == null) {
            _uiState.update { it.copy(isLyricsCached = false) }
            return
        }
        _uiState.update { it.copy(isLyricsCached = true) }
        if (!TextUtils.isEmpty(lyricsCache.structuredLyrics)) {
            try {
                val cachedList = gson.fromJson(lyricsCache.structuredLyrics, LyricsList::class.java)
                _uiState.update { it.copy(lyricsList = cachedList, lyrics = null) }
            } catch (_: Exception) {
                _uiState.update { it.copy(lyricsList = null, lyrics = lyricsCache.lyrics) }
            }
        } else {
            _uiState.update { it.copy(lyricsList = null, lyrics = lyricsCache.lyrics) }
        }
    }
    private fun saveLyricsToCache(media: Child?, lyrics: String?, lyricsList: LyricsList?) {
        if (media == null) return
        if ((lyricsList == null || !hasStructuredLyrics(lyricsList)) && TextUtils.isEmpty(lyrics)) return
        val lyricsCache = LyricsCache(media.id)
        lyricsCache.artist = media.artist
        lyricsCache.title = media.title
        lyricsCache.updatedAt = System.currentTimeMillis()
        if (lyricsList != null && hasStructuredLyrics(lyricsList)) {
            lyricsCache.structuredLyrics = gson.toJson(lyricsList)
            lyricsCache.lyrics = null
        } else {
            lyricsCache.lyrics = lyrics
            lyricsCache.structuredLyrics = null
        }
        lyricsRepository.insert(lyricsCache)
        _uiState.update { it.copy(isLyricsCached = true) }
    }
    private fun hasStructuredLyrics(lyricsList: LyricsList?): Boolean {
        return lyricsList?.structuredLyrics?.firstOrNull()?.line?.isNotEmpty() == true
    }
    private fun shouldAutoDownloadLyrics(): Boolean = Preferences.isAutoDownloadLyricsEnabled()
    fun downloadCurrentLyrics(): Boolean {
        val media = _uiState.value.currentSong ?: return false
        val lyricsList = _uiState.value.lyricsList
        val lyrics = _uiState.value.lyrics
        if ((lyricsList == null || !hasStructuredLyrics(lyricsList)) && TextUtils.isEmpty(lyrics)) {
            return false
        }
        saveLyricsToCache(media, lyrics, lyricsList)
        return true
    }
    fun changeSyncLyricsState() {
        _uiState.update { it.copy(isLyricsSynced = !it.isLyricsSynced) }
    }
    companion object {
        private const val TAG = "PlayerBottomSheetViewModel"
    }
}
