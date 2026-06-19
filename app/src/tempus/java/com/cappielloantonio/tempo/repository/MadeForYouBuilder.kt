package com.cappielloantonio.tempo.repository

import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.database.AppDatabase
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepository
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.ConstantsAA
import com.cappielloantonio.tempo.util.Preferences
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

@UnstableApi
class MadeForYouBuilder(private val repository: AutomotiveRepository) {
    private val chronologyDao = AppDatabase.getInstance().chronologyDao()
    private val isRunning = AtomicBoolean(false)
    private val subsonicRepository: SubsonicRepository = App.get(SubsonicRepository::class.java)

    private enum class MixStep { RECENT, STARRED_ALBUM, STARRED_ARTIST, STARRED_TRACKS }

    companion object {
        private const val TAG = "MadeForYouBuilder"
        private const val MAX_CYCLES = 100
    }

    fun buildAndEnqueue(
        mixType: String,
        usedTrackId: String,
        count: Int,
        browserFuture: ListenableFuture<MediaBrowser>
    ) {
        if (!isRunning.compareAndSet(false, true)) {
            Log.d(TAG, "$mixType Build already running, skipping")
            return
        }

        Log.d(TAG, "$mixType Building remaining $count tracks")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (mixType == ConstantsAA.QUICKMIX_ID) {
                    val response = subsonicRepository.getAlbumList2("recent", ConstantsAA.NUMBER_OF_RECENT_ALBUMS_FOR_MIX, 0, null, null)
                    val recentAlbums = response?.albumList2?.albums?.toMutableList() ?: mutableListOf()
                    
                    if (recentAlbums.isNotEmpty()) {
                        Log.d(TAG, "$mixType recent albums loaded: ${recentAlbums.size}")
                        val usedTrackIds = mutableSetOf(usedTrackId)
                        runMixLoop(mixType, count, usedTrackIds, recentAlbums, mutableListOf(), mutableListOf(), mutableListOf(), emptySet(), browserFuture)
                    } else {
                        fallbackToRandomSongs(count, usedTrackId, browserFuture)
                    }
                    return@launch
                }

                // MY_MIX and DISCOVERY_MIX
                val recentResponse = async { subsonicRepository.getAlbumList2("recent", ConstantsAA.NUMBER_OF_RECENT_ALBUMS_FOR_MIX, 0, null, null) }
                val starredResponse = async { subsonicRepository.getStarred2() }
                val recentTracksIds = async { 
                    chronologyDao.getLastPlayedSync(Preferences.getServerId(), ConstantsAA.NUMBER_OF_RECENT_TRACKS_FOR_MIX).map { it.id }.toSet()
                }

                val recentAlbums = recentResponse.await()?.albumList2?.albums?.toMutableList() ?: mutableListOf()
                val starred = starredResponse.await()?.starred2
                val starredAlbums = starred?.albums?.toMutableList() ?: mutableListOf()
                val starredArtists = starred?.artists?.toMutableList() ?: mutableListOf()
                val starredTracks = starred?.songs?.toMutableList() ?: mutableListOf()
                val recentTracks = recentTracksIds.await()

                if (recentAlbums.isEmpty() && starredAlbums.isEmpty() && starredArtists.isEmpty()) {
                    fallbackToRandomSongs(count, usedTrackId, browserFuture)
                } else {
                    val usedTrackIds = mutableSetOf(usedTrackId)
                    runMixLoop(mixType, count, usedTrackIds, recentAlbums, starredAlbums, starredArtists, starredTracks, recentTracks, browserFuture)
                }

            } catch (e: Exception) {
                Log.e(TAG, "$mixType Error building mix", e)
            } finally {
                isRunning.set(false)
            }
        }
    }

    private suspend fun runMixLoop(
        mixType: String,
        count: Int,
        usedTrackIds: MutableSet<String>,
        recentAlbums: MutableList<AlbumID3>,
        starredAlbums: MutableList<AlbumID3>,
        starredArtists: MutableList<ArtistID3>,
        starredTracks: MutableList<Child>,
        recentTrackIds: Set<String>,
        browserFuture: ListenableFuture<MediaBrowser>
    ) {
        val mixTracks = mutableListOf<Child>()
        var cycleIndex = 1
        
        var recentIdx = 0
        var starredAlbumIdx = 0
        var starredArtistIdx = 0
        var starredTracksIdx = 0

        while (mixTracks.size < count && cycleIndex <= MAX_CYCLES) {
            val currentStep = getNextStep(cycleIndex, mixType, recentAlbums, starredAlbums, starredArtists, starredTracks)
            var songIdAdded: String? = null

            when (currentStep) {
                MixStep.RECENT -> {
                    if (recentAlbums.isNotEmpty()) {
                        val album = recentAlbums[recentIdx % recentAlbums.size]
                        songIdAdded = fetchAndAddFromAlbum(album.id, mixTracks, usedTrackIds)
                        recentIdx++
                    }
                }
                MixStep.STARRED_ALBUM -> {
                    if (starredAlbums.isNotEmpty()) {
                        val album = starredAlbums[starredAlbumIdx % starredAlbums.size]
                        songIdAdded = fetchAndAddFromAlbum(album.id, mixTracks, usedTrackIds)
                        starredAlbumIdx++
                    }
                }
                MixStep.STARRED_ARTIST -> {
                    if (starredArtists.isNotEmpty()) {
                        val artist = starredArtists[starredArtistIdx % starredArtists.size]
                        val response = subsonicRepository.getArtist(artist.id!!)
                        val artistAlbums = response?.artist?.albums
                        if (!artistAlbums.isNullOrEmpty()) {
                            val album = artistAlbums.shuffled().first()
                            songIdAdded = fetchAndAddFromAlbum(album.id, mixTracks, usedTrackIds)
                        }
                        starredArtistIdx++
                    }
                }
                MixStep.STARRED_TRACKS -> {
                    if (starredTracks.isNotEmpty()) {
                        val track = starredTracks[starredTracksIdx % starredTracks.size]
                        if (!usedTrackIds.contains(track.id)) {
                            mixTracks.add(track)
                            usedTrackIds.add(track.id)
                            songIdAdded = track.id
                        }
                        starredTracksIdx++
                    }
                }
            }

            // Discovery similar songs
            if (mixType == ConstantsAA.DISCOVERYMIX_ID && songIdAdded != null && mixTracks.size < count) {
                val similarResponse = subsonicRepository.getSimilarSongs(songIdAdded, 10)
                val similar = similarResponse?.similarSongs?.songs ?: emptyList()
                val candidate = similar.shuffled().find { !usedTrackIds.contains(it.id) && !recentTrackIds.contains(it.id) }
                candidate?.let {
                    mixTracks.add(it)
                    usedTrackIds.add(it.id)
                }
            }

            cycleIndex++
        }

        withContext(Dispatchers.Main) {
            enqueueMix(mixTracks, mixType, browserFuture)
        }
    }

    private suspend fun fetchAndAddFromAlbum(albumId: String?, mixTracks: MutableList<Child>, usedTrackIds: MutableSet<String>): String? {
        if (albumId == null) return null
        val response = subsonicRepository.getAlbum(albumId)
        val songs = response?.album?.songs ?: emptyList()
        val candidate = songs.shuffled().find { !usedTrackIds.contains(it.id) }
        return candidate?.let {
            mixTracks.add(it)
            usedTrackIds.add(it.id)
            it.id
        }
    }

    private fun getNextStep(
        cycleIndex: Int,
        mixType: String,
        recentAlbums: MutableList<AlbumID3>,
        starredAlbums: MutableList<AlbumID3>,
        starredArtists: MutableList<ArtistID3>,
        starredTracks: MutableList<Child>
    ): MixStep {
        if (mixType == ConstantsAA.QUICKMIX_ID) {
            if (cycleIndex % 2 == 0) recentAlbums.shuffle()
            return MixStep.RECENT
        } else {
            val posInCycle = cycleIndex % 4
            if (posInCycle == 0) {
                recentAlbums.shuffle()
                starredAlbums.shuffle()
                starredArtists.shuffle()
                starredTracks.shuffle()
            }
            return if (posInCycle == 0 || posInCycle == 2) {
                MixStep.RECENT
            } else {
                when (Preferences.getAndroidAutoStarredForMadeForYou()) {
                    2 -> MixStep.STARRED_TRACKS
                    1 -> MixStep.STARRED_ALBUM
                    else -> MixStep.STARRED_ARTIST
                }
            }
        }
    }

    private suspend fun fallbackToRandomSongs(count: Int, usedTrackId: String, browserFuture: ListenableFuture<MediaBrowser>) {
        val response = subsonicRepository.getRandomSongs(count, null, null, null)
        val songs = response?.randomSongs?.songs?.toMutableList() ?: mutableListOf()
        songs.removeAll { it.id == usedTrackId }
        withContext(Dispatchers.Main) {
            repository.setChildrenMetadata(songs)
            MediaManager.enqueue(browserFuture, songs, true)
        }
    }

    private fun enqueueMix(mixTracks: List<Child>, mixType: String, browserFuture: ListenableFuture<MediaBrowser>) {
        Log.d(TAG, "$mixType complete with ${mixTracks.size} tracks, enqueuing")
        val finalTracks = if (mixType == ConstantsAA.DISCOVERYMIX_ID) mixTracks.shuffled() else mixTracks
        repository.setChildrenMetadata(finalTracks)
        MediaManager.enqueue(browserFuture, finalTracks, true)
    }
}
