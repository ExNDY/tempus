package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cappielloantonio.tempo.interfaces.StarCallback
import com.cappielloantonio.tempo.model.Favorite
import com.cappielloantonio.tempo.model.HomeSector
import com.cappielloantonio.tempo.playback.PlaybackState
import com.cappielloantonio.tempo.playback.PlaybackStateStore
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.ChronologyRepository
import com.cappielloantonio.tempo.repository.FavoriteRepository
import com.cappielloantonio.tempo.repository.PlaylistRepository
import com.cappielloantonio.tempo.repository.SharingRepository
import com.cappielloantonio.tempo.repository.SongRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.subsonic.models.Share
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.Preferences
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeMusicUiState(
    val discoverSongs: List<Child> = emptyList(),
    val similarTracks: List<Child> = emptyList(),
    val bestOfArtists: List<ArtistID3> = emptyList(),
    val topSongs: List<Child> = emptyList(),
    val starredTracks: List<Child> = emptyList(),
    val starredAlbums: List<AlbumID3> = emptyList(),
    val starredArtists: List<ArtistID3> = emptyList(),
    val newReleases: List<AlbumID3> = emptyList(),
    val flashbackYears: List<Int> = emptyList(),
    val mostPlayedAlbums: List<AlbumID3> = emptyList(),
    val recentlyPlayedAlbums: List<AlbumID3> = emptyList(),
    val recentlyAddedAlbums: List<AlbumID3> = emptyList(),
    val pinnedPlaylists: List<Playlist> = emptyList(),
    val shares: List<Share> = emptyList(),
    val sectorConfig: List<HomeSector> = emptyList(),
    val isLoading: Boolean = true,
    val currentSongId: String? = null,
    val isPlaying: Boolean = false,
)

class HomeViewModel(
    private val songRepository: SongRepository,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
    private val chronologyRepository: ChronologyRepository,
    private val favoriteRepository: FavoriteRepository,
    private val playlistRepository: PlaylistRepository,
    private val sharingRepository: SharingRepository,
    private val playbackStateStore: PlaybackStateStore,
) : ViewModel() {
    private var started = false
    private val _discoverSongs = MutableStateFlow<List<Child>>(emptyList())
    private val _similarTracks = MutableStateFlow<List<Child>>(emptyList())
    private val _bestOfArtists = MutableStateFlow<List<ArtistID3>>(emptyList())
    private val _topSongs = MutableStateFlow<List<Child>>(emptyList())
    private val _starredTracks = MutableStateFlow<List<Child>>(emptyList())
    private val _starredAlbums = MutableStateFlow<List<AlbumID3>>(emptyList())
    private val _starredArtists = MutableStateFlow<List<ArtistID3>>(emptyList())
    private val _newReleases = MutableStateFlow<List<AlbumID3>>(emptyList())
    private val _flashbackYears = MutableStateFlow<List<Int>>(emptyList())
    private val _mostPlayedAlbums = MutableStateFlow<List<AlbumID3>>(emptyList())
    private val _recentlyPlayedAlbums = MutableStateFlow<List<AlbumID3>>(emptyList())
    private val _recentlyAddedAlbums = MutableStateFlow<List<AlbumID3>>(emptyList())
    private val _pinnedPlaylists = MutableStateFlow<List<Playlist>>(emptyList())
    private val _shares = MutableStateFlow<List<Share>>(emptyList())
    private val _sectorConfig = MutableStateFlow<List<HomeSector>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    val musicUiState: StateFlow<HomeMusicUiState> = combine(
        _discoverSongs, _similarTracks, _bestOfArtists, _topSongs,
        _starredTracks, _starredAlbums, _starredArtists, _newReleases, _flashbackYears,
        _mostPlayedAlbums, _recentlyPlayedAlbums, _recentlyAddedAlbums, _pinnedPlaylists,
        _shares, _sectorConfig, _isLoading, playbackStateStore.state
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        HomeMusicUiState(
            discoverSongs = args[0] as List<Child>,
            similarTracks = args[1] as List<Child>,
            bestOfArtists = args[2] as List<ArtistID3>,
            topSongs = args[3] as List<Child>,
            starredTracks = args[4] as List<Child>,
            starredAlbums = args[5] as List<AlbumID3>,
            starredArtists = args[6] as List<ArtistID3>,
            newReleases = args[7] as List<AlbumID3>,
            flashbackYears = args[8] as List<Int>,
            mostPlayedAlbums = args[9] as List<AlbumID3>,
            recentlyPlayedAlbums = args[10] as List<AlbumID3>,
            recentlyAddedAlbums = args[11] as List<AlbumID3>,
            pinnedPlaylists = args[12] as List<Playlist>,
            shares = args[13] as List<Share>,
            sectorConfig = args[14] as List<HomeSector>,
            isLoading = args[15] as Boolean,
            currentSongId = (args[16] as PlaybackState).currentSongId,
            isPlaying = (args[16] as PlaybackState).isPlaying,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeMusicUiState()
    )

    fun onStart() {
        if (started) return
        started = true
        loadSectorConfig()
        loadInitialData()
        setOfflineFavorite()
    }

    fun refreshHomeLayout() {
        loadSectorConfig()
        loadInitialData()
    }

    private fun loadSectorConfig() {
        val stored = Preferences.getHomeSectorList()
        _sectorConfig.value = if (!stored.isNullOrEmpty() && stored != "null") {
            Gson().fromJson<List<HomeSector>?>(
                stored,
                object : TypeToken<List<HomeSector>>() {}.type,
            )?.sortedBy { it.order } ?: defaultHomeSectorList()
        } else {
            defaultHomeSectorList()
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            _sectorConfig.value.filter { it.isVisible }.forEach { sector ->
                refreshSector(sector.id)
            }
            _isLoading.value = false
        }
    }

    fun refreshSector(sectorId: String) {
        viewModelScope.launch {
            when (sectorId) {
                Constants.HOME_SECTOR_DISCOVERY -> {
                    songRepository.getRandomSample(10, null, null)
                        .observeForever { _discoverSongs.value = it ?: emptyList() }
                }
                Constants.HOME_SECTOR_MADE_FOR_YOU -> {
                    songRepository.getStarredSongs(true, 10)
                        .observeForever { _similarTracks.value = it ?: emptyList() }
                }
                Constants.HOME_SECTOR_BEST_OF -> {
                    artistRepository.getStarredArtists(true, 20)
                        .observeForever { _bestOfArtists.value = it ?: emptyList() }
                }
                Constants.HOME_SECTOR_TOP_SONGS -> {
                    loadTopSongs()
                }
                Constants.HOME_SECTOR_STARRED_TRACKS -> {
                    songRepository.getStarredSongs(true, 20)
                        .observeForever { _starredTracks.value = it ?: emptyList() }
                }
                Constants.HOME_SECTOR_STARRED_ALBUMS -> {
                    albumRepository.getStarredAlbums(true, 20)
                        .observeForever { _starredAlbums.value = it ?: emptyList() }
                }
                Constants.HOME_SECTOR_STARRED_ARTISTS -> {
                    artistRepository.getStarredArtists(true, 20)
                        .observeForever { _starredArtists.value = it ?: emptyList() }
                }
                Constants.HOME_SECTOR_NEW_RELEASES -> {
                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                    albumRepository.getAlbums("byYear", 500, currentYear, currentYear)
                        .observeForever { list ->
                            _newReleases.value =
                                list?.sortedByDescending { it.created }?.take(20) ?: emptyList()
                        }
                }
                Constants.HOME_SECTOR_FLASHBACK -> {
                    albumRepository.getDecades()
                        .observeForever { _flashbackYears.value = it ?: emptyList() }
                }
                Constants.HOME_SECTOR_MOST_PLAYED -> {
                    albumRepository.getAlbums("frequent", 20, null, null)
                        .observeForever { _mostPlayedAlbums.value = it ?: emptyList() }
                }
                Constants.HOME_SECTOR_LAST_PLAYED -> {
                    albumRepository.getAlbums("recent", 20, null, null)
                        .observeForever { _recentlyPlayedAlbums.value = it ?: emptyList() }
                }
                Constants.HOME_SECTOR_RECENTLY_ADDED -> {
                    albumRepository.getAlbums("newest", 20, null, null)
                        .observeForever { _recentlyAddedAlbums.value = it ?: emptyList() }
                }
                Constants.HOME_SECTOR_PINNED_PLAYLISTS -> {
                    val sortOrder = Preferences.getHomeSortPlaylists()
                    playlistRepository.getSortedPlaylistsPreview(sortOrder, 5)
                        .observeForever { _pinnedPlaylists.value = it ?: emptyList() }
                }
                Constants.HOME_SECTOR_SHARED -> {
                    if (Preferences.isSharingEnabled()) {
                        sharingRepository.getShares()
                            .observeForever { _shares.value = it ?: emptyList() }
                    }
                }
            }
        }
    }

    private fun loadTopSongs() {
        val cal = Calendar.getInstance()
        val server = Preferences.getServerId()
        val currentWeek = cal.get(Calendar.WEEK_OF_YEAR)
        val start = cal.timeInMillis
        cal.set(Calendar.WEEK_OF_YEAR, currentWeek - 1)
        val end = cal.timeInMillis
        chronologyRepository.getChronology(server ?: "", start, end).observeForever { list ->
            _topSongs.value = list?.filterIsInstance<Child>() ?: emptyList()
        }
    }

    fun changeTopSongsPeriod(period: Int) {
        val cal = Calendar.getInstance()
        val server = Preferences.getServerId()
        val currentWeek = cal.get(Calendar.WEEK_OF_YEAR)
        val start = cal.timeInMillis
        val end = when (period) {
            0 -> {
                cal.set(Calendar.WEEK_OF_YEAR, currentWeek - 1); cal.timeInMillis
            }
            1 -> {
                cal.set(Calendar.WEEK_OF_YEAR, currentWeek - 4); cal.timeInMillis
            }
            2 -> {
                cal.set(Calendar.WEEK_OF_YEAR, currentWeek - 52); cal.timeInMillis
            }
            else -> start
        }
        chronologyRepository.getChronology(server ?: "", start, end).observeForever { list ->
            _topSongs.value = list?.filterIsInstance<Child>() ?: emptyList()
        }
    }

    fun checkHomeSectorVisibility(sectorId: String): Boolean {
        return _sectorConfig.value.firstOrNull { it.id == sectorId }?.isVisible == true
    }

    fun refreshShares(unused: Any? = null) {
        refreshSector(Constants.HOME_SECTOR_SHARED)
    }

    private fun defaultHomeSectorList(): List<HomeSector> = listOf(
        HomeSector(Constants.HOME_SECTOR_DISCOVERY, "", true, 1),
        HomeSector(Constants.HOME_SECTOR_MADE_FOR_YOU, "", true, 2),
        HomeSector(Constants.HOME_SECTOR_BEST_OF, "", true, 3),
        HomeSector(Constants.HOME_SECTOR_TOP_SONGS, "", true, 4),
        HomeSector(Constants.HOME_SECTOR_STARRED_TRACKS, "", true, 5),
        HomeSector(Constants.HOME_SECTOR_STARRED_ALBUMS, "", true, 6),
        HomeSector(Constants.HOME_SECTOR_STARRED_ARTISTS, "", true, 7),
        HomeSector(Constants.HOME_SECTOR_NEW_RELEASES, "", true, 8),
        HomeSector(Constants.HOME_SECTOR_FLASHBACK, "", true, 9),
        HomeSector(Constants.HOME_SECTOR_MOST_PLAYED, "", true, 10),
        HomeSector(Constants.HOME_SECTOR_LAST_PLAYED, "", true, 11),
        HomeSector(Constants.HOME_SECTOR_RECENTLY_ADDED, "", true, 12),
        HomeSector(Constants.HOME_SECTOR_PINNED_PLAYLISTS, "", true, 13),
        HomeSector(Constants.HOME_SECTOR_SHARED, "", true, 14),
    )

    fun setOfflineFavorite() {
        val favorites = ArrayList(favoriteRepository.getFavorites())
        favorites.forEach { favorite ->
            if (favorite.toStar) favoriteToStar(favorite)
            else favoriteToUnstar(favorite)
        }
    }

    private fun favoriteToStar(favorite: Favorite) {
        when {
            favorite.songId != null -> favoriteRepository.star(
                favorite.songId,
                null,
                null,
                object : StarCallback {
                    override fun onSuccess() {
                        favoriteRepository.delete(favorite)
                    }

                    override fun onError() {}
                })
            favorite.albumId != null -> favoriteRepository.star(
                null,
                favorite.albumId,
                null,
                object : StarCallback {
                    override fun onSuccess() {
                        favoriteRepository.delete(favorite)
                    }

                    override fun onError() {}
                })
            favorite.artistId != null -> favoriteRepository.star(
                null,
                null,
                favorite.artistId,
                object : StarCallback {
                    override fun onSuccess() {
                        favoriteRepository.delete(favorite)
                    }

                    override fun onError() {}
                })
        }
    }

    private fun favoriteToUnstar(favorite: Favorite) {
        when {
            favorite.songId != null -> favoriteRepository.unstar(
                favorite.songId,
                null,
                null,
                object : StarCallback {
                    override fun onSuccess() {
                        favoriteRepository.delete(favorite)
                    }

                    override fun onError() {}
                })
            favorite.albumId != null -> favoriteRepository.unstar(
                null,
                favorite.albumId,
                null,
                object : StarCallback {
                    override fun onSuccess() {
                        favoriteRepository.delete(favorite)
                    }

                    override fun onError() {}
                })
            favorite.artistId != null -> favoriteRepository.unstar(
                null,
                null,
                favorite.artistId,
                object : StarCallback {
                    override fun onSuccess() {
                        favoriteRepository.delete(favorite)
                    }

                    override fun onError() {}
                })
        }
    }
}
