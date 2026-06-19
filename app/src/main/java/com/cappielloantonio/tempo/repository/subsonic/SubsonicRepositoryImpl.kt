package com.cappielloantonio.tempo.repository.subsonic

import com.cappielloantonio.tempo.subsonic.models.SubsonicResponse
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.subsonic.utils.StringUtil
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.util.UUID

class SubsonicRepositoryImpl(
    private val httpClient: HttpClient,
) : SubsonicRepository {

    private fun getBaseUrl(): String {
        var url = Preferences.getInUseServerAddress() ?: "http://localhost"
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        return if (url.endsWith("/")) "${url}rest" else "$url/rest"
    }

    private fun HttpRequestBuilder.appendSubsonicParams() {
        parameter("u", Preferences.getUser())
        
        val password = Preferences.getPassword()
        if (Preferences.isLowSecurity()) {
            parameter("p", password)
        } else {
            val salt = UUID.randomUUID().toString()
            val token = StringUtil.tokenize(password + salt)
            parameter("t", token)
            parameter("s", salt)
        }
        
        parameter("v", "1.16.1") // API version
        parameter("c", "Tempus")
        parameter("f", "json")
    }

    private suspend fun safeGet(endpoint: String, block: HttpRequestBuilder.() -> Unit = {}): SubsonicResponse? {
        return try {
            val response: ApiResponse = httpClient.get("${getBaseUrl()}/$endpoint") {
                appendSubsonicParams()
                block()
            }.body()
            response.subsonicResponse
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // AuthSource
    override suspend fun ping() = safeGet("ping.view")
    override suspend fun getLicense() = safeGet("getLicense.view")
    override suspend fun getOpenSubsonicExtensions() = safeGet("getOpenSubsonicExtensions.view")

    // BrowsingSource
    override suspend fun getMusicFolders() = safeGet("getMusicFolders.view")
    override suspend fun getIndexes(musicFolderId: String?, ifModifiedSince: Long?) = safeGet("getIndexes.view") {
        musicFolderId?.let { parameter("musicFolderId", it) }
        ifModifiedSince?.let { parameter("ifModifiedSince", it) }
    }
    override suspend fun getMusicDirectory(id: String) = safeGet("getMusicDirectory.view") { parameter("id", id) }
    override suspend fun getGenres() = safeGet("getGenres.view")
    override suspend fun getArtists() = safeGet("getArtists.view")
    override suspend fun getArtist(id: String) = safeGet("getArtist.view") { parameter("id", id) }
    override suspend fun getAlbum(id: String) = safeGet("getAlbum.view") { parameter("id", id) }
    override suspend fun getSong(id: String) = safeGet("getSong.view") { parameter("id", id) }
    override suspend fun getVideos() = safeGet("getVideos.view")
    override suspend fun getVideoInfo(id: String) = safeGet("getVideoInfo.view") { parameter("id", id) }
    override suspend fun getArtistInfo(id: String) = safeGet("getArtistInfo.view") { parameter("id", id) }
    override suspend fun getArtistInfo2(id: String) = safeGet("getArtistInfo2.view") { parameter("id", id) }
    override suspend fun getAlbumInfo(id: String) = safeGet("getAlbumInfo.view") { parameter("id", id) }
    override suspend fun getAlbumInfo2(id: String) = safeGet("getAlbumInfo2.view") { parameter("id", id) }
    override suspend fun getSimilarSongs(id: String, count: Int) = safeGet("getSimilarSongs.view") {
        parameter("id", id)
        parameter("count", count)
    }
    override suspend fun getSimilarSongs2(id: String, limit: Int) = safeGet("getSimilarSongs2.view") {
        parameter("id", id)
        parameter("count", limit)
    }
    override suspend fun getTopSongs(artist: String, count: Int) = safeGet("getTopSongs.view") {
        parameter("artist", artist)
        parameter("count", count)
    }

    // AlbumSongListSource
    override suspend fun getAlbumList(type: String, size: Int, offset: Int) = safeGet("getAlbumList.view") {
        parameter("type", type)
        parameter("size", size)
        parameter("offset", offset)
    }
    override suspend fun getAlbumList2(type: String, size: Int, offset: Int, fromYear: Int?, toYear: Int?) = safeGet("getAlbumList2.view") {
        parameter("type", type)
        parameter("size", size)
        parameter("offset", offset)
        fromYear?.let { parameter("fromYear", it) }
        toYear?.let { parameter("toYear", it) }
    }
    override suspend fun getRandomSongs(size: Int, fromYear: Int?, toYear: Int?, genre: String?) = safeGet("getRandomSongs.view") {
        parameter("size", size)
        fromYear?.let { parameter("fromYear", it) }
        toYear?.let { parameter("toYear", it) }
        genre?.let { parameter("genre", it) }
    }
    override suspend fun getSongsByGenre(genre: String, count: Int, offset: Int) = safeGet("getSongsByGenre.view") {
        parameter("genre", genre)
        parameter("count", count)
        parameter("offset", offset)
    }
    override suspend fun getNowPlaying() = safeGet("getNowPlaying.view")
    override suspend fun getStarred() = safeGet("getStarred.view")
    override suspend fun getStarred2() = safeGet("getStarred2.view")

    // MediaSource
    override suspend fun stream(id: String, maxBitRate: Int?, format: String?) = safeGet("stream.view") {
        parameter("id", id)
        maxBitRate?.let { parameter("maxBitRate", it) }
        format?.let { parameter("format", it) }
    }
    override suspend fun download(id: String) = safeGet("download.view") { parameter("id", id) }
    override suspend fun getLyrics(artist: String, title: String) = safeGet("getLyrics.view") {
        parameter("artist", artist)
        parameter("title", title)
    }
    override suspend fun getLyricsBySongId(id: String) = safeGet("getLyricsBySongId.view") { parameter("id", id) }
    override suspend fun star(id: String?, albumId: String?, artistId: String?) = safeGet("star.view") {
        id?.let { parameter("id", it) }
        albumId?.let { parameter("albumId", it) }
        artistId?.let { parameter("artistId", it) }
    }
    override suspend fun unstar(id: String?, albumId: String?, artistId: String?) = safeGet("unstar.view") {
        id?.let { parameter("id", it) }
        albumId?.let { parameter("albumId", it) }
        artistId?.let { parameter("artistId", it) }
    }
    override suspend fun setRating(id: String, rating: Int) = safeGet("setRating.view") {
        parameter("id", id)
        parameter("rating", rating)
    }
    override suspend fun scrobble(id: String, submission: Boolean?) = safeGet("scrobble.view") {
        parameter("id", id)
        submission?.let { parameter("submission", it) }
    }

    // PlaylistSource
    override suspend fun getPlaylists() = safeGet("getPlaylists.view")
    override suspend fun getPlaylist(id: String) = safeGet("getPlaylist.view") { parameter("id", id) }
    override suspend fun createPlaylist(playlistId: String?, name: String?, songsId: List<String>) = safeGet("createPlaylist.view") {
        playlistId?.let { parameter("playlistId", it) }
        name?.let { parameter("name", it) }
        songsId.forEach { parameter("songId", it) }
    }
    override suspend fun updatePlaylist(playlistId: String, name: String?, isPublic: Boolean?, songIdToAdd: List<String>?, songIndexToRemove: List<Int>?) = safeGet("updatePlaylist.view") {
        parameter("playlistId", playlistId)
        name?.let { parameter("name", it) }
        isPublic?.let { parameter("public", it) }
        songIdToAdd?.forEach { parameter("songIdToAdd", it) }
        songIndexToRemove?.forEach { parameter("songIndexToRemove", it) }
    }
    override suspend fun deletePlaylist(id: String) = safeGet("deletePlaylist.view") { parameter("id", id) }

    // SearchSource
    override suspend fun search2(query: String, songCount: Int, albumCount: Int, artistCount: Int) = safeGet("search2.view") {
        parameter("query", query)
        parameter("songCount", songCount)
        parameter("albumCount", albumCount)
        parameter("artistCount", artistCount)
    }
    override suspend fun search3(query: String, songCount: Int, songOffset: Int, albumCount: Int, albumOffset: Int, artistCount: Int, artistOffset: Int) = safeGet("search3.view") {
        parameter("query", query)
        parameter("songCount", songCount)
        parameter("songOffset", songOffset)
        parameter("albumCount", albumCount)
        parameter("albumOffset", albumOffset)
        parameter("artistCount", artistCount)
        parameter("artistOffset", artistOffset)
    }

    // BookmarkSource
    override suspend fun getPlayQueue() = safeGet("getPlayQueue.view")
    override suspend fun savePlayQueue(ids: List<String>, current: String?, position: Long?) = safeGet("savePlayQueue.view") {
        ids.forEach { parameter("id", it) }
        current?.let { parameter("current", it) }
        position?.let { parameter("position", it) }
    }

    // MediaLibraryScanningSource
    override suspend fun startScan() = safeGet("startScan.view")
    override suspend fun getScanStatus() = safeGet("getScanStatus.view")

    // InternetRadioSource
    override suspend fun getInternetRadioStations() = safeGet("getInternetRadioStations.view")
    override suspend fun createInternetRadioStation(streamUrl: String, name: String, homepageUrl: String?) = safeGet("createInternetRadioStation.view") {
        parameter("streamUrl", streamUrl)
        parameter("name", name)
        homepageUrl?.let { parameter("homepageUrl", it) }
    }
    override suspend fun updateInternetRadioStation(id: String, streamUrl: String, name: String, homepageUrl: String?) = safeGet("updateInternetRadioStation.view") {
        parameter("id", id)
        parameter("streamUrl", streamUrl)
        parameter("name", name)
        homepageUrl?.let { parameter("homepageUrl", it) }
    }
    override suspend fun deleteInternetRadioStation(id: String) = safeGet("deleteInternetRadioStation.view") { parameter("id", id) }

    // SharingSource
    override suspend fun getShares() = safeGet("getShares.view")
    override suspend fun createShare(id: String, description: String?, expires: Long?) = safeGet("createShare.view") {
        parameter("id", id)
        description?.let { parameter("description", it) }
        expires?.let { parameter("expires", it) }
    }
    override suspend fun updateShare(id: String, description: String?, expires: Long?) = safeGet("updateShare.view") {
        parameter("id", id)
        description?.let { parameter("description", it) }
        expires?.let { parameter("expires", it) }
    }
    override suspend fun deleteShare(id: String) = safeGet("deleteShare.view") { parameter("id", id) }

    // PodcastSource
    override suspend fun getPodcasts(includeEpisodes: Boolean, id: String?) = safeGet("getPodcasts.view") {
        parameter("includeEpisodes", includeEpisodes)
        id?.let { parameter("id", it) }
    }
    override suspend fun getNewestPodcasts(count: Int) = safeGet("getNewestPodcasts.view") { parameter("count", count) }
    override suspend fun refreshPodcasts() = safeGet("refreshPodcasts.view")
    override suspend fun createPodcastChannel(url: String) = safeGet("createPodcastChannel.view") { parameter("url", url) }
    override suspend fun deletePodcastChannel(id: String) = safeGet("deletePodcastChannel.view") { parameter("id", id) }
    override suspend fun deletePodcastEpisode(id: String) = safeGet("deletePodcastEpisode.view") { parameter("id", id) }
    override suspend fun downloadPodcastEpisode(id: String) = safeGet("downloadPodcastEpisode.view") { parameter("id", id) }
}
