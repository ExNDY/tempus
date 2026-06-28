package com.cappielloantonio.tempo.repository

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
import androidx.media3.session.SessionError
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.BuildConfig
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.database.AppDatabase
import com.cappielloantonio.tempo.model.SessionMediaItem
import com.cappielloantonio.tempo.provider.AlbumArtContentProvider
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepository
import com.cappielloantonio.tempo.subsonic.models.*
import com.cappielloantonio.tempo.util.*
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.*
import java.util.*

@UnstableApi
class AutomotiveRepository {
    private val sessionMediaItemDao = AppDatabase.getInstance().sessionMediaItemDao()
    private val chronologyDao = AppDatabase.getInstance().chronologyDao()
    private val subsonicRepository: SubsonicRepository = App.get(SubsonicRepository::class.java)

    val instantMixBuilder = InstantMixBuilder(this)
    val madeForYouBuilder = MadeForYouBuilder(this)

    companion object {
        private const val TAG = "AutomotiveRepository"
    }

    private fun createContentStyleExtras(gridView: Boolean): Bundle {
        val extras = Bundle()
        val contentStyle = if (gridView) MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM else MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
        extras.putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, contentStyle)
        extras.putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE, contentStyle)
        return extras
    }

    private fun createFunction(title: String, id: String, isGridView: Boolean, artworkUri: Uri): MediaItem {
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(title)
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setArtworkUri(artworkUri)
            .setExtras(createContentStyleExtras(isGridView))
            .build()

        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(mediaMetadata)
            .setUri("")
            .build()
    }

    private fun createArtist(artistName: String?, id: String, isGridView: Boolean, artistCoverArtId: String?): MediaItem {
        val artworkUri = if (!artistCoverArtId.isNullOrEmpty()) AlbumArtContentProvider.contentUri(artistCoverArtId)
        else Uri.parse("android.resource://" + BuildConfig.APPLICATION_ID + "/" + R.drawable.ic_aa_artists)

        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(artistName)
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_ARTIST)
            .setArtworkUri(artworkUri)
            .setExtras(createContentStyleExtras(isGridView))
            .build()

        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(mediaMetadata)
            .setUri("")
            .build()
    }

    private fun createAlbum(albumName: String?, artistName: String?, genre: String?, id: String, isPlayable: Boolean, albumCoverArtId: String?): MediaItem {
        val artworkUri = if (!albumCoverArtId.isNullOrEmpty()) AlbumArtContentProvider.contentUri(albumCoverArtId)
        else Uri.parse("android.resource://" + BuildConfig.APPLICATION_ID + "/" + R.drawable.ic_aa_albums)

        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(albumName)
            .setAlbumTitle(albumName)
            .setArtist(artistName)
            .setGenre(genre)
            .setIsBrowsable(!isPlayable)
            .setIsPlayable(isPlayable)
            .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
            .setArtworkUri(artworkUri)
            .build()

        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(mediaMetadata)
            .setUri("")
            .build()
    }

    fun getAlbums(prefix: String, type: String, size: Int, isRootCall: Boolean?): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
        val maxSize = size.coerceAtMost(ConstantsAA.MAX_ITEMS)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = subsonicRepository.getAlbumList2(type, maxSize, 0, null, null)
                val albums = response?.albumList2?.albums ?: emptyList()

                if ("alphabeticalByArtist" == type) {
                    albums.forEach { album ->
                        val artistName = album.artist
                        val albumName = album.name
                        album.name = artistName
                        album.artist = albumName
                    }
                }

                val mediaItems = albums.map { album ->
                    createAlbum(album.name, album.artist, album.genre, prefix + album.id, false, album.coverArtId)
                }.toMutableList()

                if (isRootCall == true) {
                    val jumpTo = createFunction(
                        App.getContext().getString(R.string.aa_starred_albums),
                        ConstantsAA.JUMP_TO_STARRED_ALBUMS_ID,
                        Preferences.isAndroidAutoAlbumViewEnabled(),
                        Uri.parse("android.resource://" + BuildConfig.APPLICATION_ID + "/" + R.drawable.ic_aa_star_album)
                    )
                    mediaItems.add(0, jumpTo)
                }

                listenableFuture.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null))
            } catch (e: Exception) {
                listenableFuture.setException(e)
            }
        }
        return listenableFuture
    }

    fun getArtists(prefix: String, isRootCall: Boolean?): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = subsonicRepository.getArtists()
                val indices = response?.artists?.indices ?: emptyList()
                val mediaItems = mutableListOf<MediaItem>()

                var count = 0
                for (index in indices) {
                    index.artists?.let { artists ->
                        for (artist in artists) {
                            if (count >= ConstantsAA.MAX_ITEMS) break
                            mediaItems.add(createArtist(artist.name, prefix + artist.id, Preferences.isAndroidAutoAlbumViewEnabled(), artist.coverArtId))
                            count++
                        }
                    }
                    if (count >= ConstantsAA.MAX_ITEMS) break
                }

                mediaItems.add(0, createFunction(
                    App.getContext().getString(R.string.aa_view_by_albums),
                    ConstantsAA.ARTISTS_BY_ALBUMS_ID,
                    Preferences.isAndroidAutoAlbumViewEnabled(),
                    Uri.parse("android.resource://" + BuildConfig.APPLICATION_ID + "/" + R.drawable.ic_aa_albums)
                ))

                if (isRootCall == true) {
                    mediaItems.add(0, createFunction(
                        App.getContext().getString(R.string.aa_starred_artists),
                        ConstantsAA.JUMP_TO_STARRED_ARTISTS_ID,
                        Preferences.isAndroidAutoAlbumViewEnabled(),
                        Uri.parse("android.resource://" + BuildConfig.APPLICATION_ID + "/" + R.drawable.ic_aa_artists)
                    ))
                }

                listenableFuture.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null))
            } catch (e: Exception) {
                listenableFuture.setException(e)
            }
        }
        return listenableFuture
    }

    fun getStarredSongs(): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = subsonicRepository.getStarred2()
                var songs = response?.starred2?.songs ?: emptyList()

                setChildrenMetadata(songs)

                songs = if (!Preferences.isAndroidAutoShuffleStarredTracksEnabled()) {
                    songs.take(ConstantsAA.MAX_ITEMS)
                } else {
                    songs.shuffled().take(ConstantsAA.MAX_SHUFFLE_ITEMS)
                }

                val mediaItems = MappingUtil.mapMediaItems(songs, ConstantsAA.QUEUE_CACHED_SOURCE)
                listenableFuture.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null))
            } catch (e: Exception) {
                listenableFuture.setException(e)
            }
        }
        return listenableFuture
    }

    fun getRandomSongs(count: Int): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = subsonicRepository.getRandomSongs(count, null, null, null)
                val songs = response?.randomSongs?.songs ?: emptyList()

                setChildrenMetadata(songs)

                val mediaItems = MappingUtil.mapMediaItems(songs, ConstantsAA.QUEUE_CACHED_SOURCE)
                listenableFuture.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null))
            } catch (e: Exception) {
                listenableFuture.setException(e)
            }
        }
        return listenableFuture
    }

    fun getRecentlyPlayedSongs(server: String, count: Int): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        CoroutineScope(Dispatchers.IO).launch {
            val chronology = chronologyDao.getLastPlayedSync(server, count)
            if (chronology.isNotEmpty()) {
                val songs = ArrayList<Child>(chronology)
                setChildrenMetadata(songs)
                val mediaItems = MappingUtil.mapMediaItems(songs, ConstantsAA.QUEUE_CACHED_SOURCE)
                listenableFuture.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null))
            } else {
                listenableFuture.set(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
            }
        }
        return listenableFuture
    }

    fun getStarredAlbums(prefix: String, isRootCall: Boolean?): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = subsonicRepository.getStarred2()
                val allAlbums = response?.starred2?.albums ?: emptyList()
                val albums = allAlbums.take(ConstantsAA.MAX_ITEMS)

                val mediaItems = albums.map { album ->
                    createAlbum(album.name, album.artist, album.genre, prefix + album.id, false, album.coverArtId)
                }.toMutableList()

                if (isRootCall == true) {
                    mediaItems.add(0, createFunction(
                        App.getContext().getString(R.string.aa_albums),
                        ConstantsAA.JUMP_TO_ALBUMS_ID,
                        Preferences.isAndroidAutoAlbumViewEnabled(),
                        Uri.parse("android.resource://" + BuildConfig.APPLICATION_ID + "/" + R.drawable.ic_aa_albums)
                    ))
                }

                listenableFuture.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null))
            } catch (e: Exception) {
                listenableFuture.setException(e)
            }
        }
        return listenableFuture
    }

    fun getStarredArtists(prefix: String, isRootCall: Boolean?): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = subsonicRepository.getStarred2()
                val allArtists = response?.starred2?.artists ?: emptyList()
                val artists = allArtists.take(ConstantsAA.MAX_ITEMS).sortedBy { it.name?.lowercase() }

                val mediaItems = artists.map { artist ->
                    createArtist(artist.name, prefix + artist.id, Preferences.isAndroidAutoAlbumViewEnabled(), artist.coverArtId)
                }.toMutableList()

                if (isRootCall == true) {
                    mediaItems.add(0, createFunction(
                        App.getContext().getString(R.string.aa_artists),
                        ConstantsAA.JUMP_TO_ARTISTS_ID,
                        Preferences.isAndroidAutoAlbumViewEnabled(),
                        Uri.parse("android.resource://" + BuildConfig.APPLICATION_ID + "/" + R.drawable.ic_aa_artists)
                    ))
                }

                listenableFuture.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null))
            } catch (e: Exception) {
                listenableFuture.setException(e)
            }
        }
        return listenableFuture
    }

    fun getMusicFolders(prefix: String): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = subsonicRepository.getMusicFolders()
                val musicFolders = response?.musicFolders?.musicFolders ?: emptyList()
                val artworkUri = Uri.parse("android.resource://" + BuildConfig.APPLICATION_ID + "/" + R.drawable.ic_aa_folders)

                val mediaItems = musicFolders.map { musicFolder ->
                    val mediaMetadata = MediaMetadata.Builder()
                        .setTitle(musicFolder.name)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .setArtworkUri(artworkUri)
                        .build()

                    MediaItem.Builder()
                        .setMediaId(prefix + musicFolder.id)
                        .setMediaMetadata(mediaMetadata)
                        .setUri("")
                        .build()
                }

                listenableFuture.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null))
            } catch (e: Exception) {
                listenableFuture.setException(e)
            }
        }
        return listenableFuture
    }

    fun getIndexes(prefix: String, id: String): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = subsonicRepository.getIndexes(id, null)
                val mediaItems = mutableListOf<MediaItem>()

                response?.indexes?.indices?.forEach { index ->
                    index.artists?.forEach { artist ->
                        val mediaMetadata = MediaMetadata.Builder()
                            .setTitle(artist.name)
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_ARTIST)
                            .build()

                        mediaItems.add(MediaItem.Builder()
                            .setMediaId(prefix + artist.id)
                            .setMediaMetadata(mediaMetadata)
                            .setUri("")
                            .build())
                    }
                }

                response?.indexes?.children?.let { children ->
                    children.forEach { song ->
                        val artworkUri = song.coverArtId?.let { AlbumArtContentProvider.contentUri(it) }
                        val mediaMetadata = MediaMetadata.Builder()
                            .setTitle(song.title)
                            .setAlbumTitle(song.album)
                            .setArtist(song.artist)
                            .setIsBrowsable(false)
                            .setIsPlayable(true)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                            .setArtworkUri(artworkUri)
                            .build()

                        mediaItems.add(MediaItem.Builder()
                            .setMediaId(prefix + song.id)
                            .setMediaMetadata(mediaMetadata)
                            .setUri(MusicUtil.getStreamUri(song.id))
                            .build())
                    }
                    setChildrenMetadata(children)
                }

                listenableFuture.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null))
            } catch (e: Exception) {
                listenableFuture.setException(e)
            }
        }
        return listenableFuture
    }

    fun getDirectories(prefix: String, id: String): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = subsonicRepository.getMusicDirectory(id)
                val directory = response?.directory
                val children = directory?.children ?: emptyList()

                val mediaItems = children.map { child ->
                    val artworkUri = child.coverArtId?.let { AlbumArtContentProvider.contentUri(it) }
                    val mediaMetadata = MediaMetadata.Builder()
                        .setTitle(child.title)
                        .setIsBrowsable(child.isDir)
                        .setIsPlayable(!child.isDir)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .setArtworkUri(artworkUri)
                        .build()

                    MediaItem.Builder()
                        .setMediaId(if (child.isDir) prefix + child.id else child.id)
                        .setMediaMetadata(mediaMetadata)
                        .setUri(if (!child.isDir) MusicUtil.getStreamUri(child.id) else Uri.parse(""))
                        .build()
                }

                setChildrenMetadata(children.filter { !it.isDir })

                listenableFuture.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null))
            } catch (e: Exception) {
                listenableFuture.setException(e)
            }
        }
        return listenableFuture
    }

    fun getPlaylists(prefix: String): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = subsonicRepository.getPlaylists()
                val allPlaylists = response?.playlists?.playlists ?: emptyList()
                val playlists = allPlaylists.take(ConstantsAA.MAX_ITEMS)

                val mediaItems = playlists.map { playlist ->
                    val artworkUri = if (!playlist.coverArtId.isNullOrEmpty()) AlbumArtContentProvider.contentUri(playlist.coverArtId)
                    else Uri.parse("android.resource://" + BuildConfig.APPLICATION_ID + "/" + R.drawable.ic_aa_playlist)

                    val mediaMetadata = MediaMetadata.Builder()
                        .setTitle(playlist.name)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                        .setArtworkUri(artworkUri)
                        .build()

                    MediaItem.Builder()
                        .setMediaId(prefix + playlist.id)
                        .setMediaMetadata(mediaMetadata)
                        .setUri("")
                        .build()
                }

                listenableFuture.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null))
            } catch (e: Exception) {
                listenableFuture.setException(e)
            }
        }
        return listenableFuture
    }

    fun getNewestPodcastEpisodes(count: Int): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = subsonicRepository.getNewestPodcasts(count)
                val episodes = response?.newestPodcasts?.episodes ?: emptyList()

                val mediaItems = episodes.map { episode ->
                    val artworkUri = episode.coverArtId?.let { AlbumArtContentProvider.contentUri(it) }
                    val mediaMetadata = MediaMetadata.Builder()
                        .setTitle(episode.title)
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE)
                        .setArtworkUri(artworkUri)
                        .build()

                    MediaItem.Builder()
                        .setMediaId(episode.id ?: "")
                        .setMediaMetadata(mediaMetadata)
                        .setUri(MusicUtil.getStreamUri(episode.streamId ?: ""))
                        .build()
                }

                setPodcastEpisodesMetadata(episodes)

                listenableFuture.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null))
            } catch (e: Exception) {
                listenableFuture.setException(e)
            }
        }
        return listenableFuture
    }

    fun getInternetRadioStations(): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = subsonicRepository.getInternetRadioStations()
                val radioStations = (response?.internetRadioStations?.internetRadioStations ?: emptyList()).toMutableList()

                val localCaches = AppDatabase.getInstance().internetRadioStationDao().getLocal()
                radioStations.addAll(localCaches.map { it.toInternetRadioStation() })

                radioStations.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name ?: "" })

                val mediaItems = radioStations.map { MappingUtil.mapInternetRadioStation(it) }

                setInternetRadioStationsMetadata(radioStations)

                listenableFuture.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null))
            } catch (e: Exception) {
                listenableFuture.setException(e)
            }
        }
        return listenableFuture
    }

    fun getAlbumTracks(id: String): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = subsonicRepository.getAlbum(id)
                val tracks = response?.album?.songs ?: emptyList()

                setChildrenMetadata(tracks)

                val mediaItems = MappingUtil.mapMediaItems(tracks, ConstantsAA.QUEUE_CACHED_SOURCE)
                listenableFuture.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null))
            } catch (e: Exception) {
                listenableFuture.setException(e)
            }
        }
        return listenableFuture
    }

    fun getArtistAlbum(prefix: String, id: String): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = subsonicRepository.getArtist(id)
                val artist = response?.artist
                val albums = artist?.albums ?: emptyList()

                var totalTracks = 0
                val mediaItems = albums.map { album ->
                    totalTracks += album.songCount ?: 0
                    createAlbum(album.name, album.artist, album.genre, prefix + album.id, false, album.coverArtId)
                }.toMutableList()

                if (albums.size >= 2 && totalTracks >= ConstantsAA.MIN_TRACKS_SMALL_MIX) {
                    val numberOfTracks = when {
                        totalTracks >= ConstantsAA.MIN_TRACKS_LARGE_MIX -> ConstantsAA.NUMBER_OF_TRACKS_IN_LARGE_MIX
                        totalTracks >= ConstantsAA.MIN_TRACKS_MEDIUM_MIX -> ConstantsAA.NUMBER_OF_TRACKS_IN_MEDIUM_MIX
                        else -> ConstantsAA.NUMBER_OF_TRACKS_IN_SMALL_MIX
                    }
                    val instantMixItem = createAlbum(
                        App.getContext().getString(R.string.aa_instant_mix),
                        "By Tempus",
                        "Instant Mix",
                        ConstantsAA.INSTANTMIX_SOURCE + "[" + numberOfTracks + "]" + id,
                        true,
                        artist?.coverArtId
                    )
                    mediaItems.add(0, instantMixItem)
                }

                listenableFuture.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null))
            } catch (e: Exception) {
                listenableFuture.setException(e)
            }
        }
        return listenableFuture
    }

    fun getInstantMix(artistId: String, count: Int): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        Log.d(TAG, "Instant Mix: Starting for artistId=$artistId for $count tracks")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = subsonicRepository.getArtist(artistId)
                val albums = response?.artist?.albums ?: emptyList()
                
                if (albums.isNotEmpty()) {
                    val firstAlbum = albums.shuffled().first()
                    val albumResponse = firstAlbum.id?.let { subsonicRepository.getAlbum(it) }
                    val songs = albumResponse?.album?.songs
                    
                    if (!songs.isNullOrEmpty()) {
                        val firstTrack = songs.shuffled().first()
                        setChildrenMetadata(listOf(firstTrack))
                        val mediaItem = MappingUtil.mapMediaItem(firstTrack, ConstantsAA.INSTANTMIX_SOURCE + "[" + count + "]" + artistId)
                        listenableFuture.set(LibraryResult.ofItemList(ImmutableList.of(mediaItem), null))
                        return@launch
                    }
                }
                listenableFuture.set(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
            } catch (e: Exception) {
                listenableFuture.setException(e)
            }
        }
        return listenableFuture
    }

    fun getPlaylistSongs(id: String): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = subsonicRepository.getPlaylist(id)
                var tracks = response?.playlist?.entries ?: emptyList()

                tracks = if (!Preferences.isAndroidAutoShufflePlaylistsEnabled()) {
                    tracks.take(ConstantsAA.MAX_ITEMS)
                } else {
                    tracks.shuffled().take(ConstantsAA.MAX_SHUFFLE_ITEMS)
                }

                setChildrenMetadata(tracks)
                val mediaItems = MappingUtil.mapMediaItems(tracks, ConstantsAA.QUEUE_CACHED_SOURCE)
                listenableFuture.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null))
            } catch (e: Exception) {
                listenableFuture.setException(e)
            }
        }
        return listenableFuture
    }

    fun getMadeForYou(mixType: String, count: Int): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = subsonicRepository.getAlbumList2("recent", ConstantsAA.NUMBER_OF_RECENT_ALBUMS_FOR_MIX, 0, null, null)
                val recentAlbums = response?.albumList2?.albums ?: emptyList()

                if (recentAlbums.isEmpty()) {
                    fallbackToFirstRandomSong(mixType, count, listenableFuture)
                    return@launch
                }

                val firstAlbum = recentAlbums.shuffled().first()
                val albumResponse = firstAlbum.id?.let { subsonicRepository.getAlbum(it) }
                val songs = albumResponse?.album?.songs

                if (!songs.isNullOrEmpty()) {
                    val firstTrack = songs.shuffled().first()
                    setChildrenMetadata(listOf(firstTrack))
                    val mediaItem = MappingUtil.mapMediaItem(firstTrack, ConstantsAA.MADE_FOR_YOU_SOURCE + "[" + count + "]" + mixType)
                    listenableFuture.set(LibraryResult.ofItemList(ImmutableList.of(mediaItem), null))
                } else {
                    fallbackToFirstRandomSong(mixType, count, listenableFuture)
                }
            } catch (e: Exception) {
                fallbackToFirstRandomSong(mixType, count, listenableFuture)
            }
        }
        return listenableFuture
    }

    private suspend fun fallbackToFirstRandomSong(mixType: String, count: Int, listenableFuture: SettableFuture<LibraryResult<ImmutableList<MediaItem>>>) {
        try {
            val response = subsonicRepository.getRandomSongs(1, null, null, null)
            val songs = response?.randomSongs?.songs ?: emptyList()
            if (songs.isNotEmpty()) {
                val firstTrack = songs[0]
                setChildrenMetadata(listOf(firstTrack))
                val mediaItem = MappingUtil.mapMediaItem(firstTrack, ConstantsAA.MADE_FOR_YOU_SOURCE + "[" + count + "]" + mixType)
                listenableFuture.set(LibraryResult.ofItemList(ImmutableList.of(mediaItem), null))
            } else {
                listenableFuture.set(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
            }
        } catch (e: Exception) {
            listenableFuture.setException(e)
        }
    }

    fun search(query: String, albumPrefix: String, artistPrefix: String): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = subsonicRepository.search3(query, 20, 0, 20, 0, 20, 0)
                val mediaItems = mutableListOf<MediaItem>()

                response?.searchResult3?.artists?.forEach { artist ->
                    mediaItems.add(createArtist(artist.name, artistPrefix + artist.id, Preferences.isAndroidAutoAlbumViewEnabled(), artist.coverArtId))
                }

                response?.searchResult3?.albums?.forEach { album ->
                    mediaItems.add(createAlbum(album.name, album.artist, album.genre, albumPrefix + album.id, false, album.coverArtId))
                }

                response?.searchResult3?.songs?.let { tracks ->
                    setChildrenMetadata(tracks)
                    mediaItems.addAll(MappingUtil.mapMediaItems(tracks))
                }

                listenableFuture.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null))
            } catch (e: Exception) {
                listenableFuture.setException(e)
            }
        }
        return listenableFuture
    }

    fun setChildrenMetadata(children: List<Child>) {
        val timestamp = System.currentTimeMillis()
        val sessionMediaItems = children.map { SessionMediaItem(it).apply { this.timestamp = timestamp } }
        CoroutineScope(Dispatchers.IO).launch {
            sessionMediaItemDao.insertAll(sessionMediaItems)
        }
    }

    fun setPodcastEpisodesMetadata(podcastEpisodes: List<PodcastEpisode>) {
        val timestamp = System.currentTimeMillis()
        val sessionMediaItems = podcastEpisodes.map { SessionMediaItem(it).apply { this.timestamp = timestamp } }
        CoroutineScope(Dispatchers.IO).launch {
            sessionMediaItemDao.insertAll(sessionMediaItems)
        }
    }

    fun setInternetRadioStationsMetadata(internetRadioStations: List<InternetRadioStation>) {
        val timestamp = System.currentTimeMillis()
        val sessionMediaItems = internetRadioStations.map { SessionMediaItem(it).apply { this.timestamp = timestamp } }
        CoroutineScope(Dispatchers.IO).launch {
            sessionMediaItemDao.insertAll(sessionMediaItems)
        }
    }

    fun getSessionMediaItem(id: String): SessionMediaItem? {
        return runBlocking(Dispatchers.IO) {
            sessionMediaItemDao.get(id)
        }
    }

    fun getMetadatas(timestamp: Long): List<MediaItem> {
        return runBlocking(Dispatchers.IO) {
            sessionMediaItemDao.get(timestamp).map { it.getMediaItem() }
        }
    }

    fun deleteMetadata() {
        CoroutineScope(Dispatchers.IO).launch {
            sessionMediaItemDao.deleteAll()
        }
    }

    fun getGenres(prefix: String): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = subsonicRepository.getGenres()
                val genres = (response?.genres?.genres ?: emptyList()).sortedBy { it.genre?.lowercase() }

                val mediaItems = genres.map { genre ->
                    val mediaMetadata = MediaMetadata.Builder()
                        .setTitle(genre.genre)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                        .build()

                    MediaItem.Builder()
                        .setMediaId(prefix + genre.genre)
                        .setMediaMetadata(mediaMetadata)
                        .setUri("")
                        .build()
                }

                listenableFuture.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null))
            } catch (e: Exception) {
                listenableFuture.setException(e)
            }
        }
        return listenableFuture
    }

    fun getSongsByGenre(genre: String, count: Int, shuffle: Boolean = false): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = if (shuffle) {
                    subsonicRepository.getRandomSongs(count, null, null, genre)
                } else {
                    subsonicRepository.getSongsByGenre(genre, count, 0)
                }

                val songs = if (shuffle) response?.randomSongs?.songs else response?.songsByGenre?.songs

                if (songs != null) {
                    setChildrenMetadata(songs)
                    val mediaItems = MappingUtil.mapMediaItems(songs, ConstantsAA.QUEUE_CACHED_SOURCE)
                    listenableFuture.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null))
                } else {
                    listenableFuture.set(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
                }
            } catch (e: Exception) {
                listenableFuture.setException(e)
            }
        }
        return listenableFuture
    }
}
