package com.cappielloantonio.tempo.viewmodel
import androidx.lifecycle.asFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cappielloantonio.tempo.interfaces.StarCallback
import com.cappielloantonio.tempo.playback.PlaybackStateStore
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.FavoriteRepository
import com.cappielloantonio.tempo.subsonic.models.*
import com.cappielloantonio.tempo.util.NetworkUtil
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume
data class ArtistPageUiState(
    val artist: ArtistID3? = null,
    val artistInfo: ArtistInfo2? = null,
    val topSongs: List<Child> = emptyList(),
    val albums: Map<String, List<AlbumID3>> = emptyMap(),
    val isLoading: Boolean = true,
    val currentSongId: String? = null,
    val isPlaying: Boolean = false,
)
class ArtistPageViewModel(
    private val artistRepository: ArtistRepository,
    private val albumRepository: AlbumRepository,
    private val favoriteRepository: FavoriteRepository,
    playbackStateStore: PlaybackStateStore,
) : ViewModel() {
    private val _artist = MutableStateFlow<ArtistID3?>(null)
    private val _artistInfo = MutableStateFlow<ArtistInfo2?>(null)
    private val _topSongs = MutableStateFlow<List<Child>>(emptyList())
    private val _albums = MutableStateFlow<Map<String, List<AlbumID3>>>(emptyMap())
    private val _isLoading = MutableStateFlow(true)
    private var cachedArtistSongs: List<Child> = emptyList()
    private var startedArtistId: String? = null
    val uiState: StateFlow<ArtistPageUiState> = combine(
        _artist, _artistInfo, _topSongs, _albums, _isLoading, playbackStateStore.state
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        ArtistPageUiState(
            artist = args[0] as ArtistID3?,
            artistInfo = args[1] as ArtistInfo2?,
            topSongs = args[2] as List<Child>,
            albums = args[3] as Map<String, List<AlbumID3>>,
            isLoading = args[4] as Boolean,
            currentSongId = (args[5] as com.cappielloantonio.tempo.playback.PlaybackState).currentSongId,
            isPlaying = (args[5] as com.cappielloantonio.tempo.playback.PlaybackState).isPlaying,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ArtistPageUiState()
    )
    fun onStart(artist: ArtistID3) {
        if (startedArtistId == artist.id && _artist.value?.id == artist.id) return
        startedArtistId = artist.id
        _artist.value = artist
        loadData(artist)
    }
    fun onStart(artistId: String) {
        if (startedArtistId == artistId && _artist.value?.id == artistId) return
        val initialArtist = ArtistID3(id = artistId)
        startedArtistId = artistId
        _artist.value = initialArtist
        loadData(initialArtist)
    }
    private fun loadData(artist: ArtistID3) {
        viewModelScope.launch {
            _isLoading.value = true
            val artistId = artist.id.orEmpty()
            val artistName = artist.name.orEmpty()
            val artistDeferred = async { artistRepository.getArtistInfo(artistId).asFlow().first() }
            val artistInfoDeferred = async { artistRepository.getArtistFullInfo(artistId).asFlow().first() }
            val topSongsDeferred = async { artistRepository.getTopSongs(artistName, 20).asFlow().first().orEmpty() }
            val albumsDeferred = async { albumRepository.getArtistAlbums(artistId).asFlow().first().orEmpty() }
            val resolvedArtist = artistDeferred.await() ?: artist
            val resolvedArtistInfo = artistInfoDeferred.await()
            val resolvedTopSongs = topSongsDeferred.await()
            val resolvedAlbums = albumsDeferred.await()
            _artist.value = resolvedArtist
            _artistInfo.value = resolvedArtistInfo
            _albums.value = groupAlbums(resolvedAlbums)
            cachedArtistSongs = if (resolvedTopSongs.isNotEmpty()) {
                collectUniqueSongs(resolvedTopSongs)
            } else {
                collectAllArtistSongs(artistId)
            }
            _topSongs.value = when {
                resolvedTopSongs.isNotEmpty() -> resolvedTopSongs
                cachedArtistSongs.isNotEmpty() -> cachedArtistSongs.take(20)
                else -> emptyList()
            }
            _isLoading.value = false
        }
    }
    fun setFavorite() {
        val currentArtist = _artist.value ?: return
        if (currentArtist.starred != null) {
            if (NetworkUtil.isOffline()) {
                removeFavoriteOffline(currentArtist)
            } else {
                removeFavoriteOnline(currentArtist)
            }
        } else {
            if (NetworkUtil.isOffline()) {
                setFavoriteOffline(currentArtist)
            } else {
                setFavoriteOnline(currentArtist)
            }
        }
    }
    private fun removeFavoriteOffline(artist: ArtistID3) {
        favoriteRepository.starLater(null, null, artist.id, false)
        _artist.value = artist.withStarred(null)
    }
    private fun removeFavoriteOnline(artist: ArtistID3) {
        val artistId = artist.id ?: return
        favoriteRepository.unstar(null, null, artistId, object : StarCallback {
            override fun onSuccess() {
                _artist.value = artist.withStarred(null)
            }
            override fun onError() {
                favoriteRepository.starLater(null, null, artistId, false)
            }
        })
    }
    private fun setFavoriteOffline(artist: ArtistID3) {
        favoriteRepository.starLater(null, null, artist.id, true)
        _artist.value = artist.withStarred(Date())
    }
    private fun setFavoriteOnline(artist: ArtistID3) {
        val artistId = artist.id ?: return
        favoriteRepository.star(null, null, artistId, object : StarCallback {
            override fun onSuccess() {
                _artist.value = artist.withStarred(Date())
            }
            override fun onError() {
                favoriteRepository.starLater(null, null, artistId, true)
            }
        })
    }
    fun getArtistInstantMix(): Flow<List<Child>> {
        val artist = _artist.value ?: return flowOf(emptyList())
        return artistRepository.getInstantMix(artist, 30).asFlow()
    }
    fun getShuffledArtistSongs(): List<Child> {
        return cachedArtistSongs.shuffled()
    }
    private suspend fun collectAllArtistSongs(artistId: String): List<Child> {
        if (artistId.isBlank()) return emptyList()
        return suspendCancellableCoroutine { continuation ->
            artistRepository.getArtistAllSongs(artistId) { songs ->
                if (continuation.isActive) {
                    continuation.resume(collectUniqueSongs(songs))
                }
            }
        }
    }
    private fun collectUniqueSongs(songs: List<Child>): List<Child> {
        return songs
            .filter { !it.isDir && !it.isVideo }
            .distinctBy { it.id }
    }
    private fun groupAlbums(albums: List<AlbumID3>): Map<String, List<AlbumID3>> {
        val grouped = albums.groupBy { it.releaseTypes?.firstOrNull() ?: "album" }
        val orderedKeys = listOf(
            "album",
            "ep",
            "single",
            "compilation",
            "soundtrack",
            "live",
            "remix",
            "appears_on",
        )
        return buildMap {
            orderedKeys.forEach { key ->
                grouped[key]
                    ?.sortedWith(compareBy<AlbumID3> { it.year }.thenBy { it.sortName ?: it.name.orEmpty() })
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { put(key, it) }
            }
            grouped
                .filterKeys { it !in orderedKeys }
                .toSortedMap()
                .forEach { (key, value) ->
                    put(key, value.sortedWith(compareBy<AlbumID3> { it.year }.thenBy { it.sortName ?: it.name.orEmpty() }))
                }
        }
    }
    private fun ArtistID3.withStarred(starred: Date?): ArtistID3 {
        return ArtistID3(
            id = id,
            name = name,
            coverArtId = coverArtId,
            albumCount = albumCount,
            starred = starred,
        )
    }
}
