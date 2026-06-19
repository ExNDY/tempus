package com.cappielloantonio.tempo.viewmodel

import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.interfaces.StarCallback
import com.cappielloantonio.tempo.model.LyricsCache
import com.cappielloantonio.tempo.model.Queue
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.FavoriteRepository
import com.cappielloantonio.tempo.repository.LyricsRepository
import com.cappielloantonio.tempo.repository.OpenRepository
import com.cappielloantonio.tempo.repository.QueueRepository
import com.cappielloantonio.tempo.repository.SongRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.LyricsList
import com.cappielloantonio.tempo.subsonic.models.PlayQueue
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.NetworkUtil
import com.cappielloantonio.tempo.util.OpenSubsonicExtensionsUtil
import com.cappielloantonio.tempo.util.Preferences
import com.google.gson.Gson
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.Collections
import java.util.Date

@OptIn(UnstableApi::class)
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

    private val lyricsLiveData = MutableLiveData<String?>(null)
    private val lyricsListLiveData = MutableLiveData<LyricsList?>(null)
    private val lyricsCachedLiveData = MutableLiveData(false)
    private val descriptionLiveData = MutableLiveData<String?>(null)
    private val liveMedia = MutableLiveData<Child?>(null)
    private val liveAlbum = MutableLiveData<AlbumID3?>(null)
    private val liveArtist = MutableLiveData<ArtistID3?>(null)
    private val instantMix = MutableLiveData<List<Child>?>(null)
    private val gson = Gson()
    private val _actions = Channel<Action>(Channel.BUFFERED)
    val actions: LiveData<Action> = _actions.receiveAsFlow().asLiveData()

    private var lyricsSyncState = true
    private var cachedLyricsSource: LiveData<LyricsCache?>? = null
    private var currentSongId: String? = null

    private val cachedLyricsObserver = Observer<LyricsCache?> { onCachedLyricsChanged(it) }

    fun getQueueSong(): LiveData<List<Queue>> = queueRepository.getLiveQueue()

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
    }

    private fun removeFavoriteOnline(media: Child) {
        favoriteRepository.unstar(media.id, null, null, object : StarCallback {
            override fun onError() {
                favoriteRepository.starLater(media.id, null, null, false)
            }
        })
        media.starred = null
    }

    private fun setFavoriteOffline(media: Child) {
        favoriteRepository.starLater(media.id, null, null, true)
        media.starred = Date()
    }

    private fun setFavoriteOnline(media: Child) {
        favoriteRepository.star(media.id, null, null, object : StarCallback {
            override fun onError() {
                favoriteRepository.starLater(media.id, null, null, true)
            }
        })

        media.starred = Date()

        if (Preferences.isStarredSyncEnabled() && Preferences.getDownloadDirectoryUri() == null) {
            sendAction(_actions, Action.RequestDownload(media))
        }
    }

    fun getLiveLyrics(): LiveData<String?> = lyricsLiveData

    fun getLiveLyricsList(): LiveData<LyricsList?> = lyricsListLiveData

    fun refreshMediaInfo(owner: LifecycleOwner?, media: Child?) {
        lyricsLiveData.postValue(null)
        lyricsListLiveData.postValue(null)
        lyricsCachedLiveData.postValue(false)

        clearCachedLyricsObserver()

        val songId = media?.id ?: currentSongId
        if (TextUtils.isEmpty(songId) || owner == null) return
        val resolvedSongId = songId ?: return

        currentSongId = resolvedSongId
        observeCachedLyrics(owner, resolvedSongId)

        val cachedLyrics = lyricsRepository.getLyrics(resolvedSongId)
        if (cachedLyrics != null) {
            onCachedLyricsChanged(cachedLyrics)
        }

        if (NetworkUtil.isOffline() || media == null) return

        if (OpenSubsonicExtensionsUtil.isSongLyricsExtensionAvailable()) {
            openRepository.getLyricsBySongId(media.id).observe(owner) { lyricsList ->
                lyricsListLiveData.postValue(lyricsList)
                lyricsLiveData.postValue(null)

                if (shouldAutoDownloadLyrics() && hasStructuredLyrics(lyricsList)) {
                    saveLyricsToCache(media, null, lyricsList)
                }
            }
        } else {
            songRepository.getSongLyrics(media).observe(owner) { lyrics ->
                lyricsLiveData.postValue(lyrics)
                lyricsListLiveData.postValue(null)

                if (shouldAutoDownloadLyrics() && !TextUtils.isEmpty(lyrics)) {
                    saveLyricsToCache(media, lyrics, null)
                }
            }
        }
    }

    fun getLiveMedia(): LiveData<Child?> = liveMedia

    fun setLiveMedia(owner: LifecycleOwner, mediaType: String?, mediaId: String?) {
        currentSongId = mediaId

        if (!TextUtils.isEmpty(mediaId)) {
            refreshMediaInfo(owner, null)
        } else {
            clearCachedLyricsObserver()
            lyricsLiveData.postValue(null)
            lyricsListLiveData.postValue(null)
            lyricsCachedLiveData.postValue(false)
        }

        if (mediaType != null) {
            when (mediaType) {
                Constants.MEDIA_TYPE_MUSIC -> {
                    songRepository.getSong(mediaId ?: "").observe(owner) { liveMedia.postValue(it) }
                    descriptionLiveData.postValue(null)
                }
                Constants.MEDIA_TYPE_PODCAST -> liveMedia.postValue(null)
                else -> liveMedia.postValue(null)
            }
        } else {
            liveMedia.postValue(null)
        }
    }

    fun getLiveAlbum(): LiveData<AlbumID3?> = liveAlbum

    fun setLiveAlbum(owner: LifecycleOwner, mediaType: String?, albumId: String?) {
        if (mediaType != null) {
            when (mediaType) {
                Constants.MEDIA_TYPE_MUSIC -> albumRepository.getAlbum(albumId ?: "").observe(owner) {
                    liveAlbum.postValue(it)
                }
                Constants.MEDIA_TYPE_PODCAST -> liveAlbum.postValue(null)
            }
        }
    }

    fun getLiveArtist(): LiveData<ArtistID3?> = liveArtist

    fun setLiveArtist(owner: LifecycleOwner, mediaType: String?, artistId: String?) {
        if (mediaType != null) {
            when (mediaType) {
                Constants.MEDIA_TYPE_MUSIC -> artistRepository.getArtist(artistId ?: "").observe(owner) {
                    liveArtist.postValue(it)
                }
                Constants.MEDIA_TYPE_PODCAST -> liveArtist.postValue(null)
            }
        }
    }

    fun setLiveDescription(description: String?) {
        descriptionLiveData.postValue(description)
    }

    fun getLiveDescription(): LiveData<String?> = descriptionLiveData

    fun getMediaInstantMix(owner: LifecycleOwner, media: Child): LiveData<List<Child>?> {
        instantMix.value = Collections.emptyList()
        songRepository.getInstantMix(media.id, Constants.SeedType.TRACK, 20).observe(owner) {
            instantMix.postValue(it)
        }
        return instantMix
    }

    fun getPlayQueue(): LiveData<PlayQueue?> = queueRepository.getPlayQueue()

    fun savePlayQueue(): Boolean {
        val media = liveMedia.value
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

    private fun observeCachedLyrics(owner: LifecycleOwner, songId: String) {
        if (TextUtils.isEmpty(songId)) return

        cachedLyricsSource = lyricsRepository.observeLyrics(songId)
        cachedLyricsSource?.observe(owner, cachedLyricsObserver)
    }

    private fun clearCachedLyricsObserver() {
        cachedLyricsSource?.removeObserver(cachedLyricsObserver)
        cachedLyricsSource = null
    }

    private fun onCachedLyricsChanged(lyricsCache: LyricsCache?) {
        if (lyricsCache == null) {
            lyricsCachedLiveData.postValue(false)
            return
        }

        lyricsCachedLiveData.postValue(true)

        if (!TextUtils.isEmpty(lyricsCache.structuredLyrics)) {
            try {
                val cachedList = gson.fromJson(lyricsCache.structuredLyrics, LyricsList::class.java)
                lyricsListLiveData.postValue(cachedList)
                lyricsLiveData.postValue(null)
            } catch (_: Exception) {
                lyricsListLiveData.postValue(null)
                lyricsLiveData.postValue(lyricsCache.lyrics)
            }
        } else {
            lyricsListLiveData.postValue(null)
            lyricsLiveData.postValue(lyricsCache.lyrics)
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
        lyricsCachedLiveData.postValue(true)
    }

    private fun hasStructuredLyrics(lyricsList: LyricsList?): Boolean {
        return lyricsList?.structuredLyrics?.firstOrNull()?.line?.isNotEmpty() == true
    }

    private fun shouldAutoDownloadLyrics(): Boolean = Preferences.isAutoDownloadLyricsEnabled()

    fun downloadCurrentLyrics(): Boolean {
        val media = liveMedia.value ?: return false
        val lyricsList = lyricsListLiveData.value
        val lyrics = lyricsLiveData.value

        if ((lyricsList == null || !hasStructuredLyrics(lyricsList)) && TextUtils.isEmpty(lyrics)) {
            return false
        }

        saveLyricsToCache(media, lyrics, lyricsList)
        return true
    }

    fun getLyricsCachedState(): LiveData<Boolean> = lyricsCachedLiveData

    fun changeSyncLyricsState() {
        lyricsSyncState = !lyricsSyncState
    }

    fun getSyncLyricsState(): Boolean = lyricsSyncState

    companion object {
        private const val TAG = "PlayerBottomSheetViewModel"
    }
}
