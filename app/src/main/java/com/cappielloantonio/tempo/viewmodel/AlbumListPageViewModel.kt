package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.repository.DownloadRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class AlbumListPageArgs(
    val type: String,
    val artist: ArtistID3? = null,
    val albums: List<AlbumID3> = emptyList(),
    val listTitle: String? = null,
)

data class AlbumListUiState(
    val albums: List<AlbumID3> = emptyList(),
    val isLoading: Boolean = true,
    val supportsSort: Boolean = false,
    val preferListLayout: Boolean = false,
)

class AlbumListPageViewModel(
    private val albumRepository: AlbumRepository,
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumListUiState())
    val uiState: StateFlow<AlbumListUiState> = _uiState.asStateFlow()

    private var startedKey: String? = null

    fun onStart(args: AlbumListPageArgs) {
        val key = buildKey(args)
        if (startedKey == key) return
        startedKey = key

        _uiState.value = AlbumListUiState(
            albums = emptyList(),
            isLoading = true,
            supportsSort = args.type == Constants.ALBUM_STARRED,
            preferListLayout = args.type == Constants.ALBUM_DOWNLOADED || args.type == Constants.ALBUM_FROM_ARTIST,
        )

        load(args)
    }

    private fun load(args: AlbumListPageArgs) {
        viewModelScope.launch {
            when (args.type) {
                Constants.ALBUM_RECENTLY_PLAYED -> {
                    albumRepository.getAlbums("recent", 500, null, null)
                        .asFlow()
                        .collectLatest { updateAlbums(it ?: emptyList()) }
                }

                Constants.ALBUM_MOST_PLAYED -> {
                    albumRepository.getAlbums("frequent", 500, null, null)
                        .asFlow()
                        .collectLatest { updateAlbums(it ?: emptyList()) }
                }

                Constants.ALBUM_RECENTLY_ADDED -> {
                    albumRepository.getAlbums("newest", 500, null, null)
                        .asFlow()
                        .collectLatest { updateAlbums(it ?: emptyList()) }
                }

                Constants.ALBUM_STARRED -> {
                    albumRepository.getStarredAlbums(false, -1)
                        .asFlow()
                        .collectLatest { updateAlbums(it ?: emptyList()) }
                }

                Constants.ALBUM_NEW_RELEASES -> {
                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                    albumRepository.getAlbums("byYear", 500, currentYear, currentYear)
                        .asFlow()
                        .collectLatest { albums ->
                            updateAlbums(albums?.sortedByDescending { it.created }?.take(20) ?: emptyList())
                        }
                }

                Constants.ALBUM_DOWNLOADED -> {
                    downloadRepository.getLiveDownload()
                        .asFlow()
                        .collectLatest { downloads ->
                            val groupedAlbums = downloads
                                .map { it as Child }
                                .groupBy { it.albumId }
                                .map { (_, albumSongs) ->
                                    val first = albumSongs.first()
                                    AlbumID3(
                                        id = first.albumId,
                                        name = first.album,
                                        artist = first.artist,
                                        artistId = first.artistId,
                                        coverArtId = first.coverArtId,
                                        songCount = albumSongs.size,
                                        duration = albumSongs.sumOf { it.duration ?: 0 },
                                    )
                                }
                            updateAlbums(groupedAlbums)
                        }
                }

                Constants.ALBUM_FROM_ARTIST -> {
                    val artistId = args.artist?.id.orEmpty()
                    if (artistId.isBlank()) {
                        updateAlbums(emptyList())
                    } else {
                        albumRepository.getArtistAlbums(artistId)
                            .asFlow()
                            .collectLatest { updateAlbums(it ?: emptyList()) }
                    }
                }

                else -> {
                    updateAlbums(args.albums)
                }
            }
        }
    }

    private fun updateAlbums(albums: List<AlbumID3>) {
        _uiState.update {
            it.copy(
                albums = albums,
                isLoading = false,
            )
        }
    }

    private fun buildKey(args: AlbumListPageArgs): String {
        return listOf(
            args.type,
            args.artist?.id.orEmpty(),
            args.listTitle.orEmpty(),
            args.albums.joinToString(",") { it.id.orEmpty() },
        ).joinToString("|")
    }
}
