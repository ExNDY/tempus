package com.cappielloantonio.tempo.repository

import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepository
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

@UnstableApi
class InstantMixBuilder(private val repository: AutomotiveRepository) {
    private val isRunning = AtomicBoolean(false)
    private val subsonicRepository: SubsonicRepository = App.get(SubsonicRepository::class.java)

    companion object {
        private const val TAG = "InstantMixBuilder"
    }

    fun buildAndEnqueue(
        artistId: String,
        usedTrackId: String,
        count: Int,
        browserFuture: ListenableFuture<MediaBrowser>
    ) {
        if (!isRunning.compareAndSet(false, true)) {
            Log.d(TAG, "Build already running, skipping")
            return
        }

        Log.d(TAG, "Building remaining $count tracks for artist $artistId")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = subsonicRepository.getArtist(artistId)
                val albums = response?.artist?.albums?.toMutableList() ?: mutableListOf()

                if (albums.isNotEmpty()) {
                    val mixTracks = mutableListOf<Child>()
                    val usedTrackIds = mutableSetOf<String>()
                    usedTrackIds.add(usedTrackId)

                    val random = Random()
                    var albumIndex = 0

                    while (mixTracks.size < count && albums.isNotEmpty()) {
                        if (albumIndex == 0) {
                            albums.shuffle(random)
                            Log.d(TAG, "New cycle, albums shuffled")
                        }

                        val album = albums[albumIndex % albums.size]
                        val albumResponse = album.id?.let { subsonicRepository.getAlbum(it) }
                        val songs = albumResponse?.album?.songs

                        if (!songs.isNullOrEmpty()) {
                            val candidate = songs[random.nextInt(songs.size)]
                            if (!usedTrackIds.contains(candidate.id)) {
                                mixTracks.add(candidate)
                                usedTrackIds.add(candidate.id)
                                Log.d(TAG, "Added track [${mixTracks.size}/$count] ${candidate.title} from ${album.name}")
                            }
                        }

                        albumIndex++
                    }

                    withContext(Dispatchers.Main) {
                        Log.d(TAG, "Mix complete with ${mixTracks.size} tracks, enqueuing")
                        repository.setChildrenMetadata(mixTracks)
                        MediaManager.enqueue(browserFuture, mixTracks, true)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error building instant mix", e)
            } finally {
                isRunning.set(false)
            }
        }
    }
}
