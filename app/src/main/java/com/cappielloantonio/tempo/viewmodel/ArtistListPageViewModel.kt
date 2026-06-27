package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.DownloadRepository
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArtistListPageArgs(
    val type: String,
)

data class ArtistListUiState(
    val artists: List<ArtistID3> = emptyList(),
    val isLoading: Boolean = true,
    val supportsSort: Boolean = false,
)

class ArtistListPageViewModel(
    private val artistRepository: ArtistRepository,
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtistListUiState())
    val uiState: StateFlow<ArtistListUiState> = _uiState.asStateFlow()

    private var startedType: String? = null

    fun onStart(args: ArtistListPageArgs) {
        if (startedType == args.type) return
        startedType = args.type

        _uiState.value = ArtistListUiState(
            artists = emptyList(),
            isLoading = true,
            supportsSort = true,
        )

        load(args.type)
    }

    private fun load(type: String) {
        viewModelScope.launch {
            when (type) {
                Constants.ARTIST_STARRED -> {
                    artistRepository.getStarredArtists(false, -1)
                        .asFlow()
                        .collectLatest { artists ->
                            updateArtists(artists ?: emptyList())
                        }
                }

                Constants.ARTIST_DOWNLOADED -> {
                    downloadRepository.getLiveDownload()
                        .asFlow()
                        .collectLatest { downloads ->
                            val artists = downloads
                                .map { it as Child }
                                .groupBy { it.artistId ?: it.artist.orEmpty() }
                                .mapNotNull { (_, songs) ->
                                    val first = songs.firstOrNull() ?: return@mapNotNull null
                                    ArtistID3(
                                        id = first.artistId,
                                        name = first.artist,
                                        coverArtId = first.coverArtId,
                                        albumCount = songs.map { it.albumId ?: it.album.orEmpty() }.distinct().size,
                                    )
                                }
                                .sortedBy { it.name?.lowercase().orEmpty() }
                            updateArtists(artists)
                        }
                }

                else -> {
                    artistRepository.getArtists(false, -1)
                        .asFlow()
                        .collectLatest { artists ->
                            updateArtists(artists ?: emptyList())
                        }
                }
            }
        }
    }

    private fun updateArtists(artists: List<ArtistID3>) {
        _uiState.update {
            it.copy(
                artists = artists,
                isLoading = false,
            )
        }
    }
}
