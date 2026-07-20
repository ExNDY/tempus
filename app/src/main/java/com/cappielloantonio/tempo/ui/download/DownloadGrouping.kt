package com.cappielloantonio.tempo.ui.download

import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.Constants

internal fun filterDownloadedSongs(
    groupType: String,
    groupValue: String?,
    songs: List<Child>,
): List<Child> {
    if (groupValue == null) return songs

    return when (groupType) {
        Constants.DOWNLOAD_TYPE_TRACK -> songs.filter { it.id == groupValue }
        Constants.DOWNLOAD_TYPE_ALBUM -> songs.filter { it.albumId == groupValue }
        Constants.DOWNLOAD_TYPE_ARTIST -> songs.filter { it.artistId == groupValue }
        Constants.DOWNLOAD_TYPE_GENRE -> songs.filter { it.genre == groupValue }
        Constants.DOWNLOAD_TYPE_YEAR -> songs.filter { it.year?.toString() == groupValue }
        Constants.DOWNLOAD_TYPE_PLAYLIST -> songs.filter {
            it is Download && it.playlistId == groupValue
        }

        else -> emptyList()
    }
}

internal fun downloadedGroupTitle(
    groupType: String,
    groupValue: String,
    songs: List<Child>,
): String {
    val first = songs.firstOrNull()
    return when (groupType) {
        Constants.DOWNLOAD_TYPE_ALBUM -> first?.album
        Constants.DOWNLOAD_TYPE_ARTIST -> first?.artist
        Constants.DOWNLOAD_TYPE_GENRE -> first?.genre
        Constants.DOWNLOAD_TYPE_YEAR -> first?.year?.toString()
        Constants.DOWNLOAD_TYPE_PLAYLIST -> (first as? Download)?.playlistName
        Constants.DOWNLOAD_TYPE_TRACK -> first?.title
        else -> null
    }.orEmpty().ifBlank { groupValue }
}

