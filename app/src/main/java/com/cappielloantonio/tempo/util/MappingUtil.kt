package com.cappielloantonio.tempo.util

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.HeartRating
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.provider.AlbumArtContentProvider
import com.cappielloantonio.tempo.repository.DownloadRepository
import com.cappielloantonio.tempo.subsonic.models.Child
import com.google.common.collect.ImmutableList
import java.util.Date

@androidx.media3.common.util.UnstableApi
object MappingUtil {
    private const val TAG = "MappingUtil"

    @JvmStatic
    fun mapMediaItems(items: List<Child>): List<MediaItem> {
        val mediaItems = ArrayList<MediaItem>()
        for (item in items) {
            mediaItems.add(mapMediaItem(item))
        }
        return mediaItems
    }

    @JvmStatic
    fun mapMediaItem(media: Child): MediaItem {
        try {
            val uri = getUri(media)
            val coverArtId = media.coverArtId
            var artworkUri: Uri? = null
            if (coverArtId != null) {
                artworkUri = AlbumArtContentProvider.contentUri(coverArtId)
            }
            val bundle = Bundle()
            bundle.putString("id", media.id)
            bundle.putString("parentId", media.parentId)
            bundle.putBoolean("isDir", media.isDir)
            bundle.putString("title", media.title)
            bundle.putString("album", media.album)
            bundle.putString("artist", media.artist)
            bundle.putInt("track", if (media.track != null) media.track!! else 0)
            bundle.putInt("year", if (media.year != null) media.year!! else 0)
            bundle.putString("genre", media.genre)
            bundle.putString("coverArtId", coverArtId)
            bundle.putLong("size", if (media.size != null) media.size!! else 0)
            bundle.putString("contentType", media.contentType)
            bundle.putString("suffix", media.suffix)
            bundle.putString("transcodedContentType", media.transcodedContentType)
            bundle.putString("transcodedSuffix", media.transcodedSuffix)
            bundle.putInt("duration", if (media.duration != null) media.duration!! else 0)
            bundle.putInt("bitrate", if (media.bitrate != null) media.bitrate!! else 0)
            bundle.putInt(
                "samplingRate",
                if (media.samplingRate != null) media.samplingRate!! else 0
            )
            bundle.putInt("bitDepth", if (media.bitDepth != null) media.bitDepth!! else 0)
            bundle.putString("path", media.path)
            bundle.putBoolean("isVideo", media.isVideo)
            bundle.putInt("userRating", if (media.userRating != null) media.userRating!! else 0)
            bundle.putDouble(
                "averageRating",
                if (media.averageRating != null) media.averageRating!! else 0.0
            )
            bundle.putLong("playCount", if (media.playCount != null) media.playCount!! else 0)
            bundle.putInt("discNumber", if (media.discNumber != null) media.discNumber!! else 0)
            bundle.putLong("created", media.created?.time ?: 0)
            bundle.putLong("starred", media.starred?.time ?: 0)
            bundle.putString("albumId", media.albumId)
            bundle.putString("artistId", media.artistId)
            bundle.putString("type", Constants.MEDIA_TYPE_MUSIC)
            bundle.putLong(
                "bookmarkPosition",
                if (media.bookmarkPosition != null) media.bookmarkPosition!! else 0
            )
            bundle.putInt(
                "originalWidth",
                if (media.originalWidth != null) media.originalWidth!! else 0
            )
            bundle.putInt(
                "originalHeight",
                if (media.originalHeight != null) media.originalHeight!! else 0
            )
            bundle.putString("uri", uri.toString())
            // Pack ReplayGain data from the server response so ReplayGainUtil
            // can apply gain synchronously at track transitions without a
            // MetadataRetriever round-trip.
            ReplayGainBundleUtil.writeToBundle(bundle, media.replayGain)
            bundle.putString(
                "assetLinkSong",
                AssetLinkUtil.buildLink(AssetLinkUtil.TYPE_SONG, media.id)
            )
            bundle.putString(
                "assetLinkAlbum",
                if (media.albumId != null) AssetLinkUtil.buildLink(
                    AssetLinkUtil.TYPE_ALBUM,
                    media.albumId
                ) else null
            )
            bundle.putString(
                "assetLinkArtist",
                if (media.artistId != null) AssetLinkUtil.buildLink(
                    AssetLinkUtil.TYPE_ARTIST,
                    media.artistId
                ) else null
            )
            bundle.putString(
                "assetLinkGenre",
                AssetLinkUtil.buildLink(AssetLinkUtil.TYPE_GENRE, media.genre)
            )
            val year = media.year
            bundle.putString(
                "assetLinkYear",
                if (year != null && year != 0) AssetLinkUtil.buildLink(
                    AssetLinkUtil.TYPE_YEAR,
                    year.toString()
                ) else null
            )
            return MediaItem.Builder()
                .setMediaId(media.id)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(media.title)
                        .setTrackNumber(if (media.track != null) media.track!! else 0)
                        .setDiscNumber(if (media.discNumber != null) media.discNumber!! else 0)
                        .setReleaseYear(if (media.year != null) media.year!! else 0)
                        .setAlbumTitle(media.album)
                        .setArtist(media.artist)
                        .setArtworkUri(artworkUri)
                        .setUserRating(HeartRating(media.starred != null && media.starred!!.time > 0))
                        .setSupportedCommands(
                            ImmutableList.of(
                                Constants.CUSTOM_COMMAND_TOGGLE_HEART_ON,
                                Constants.CUSTOM_COMMAND_TOGGLE_HEART_OFF
                            )
                        )
                        .setExtras(bundle)
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .build()
                )
                .setRequestMetadata(
                    MediaItem.RequestMetadata.Builder()
                        .setMediaUri(uri)
                        .setExtras(bundle)
                        .build()
                )
                .setMimeType(MimeTypes.BASE_TYPE_AUDIO)
                .setUri(uri)
                .build()
        } catch (e: Exception) {
            val id = media.id
            val title = media.title ?: "N/A"
            Log.e(
                TAG, "Instant Mix CRASH! Failed to map song to MediaItem. " +
                    "Problematic Song ID: " + id +
                    ", Title: " + title +
                    ". Inspect this song's Subsonic data for missing fields.", e
            )
            throw RuntimeException("Mapping failed for song ID: $id", e)
        }
    }

    @JvmStatic
    fun mapMediaItem(old: MediaItem): MediaItem {
        var mediaId: String? = null
        val extras = old.requestMetadata.extras
        if (extras != null) {
            mediaId = extras.getString("id")
        }
        if (mediaId != null && DownloadUtil.getDownloadTracker(App.getContext())
                .isDownloaded(mediaId)
        ) {
            return old
        }
        val uri =
            if (old.requestMetadata.mediaUri == null) null else MusicUtil.updateStreamUri(old.requestMetadata.mediaUri!!)
        return MediaItem.Builder()
            .setMediaId(old.mediaId)
            .setMediaMetadata(old.mediaMetadata)
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(uri)
                    .setExtras(old.requestMetadata.extras)
                    .build()
            )
            .setMimeType(MimeTypes.BASE_TYPE_AUDIO)
            .setUri(uri)
            .build()
    }

    @JvmStatic
    fun mapMediaItems(items: List<Child>, parentId: String): List<MediaItem> {
        val mediaItems = ArrayList<MediaItem>()
        for (item in items) {
            mediaItems.add(mapMediaItem(item, parentId))
        }
        return mediaItems
    }

    @JvmStatic
    fun mapMediaItem(item: Child, parentId: String): MediaItem {
        val mediaItem = mapMediaItem(item)
        val extras = if (mediaItem.mediaMetadata.extras != null) {
            Bundle(mediaItem.mediaMetadata.extras)
        } else {
            Bundle()
        }
        extras.putString("parent_id", parentId)
        val metadata = mediaItem.mediaMetadata
            .buildUpon()
            .setExtras(extras)
            .build()
        val requestMetadata = mediaItem.requestMetadata
            .buildUpon()
            .setExtras(extras)
            .build()
        return mediaItem.buildUpon()
            .setMediaId(item.id)
            .setMediaMetadata(metadata)
            .setRequestMetadata(requestMetadata)
            .build()
    }

    @JvmStatic
    fun mapDownloads(items: List<Child>): List<MediaItem> {
        val downloads = ArrayList<MediaItem>()
        for (item in items) {
            downloads.add(mapDownload(item))
        }
        return downloads
    }

    @JvmStatic
    fun mapDownload(media: Child): MediaItem {
        val bundle = Bundle()
        bundle.putInt("samplingRate", if (media.samplingRate != null) media.samplingRate!! else 0)
        bundle.putInt("bitDepth", if (media.bitDepth != null) media.bitDepth!! else 0)
        val uri =
            if (Preferences.preferTranscodedDownload()) MusicUtil.getTranscodedDownloadUri(media.id) else MusicUtil.getDownloadUri(
                media.id
            )
        return MediaItem.Builder()
            .setMediaId(media.id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(media.title)
                    .setTrackNumber(if (media.track != null) media.track!! else 0)
                    .setDiscNumber(if (media.discNumber != null) media.discNumber!! else 0)
                    .setReleaseYear(if (media.year != null) media.year!! else 0)
                    .setAlbumTitle(media.album)
                    .setArtist(media.artist)
                    .setExtras(bundle)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setExtras(bundle)
                    .setMediaUri(uri)
                    .build()
            )
            .setMimeType(MimeTypes.BASE_TYPE_AUDIO)
            .setUri(uri)
            .build()
    }

    @JvmStatic
    fun mapToChild(item: MediaItem?): Child? {
        if (item == null) return null
        val extras = item.mediaMetadata.extras
        val id =
            if (extras != null && extras.getString("id") != null) extras.getString("id") else item.mediaId
        if (id == null) return null
        val child = Child(id)
        if (extras != null) {
            child.parentId = extras.getString("parentId")
            child.isDir = extras.getBoolean("isDir")
            child.title = extras.getString("title")
            child.album = extras.getString("album")
            child.artist = extras.getString("artist")
            child.genre = extras.getString("genre")
            child.coverArtId = extras.getString("coverArtId")
            child.contentType = extras.getString("contentType")
            child.suffix = extras.getString("suffix")
            child.transcodedContentType = extras.getString("transcodedContentType")
            child.transcodedSuffix = extras.getString("transcodedSuffix")
            child.path = extras.getString("path")
            child.isVideo = extras.getBoolean("isVideo")
            child.albumId = extras.getString("albumId")
            child.artistId = extras.getString("artistId")
            child.type = extras.getString("type")
            if (extras.containsKey("track")) child.track = extras.getInt("track")
            if (extras.containsKey("year")) child.year = extras.getInt("year")
            if (extras.containsKey("size")) child.size = extras.getLong("size")
            if (extras.containsKey("duration")) child.duration = extras.getInt("duration")
            if (extras.containsKey("bitrate")) child.bitrate = extras.getInt("bitrate")
            if (extras.containsKey("samplingRate")) child.samplingRate =
                extras.getInt("samplingRate")
            if (extras.containsKey("bitDepth")) child.bitDepth = extras.getInt("bitDepth")
            if (extras.containsKey("userRating")) child.userRating = extras.getInt("userRating")
            if (extras.containsKey("averageRating")) child.averageRating =
                extras.getDouble("averageRating")
            if (extras.containsKey("playCount")) child.playCount = extras.getLong("playCount")
            if (extras.containsKey("discNumber")) child.discNumber = extras.getInt("discNumber")
            if (extras.containsKey("bookmarkPosition")) child.bookmarkPosition =
                extras.getLong("bookmarkPosition")
            if (extras.containsKey("originalWidth")) child.originalWidth =
                extras.getInt("originalWidth")
            if (extras.containsKey("originalHeight")) child.originalHeight =
                extras.getInt("originalHeight")
            val createdTime = extras.getLong("created", 0)
            if (createdTime != 0L) child.created = Date(createdTime)
            val starredTime = extras.getLong("starred", 0)
            if (starredTime != 0L) child.starred = Date(starredTime)
            // Rehydrate OpenSubsonic ReplayGain info if the bundle carries it.
            child.replayGain = ReplayGainBundleUtil.fromBundle(extras)
        }
        // Fallbacks
        if (child.title == null && item.mediaMetadata.title != null) {
            child.title = item.mediaMetadata.title.toString()
        }
        if (child.artist == null && item.mediaMetadata.artist != null) {
            child.artist = item.mediaMetadata.artist.toString()
        }
        if (child.album == null && item.mediaMetadata.albumTitle != null) {
            child.album = item.mediaMetadata.albumTitle.toString()
        }
        if (child.track == null && item.mediaMetadata.trackNumber != null) {
            child.track = item.mediaMetadata.trackNumber
        }
        if (child.discNumber == null && item.mediaMetadata.discNumber != null) {
            child.discNumber = item.mediaMetadata.discNumber
        }
        if (child.year == null && item.mediaMetadata.releaseYear != null) {
            child.year = item.mediaMetadata.releaseYear
        }
        return child
    }

    private fun getUri(media: Child): Uri {
        // Check if it's in our local SQL Database
        val repo = DownloadRepository()
        val localDownload = repo.getDownload(media.id)
        if (localDownload != null && !localDownload.downloadUri.isNullOrEmpty()) {
            Log.d(TAG, "Playing local file for: " + media.title)
            return Uri.parse(localDownload.downloadUri)
        }
        // Legacy check for external directory, i think this was broken/buggy
        if (Preferences.getDownloadDirectoryUri() != null) {
            val local = ExternalAudioReader.getUri(media)
            if (local != null) return local
        }
        // Fallback to streaming
        Log.d(TAG, "No local file found. Streaming: " + media.title)
        return MusicUtil.getStreamUri(media.id)
    }

    private fun getDownloadUri(id: String): Uri {
        val download = DownloadRepository().getDownload(id)
        return if (download != null && !download.downloadUri.isNullOrEmpty()) {
            Uri.parse(download.downloadUri)
        } else {
            MusicUtil.getDownloadUri(id)
        }
    }

    @JvmStatic
    fun observeExternalAudioRefresh(owner: LifecycleOwner?, onRefresh: Runnable?) {
        if (owner == null || onRefresh == null) {
            return
        }
        ExternalAudioReader.getRefreshEvents().observe(owner) { onRefresh.run() }
    }
}
