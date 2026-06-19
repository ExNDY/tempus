package com.cappielloantonio.tempo.repository.subsonic

import com.cappielloantonio.tempo.subsonic.models.SubsonicResponse

interface AuthSource {
    suspend fun ping(): SubsonicResponse?
    suspend fun getLicense(): SubsonicResponse?
    suspend fun getOpenSubsonicExtensions(): SubsonicResponse?
}

interface BrowsingSource {
    suspend fun getMusicFolders(): SubsonicResponse?
    suspend fun getIndexes(musicFolderId: String?, ifModifiedSince: Long?): SubsonicResponse?
    suspend fun getMusicDirectory(id: String): SubsonicResponse?
    suspend fun getGenres(): SubsonicResponse?
    suspend fun getArtists(): SubsonicResponse?
    suspend fun getArtist(id: String): SubsonicResponse?
    suspend fun getAlbum(id: String): SubsonicResponse?
    suspend fun getSong(id: String): SubsonicResponse?
    suspend fun getVideos(): SubsonicResponse?
    suspend fun getVideoInfo(id: String): SubsonicResponse?
    suspend fun getArtistInfo(id: String): SubsonicResponse?
    suspend fun getArtistInfo2(id: String): SubsonicResponse?
    suspend fun getAlbumInfo(id: String): SubsonicResponse?
    suspend fun getAlbumInfo2(id: String): SubsonicResponse?
    suspend fun getSimilarSongs(id: String, count: Int): SubsonicResponse?
    suspend fun getSimilarSongs2(id: String, limit: Int): SubsonicResponse?
    suspend fun getTopSongs(artist: String, count: Int): SubsonicResponse?
}

interface AlbumSongListSource {
    suspend fun getAlbumList(type: String, size: Int, offset: Int): SubsonicResponse?
    suspend fun getAlbumList2(type: String, size: Int, offset: Int, fromYear: Int?, toYear: Int?): SubsonicResponse?
    suspend fun getRandomSongs(size: Int, fromYear: Int?, toYear: Int?, genre: String?): SubsonicResponse?
    suspend fun getSongsByGenre(genre: String, count: Int, offset: Int): SubsonicResponse?
    suspend fun getNowPlaying(): SubsonicResponse?
    suspend fun getStarred(): SubsonicResponse?
    suspend fun getStarred2(): SubsonicResponse?
}

interface MediaSource {
    suspend fun stream(id: String, maxBitRate: Int?, format: String?): SubsonicResponse?
    suspend fun download(id: String): SubsonicResponse?
    suspend fun getLyrics(artist: String, title: String): SubsonicResponse?
    suspend fun getLyricsBySongId(id: String): SubsonicResponse?
    suspend fun star(id: String?, albumId: String?, artistId: String?): SubsonicResponse?
    suspend fun unstar(id: String?, albumId: String?, artistId: String?): SubsonicResponse?
    suspend fun setRating(id: String, rating: Int): SubsonicResponse?
    suspend fun scrobble(id: String, submission: Boolean?): SubsonicResponse?
}

interface PlaylistSource {
    suspend fun getPlaylists(): SubsonicResponse?
    suspend fun getPlaylist(id: String): SubsonicResponse?
    suspend fun createPlaylist(playlistId: String?, name: String?, songsId: List<String>): SubsonicResponse?
    suspend fun updatePlaylist(playlistId: String, name: String?, isPublic: Boolean?, songIdToAdd: List<String>?, songIndexToRemove: List<Int>?): SubsonicResponse?
    suspend fun deletePlaylist(id: String): SubsonicResponse?
}

interface SearchSource {
    suspend fun search2(query: String, songCount: Int, albumCount: Int, artistCount: Int): SubsonicResponse?
    suspend fun search3(query: String, songCount: Int, songOffset: Int, albumCount: Int, albumOffset: Int, artistCount: Int, artistOffset: Int): SubsonicResponse?
}

interface BookmarkSource {
    suspend fun getPlayQueue(): SubsonicResponse?
    suspend fun savePlayQueue(ids: List<String>, current: String?, position: Long?): SubsonicResponse?
}

interface MediaLibraryScanningSource {
    suspend fun startScan(): SubsonicResponse?
    suspend fun getScanStatus(): SubsonicResponse?
}

interface InternetRadioSource {
    suspend fun getInternetRadioStations(): SubsonicResponse?
    suspend fun createInternetRadioStation(streamUrl: String, name: String, homepageUrl: String?): SubsonicResponse?
    suspend fun updateInternetRadioStation(id: String, streamUrl: String, name: String, homepageUrl: String?): SubsonicResponse?
    suspend fun deleteInternetRadioStation(id: String): SubsonicResponse?
}

interface SharingSource {
    suspend fun getShares(): SubsonicResponse?
    suspend fun createShare(id: String, description: String?, expires: Long?): SubsonicResponse?
    suspend fun updateShare(id: String, description: String?, expires: Long?): SubsonicResponse?
    suspend fun deleteShare(id: String): SubsonicResponse?
}

interface PodcastSource {
    suspend fun getPodcasts(includeEpisodes: Boolean, id: String?): SubsonicResponse?
    suspend fun getNewestPodcasts(count: Int): SubsonicResponse?
    suspend fun refreshPodcasts(): SubsonicResponse?
    suspend fun createPodcastChannel(url: String): SubsonicResponse?
    suspend fun deletePodcastChannel(id: String): SubsonicResponse?
    suspend fun deletePodcastEpisode(id: String): SubsonicResponse?
    suspend fun downloadPodcastEpisode(id: String): SubsonicResponse?
}

interface SubsonicRepository : AuthSource, BrowsingSource, AlbumSongListSource, MediaSource,
    PlaylistSource, SearchSource, BookmarkSource, MediaLibraryScanningSource,
    InternetRadioSource, SharingSource, PodcastSource
