package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.cappielloantonio.tempo.interfaces.StarCallback
import com.cappielloantonio.tempo.model.Chronology
import com.cappielloantonio.tempo.model.Favorite
import com.cappielloantonio.tempo.model.HomeSector
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
import com.cappielloantonio.tempo.util.Constants.SeedType
import com.cappielloantonio.tempo.util.Preferences
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.util.ArrayList
import java.util.Calendar
import java.util.Collections
import java.util.Comparator
import java.util.HashMap

class HomeViewModel(
    private val songRepository: SongRepository,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
    private val chronologyRepository: ChronologyRepository,
    private val favoriteRepository: FavoriteRepository,
    private val playlistRepository: PlaylistRepository,
    private val sharingRepository: SharingRepository,
) : ViewModel() {

    private val albumsSyncViewModel = StarredAlbumsSyncViewModel(albumRepository)
    private val artistSyncViewModel = StarredArtistsSyncViewModel(artistRepository)

    private val discoverSongSample = MutableLiveData<List<Child>?>(null)
    private val newReleasedAlbum = MutableLiveData<List<AlbumID3>?>(null)
    private val starredTracksSample = MutableLiveData<List<Child>?>(null)
    private val starredArtistsSample = MutableLiveData<List<ArtistID3>?>(null)
    private val bestOfArtists = MutableLiveData<List<ArtistID3>?>(null)
    private val starredTracks = MutableLiveData<List<Child>?>(null)
    private val starredAlbums = MutableLiveData<List<AlbumID3>?>(null)
    private val starredArtists = MutableLiveData<List<ArtistID3>?>(null)
    private val mostPlayedAlbumSample = MutableLiveData<List<AlbumID3>?>(null)
    private val recentlyPlayedAlbumSample = MutableLiveData<List<AlbumID3>?>(null)
    private val years = MutableLiveData<List<Int>?>(null)
    private val recentlyAddedAlbumSample = MutableLiveData<List<AlbumID3>?>(null)
    private val thisGridTopSong = MutableLiveData<List<Chronology>?>(null)
    private val mediaInstantMix = MutableLiveData<List<Child>?>(null)
    private val artistInstantMix = MutableLiveData<List<Child>?>(null)
    private val pinnedPlaylists = MutableLiveData<List<Playlist>?>(null)
    private val shares = MutableLiveData<List<Share>?>(null)

    private var sectors: List<HomeSector>? = null

    init {
        setHomeSectorList()
        setOfflineFavorite()
    }

    fun getDiscoverSongSample(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<Child>?> {
        if (discoverSongSample.value == null) {
            songRepository.getRandomSample(10, null, null).observe(owner) { discoverSongSample.postValue(it) }
        }

        return discoverSongSample
    }

    fun getRandomShuffleSample(): LiveData<List<Child>> {
        return songRepository.getRandomSample(100, null, null)
    }

    fun getChronologySample(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<Chronology>?> {
        val cal = Calendar.getInstance()
        val server = Preferences.getServerId()

        val currentWeek = cal.get(Calendar.WEEK_OF_YEAR)
        val start = cal.timeInMillis

        cal.set(Calendar.WEEK_OF_YEAR, currentWeek - 1)
        val end = cal.timeInMillis

        chronologyRepository.getChronology(server ?: "", start, end).observe(owner) { thisGridTopSong.postValue(it) }
        return thisGridTopSong
    }

    fun getRecentlyReleasedAlbums(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<AlbumID3>?> {
        if (newReleasedAlbum.value == null) {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)

            albumRepository.getAlbums("byYear", 500, currentYear, currentYear).observe(owner) { albums ->
                if (albums != null) {
                    albums.sortedByDescending { it.created }
                    val sorted = albums.sortedWith(Comparator.comparing<AlbumID3, java.util.Date?> { it.created }.reversed())
                    newReleasedAlbum.postValue(sorted.take(20))
                }
            }
        }

        return newReleasedAlbum
    }

    fun getStarredTracksSample(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<Child>?> {
        if (starredTracksSample.value == null) {
            songRepository.getStarredSongs(true, 10).observe(owner) { starredTracksSample.postValue(it) }
        }

        return starredTracksSample
    }

    fun getStarredArtistsSample(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<ArtistID3>?> {
        if (starredArtistsSample.value == null) {
            artistRepository.getStarredArtists(true, 10).observe(owner) { starredArtistsSample.postValue(it) }
        }

        return starredArtistsSample
    }

    fun getBestOfArtists(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<ArtistID3>?> {
        if (bestOfArtists.value == null) {
            artistRepository.getStarredArtists(true, 20).observe(owner) { bestOfArtists.postValue(it) }
        }

        return bestOfArtists
    }

    fun getStarredTracks(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<Child>?> {
        if (starredTracks.value == null) {
            songRepository.getStarredSongs(true, 20).observe(owner) { starredTracks.postValue(it) }
        }

        return starredTracks
    }

    fun getStarredAlbums(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<AlbumID3>?> {
        if (starredAlbums.value == null) {
            albumRepository.getStarredAlbums(true, 20).observe(owner) { starredAlbums.postValue(it) }
        }

        return starredAlbums
    }

    fun getAllStarredAlbumSongs(): LiveData<List<Child>?> = albumsSyncViewModel.getAllStarredAlbumSongs()

    fun getAllStarredArtistSongs(): LiveData<List<Child>?> = artistSyncViewModel.getAllStarredArtistSongs()

    fun getStarredArtists(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<ArtistID3>?> {
        if (starredArtists.value == null) {
            artistRepository.getStarredArtists(true, 20).observe(owner) { starredArtists.postValue(it) }
        }

        return starredArtists
    }

    fun getYearList(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<Int>?> {
        if (years.value == null) {
            albumRepository.getDecades().observe(owner) { years.postValue(it) }
        }

        return years
    }

    fun getMostPlayedAlbums(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<AlbumID3>?> {
        if (mostPlayedAlbumSample.value == null) {
            albumRepository.getAlbums("frequent", 20, null, null).observe(owner) { mostPlayedAlbumSample.postValue(it) }
        }

        return mostPlayedAlbumSample
    }

    fun getMostRecentlyAddedAlbums(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<AlbumID3>?> {
        if (recentlyAddedAlbumSample.value == null) {
            albumRepository.getAlbums("newest", 20, null, null).observe(owner) { recentlyAddedAlbumSample.postValue(it) }
        }

        return recentlyAddedAlbumSample
    }

    fun getRecentlyPlayedAlbumList(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<AlbumID3>?> {
        if (recentlyPlayedAlbumSample.value == null) {
            albumRepository.getAlbums("recent", 20, null, null).observe(owner) { recentlyPlayedAlbumSample.postValue(it) }
        }

        return recentlyPlayedAlbumSample
    }

    fun getMediaInstantMix(owner: androidx.lifecycle.LifecycleOwner, media: Child): LiveData<List<Child>?> {
        mediaInstantMix.value = Collections.emptyList()
        songRepository.getInstantMix(media.id, SeedType.TRACK, 20).observe(owner) { mediaInstantMix.postValue(it) }
        return mediaInstantMix
    }

    fun getArtistInstantMix(owner: androidx.lifecycle.LifecycleOwner, artist: ArtistID3): LiveData<List<Child>?> {
        artistInstantMix.value = Collections.emptyList()
        artistRepository.getTopSongs(artist.name ?: "", 10).observe(owner) { artistInstantMix.postValue(it) }
        return artistInstantMix
    }

    fun getArtistBestOf(artist: ArtistID3?): LiveData<List<Child>> {
        val result = MutableLiveData<List<Child>>()
        if (artist == null) {
            result.value = ArrayList()
            return result
        }

        val source = artistRepository.getTopSongs(artist.name ?: "", 10)
        val observer = object : Observer<List<Child>> {
            override fun onChanged(songs: List<Child>) {
                result.value = songs.ifEmpty { ArrayList() }
                source.removeObserver(this)
            }
        }
        source.observeForever(observer)

        return result
    }

    fun getPinnedPlaylists(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<Playlist>?> {
        val sortOrder = Preferences.getHomeSortPlaylists()

        playlistRepository.getSortedPlaylistsPreview(sortOrder, 5).observe(owner) { playlists ->
            if (playlists != null) {
                pinnedPlaylists.value = playlists
            }
        }

        return pinnedPlaylists
    }

    fun getShares(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<Share>?> {
        if (shares.value == null) {
            sharingRepository.getShares().observe(owner) { shares.postValue(it) }
        }

        return shares
    }

    fun getAllStarredTracks(): LiveData<List<Child>> = songRepository.getStarredSongs(false, -1)

    fun changeChronologyPeriod(owner: androidx.lifecycle.LifecycleOwner, period: Int) {
        val cal = Calendar.getInstance()
        val server = Preferences.getServerId()
        val currentWeek = cal.get(Calendar.WEEK_OF_YEAR)

        val start: Long
        val end: Long

        when (period) {
            0 -> {
                start = cal.timeInMillis
                cal.set(Calendar.WEEK_OF_YEAR, currentWeek - 1)
                end = cal.timeInMillis
            }
            1 -> {
                start = cal.timeInMillis
                cal.set(Calendar.WEEK_OF_YEAR, currentWeek - 4)
                end = cal.timeInMillis
            }
            2 -> {
                start = cal.timeInMillis
                cal.set(Calendar.WEEK_OF_YEAR, currentWeek - 52)
                end = cal.timeInMillis
            }
            else -> {
                start = cal.timeInMillis
                end = cal.timeInMillis
            }
        }

        chronologyRepository.getChronology(server ?: "", start, end).observe(owner) { thisGridTopSong.postValue(it) }
    }

    fun refreshDiscoverySongSample(owner: androidx.lifecycle.LifecycleOwner) {
        songRepository.getRandomSample(10, null, null).observe(owner) { discoverSongSample.postValue(it) }
    }

    fun refreshSimilarSongSample(owner: androidx.lifecycle.LifecycleOwner) {
        songRepository.getStarredSongs(true, 10).observe(owner) { starredTracksSample.postValue(it) }
    }

    fun refreshRadioArtistSample(owner: androidx.lifecycle.LifecycleOwner) {
        artistRepository.getStarredArtists(true, 10).observe(owner) { starredArtistsSample.postValue(it) }
    }

    fun refreshBestOfArtist(owner: androidx.lifecycle.LifecycleOwner) {
        artistRepository.getStarredArtists(true, 20).observe(owner) { bestOfArtists.postValue(it) }
    }

    fun refreshStarredTracks(owner: androidx.lifecycle.LifecycleOwner) {
        songRepository.getStarredSongs(true, 20).observe(owner) { starredTracks.postValue(it) }
    }

    fun refreshStarredAlbums(owner: androidx.lifecycle.LifecycleOwner) {
        albumRepository.getStarredAlbums(true, 20).observe(owner) { starredAlbums.postValue(it) }
    }

    fun refreshStarredArtists(owner: androidx.lifecycle.LifecycleOwner) {
        artistRepository.getStarredArtists(true, 20).observe(owner) { starredArtists.postValue(it) }
    }

    fun refreshMostPlayedAlbums(owner: androidx.lifecycle.LifecycleOwner) {
        albumRepository.getAlbums("frequent", 20, null, null).observe(owner) { mostPlayedAlbumSample.postValue(it) }
    }

    fun refreshMostRecentlyAddedAlbums(owner: androidx.lifecycle.LifecycleOwner) {
        albumRepository.getAlbums("newest", 20, null, null).observe(owner) { recentlyAddedAlbumSample.postValue(it) }
    }

    fun refreshRecentlyPlayedAlbumList(owner: androidx.lifecycle.LifecycleOwner) {
        albumRepository.getAlbums("recent", 20, null, null).observe(owner) { recentlyPlayedAlbumSample.postValue(it) }
    }

    fun refreshShares(owner: androidx.lifecycle.LifecycleOwner) {
        sharingRepository.getShares().observe(owner) { shares.postValue(it) }
    }

    private fun setHomeSectorList() {
        val stored = Preferences.getHomeSectorList()
        sectors = if (!stored.isNullOrEmpty() && stored != "null") {
            Gson().fromJson<List<HomeSector>?>(
                stored,
                object : TypeToken<List<HomeSector>>() {}.type,
            )?.sortedBy { it.order } ?: defaultHomeSectorList()
        } else {
            defaultHomeSectorList()
        }
    }

    fun getHomeSectorList(): List<HomeSector> {
        if (sectors == null) {
            sectors = defaultHomeSectorList()
        }
        return sectors ?: emptyList()
    }

    fun checkHomeSectorVisibility(sectorId: String): Boolean {
        return getHomeSectorList().firstOrNull { it.id == sectorId }?.isVisible == false
    }

    private fun defaultHomeSectorList(): List<HomeSector> = listOf(
        HomeSector(Constants.HOME_SECTOR_DISCOVERY, "", true, 1),
        HomeSector(Constants.HOME_SECTOR_MADE_FOR_YOU, "", true, 2),
        HomeSector(Constants.HOME_SECTOR_BEST_OF, "", true, 3),
        HomeSector(Constants.HOME_SECTOR_RADIO_STATION, "", true, 4),
        HomeSector(Constants.HOME_SECTOR_TOP_SONGS, "", true, 5),
        HomeSector(Constants.HOME_SECTOR_STARRED_TRACKS, "", true, 6),
        HomeSector(Constants.HOME_SECTOR_STARRED_ALBUMS, "", true, 7),
        HomeSector(Constants.HOME_SECTOR_STARRED_ARTISTS, "", true, 8),
        HomeSector(Constants.HOME_SECTOR_NEW_RELEASES, "", true, 9),
        HomeSector(Constants.HOME_SECTOR_FLASHBACK, "", true, 10),
        HomeSector(Constants.HOME_SECTOR_MOST_PLAYED, "", true, 11),
        HomeSector(Constants.HOME_SECTOR_LAST_PLAYED, "", true, 12),
        HomeSector(Constants.HOME_SECTOR_RECENTLY_ADDED, "", true, 13),
        HomeSector(Constants.HOME_SECTOR_PINNED_PLAYLISTS, "", true, 14),
        HomeSector(Constants.HOME_SECTOR_SHARED, "", true, 15),
    )

    fun setOfflineFavorite() {
        val favorites = getFavorites()
        val favoritesToSave = getFavoritesToSave(favorites)
        val favoritesToDelete = getFavoritesToDelete(favorites, favoritesToSave)

        manageFavoriteToSave(favoritesToSave)
        manageFavoriteToDelete(favoritesToDelete)
    }

    private fun getFavorites(): ArrayList<Favorite> {
        return ArrayList(favoriteRepository.getFavorites())
    }

    private fun getFavoritesToSave(favorites: ArrayList<Favorite>): ArrayList<Favorite> {
        val filteredMap = HashMap<String, Favorite>()

        for (favorite in favorites) {
            val key = favorite.toString()

            if (!filteredMap.containsKey(key) || favorite.timestamp > filteredMap[key]!!.timestamp) {
                filteredMap[key] = favorite
            }
        }

        return ArrayList(filteredMap.values)
    }

    private fun getFavoritesToDelete(
        favorites: ArrayList<Favorite>,
        favoritesToSave: ArrayList<Favorite>,
    ): ArrayList<Favorite> {
        val favoritesToDelete = ArrayList<Favorite>()

        for (favorite in favorites) {
            if (!favoritesToSave.contains(favorite)) {
                favoritesToDelete.add(favorite)
            }
        }

        return favoritesToDelete
    }

    private fun manageFavoriteToSave(favoritesToSave: ArrayList<Favorite>) {
        for (favorite in favoritesToSave) {
            if (favorite.toStar) {
                favoriteToStar(favorite)
            } else {
                favoriteToUnstar(favorite)
            }
        }
    }

    private fun manageFavoriteToDelete(favoritesToDelete: ArrayList<Favorite>) {
        for (favorite in favoritesToDelete) {
            favoriteRepository.delete(favorite)
        }
    }

    private fun favoriteToStar(favorite: Favorite) {
        when {
            favorite.songId != null -> favoriteRepository.star(favorite.songId, null, null, object : StarCallback {
                override fun onSuccess() {
                    favoriteRepository.delete(favorite)
                }
            })
            favorite.albumId != null -> favoriteRepository.star(null, favorite.albumId, null, object : StarCallback {
                override fun onSuccess() {
                    favoriteRepository.delete(favorite)
                }
            })
            favorite.artistId != null -> favoriteRepository.star(null, null, favorite.artistId, object : StarCallback {
                override fun onSuccess() {
                    favoriteRepository.delete(favorite)
                }
            })
        }
    }

    private fun favoriteToUnstar(favorite: Favorite) {
        when {
            favorite.songId != null -> favoriteRepository.unstar(favorite.songId, null, null, object : StarCallback {
                override fun onSuccess() {
                    favoriteRepository.delete(favorite)
                }
            })
            favorite.albumId != null -> favoriteRepository.unstar(null, favorite.albumId, null, object : StarCallback {
                override fun onSuccess() {
                    favoriteRepository.delete(favorite)
                }
            })
            favorite.artistId != null -> favoriteRepository.unstar(null, null, favorite.artistId, object : StarCallback {
                override fun onSuccess() {
                    favoriteRepository.delete(favorite)
                }
            })
        }
    }
}
