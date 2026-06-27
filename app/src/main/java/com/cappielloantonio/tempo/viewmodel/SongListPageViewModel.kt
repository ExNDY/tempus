package com.cappielloantonio.tempo.viewmodel

import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.ChronologyRepository
import com.cappielloantonio.tempo.repository.DownloadRepository
import com.cappielloantonio.tempo.repository.SongRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Genre
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.Preferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class SongListPageArgs(
    val type: String,
    val genre: Genre? = null,
    val artist: ArtistID3? = null,
    val album: AlbumID3? = null,
    val filters: List<String> = emptyList(),
    val filterNames: List<String> = emptyList(),
    val year: Int = 0,
)

data class SongListUiState(
    val title: String = "",
    val subtitle: String? = null,
    val songs: List<Child> = emptyList(),
    val isLoading: Boolean = true,
    val supportsSort: Boolean = false,
    val type: String = "",
)

class SongListPageViewModel(
    private val songRepository: SongRepository,
    private val artistRepository: ArtistRepository,
    private val albumRepository: AlbumRepository,
    private val downloadRepository: DownloadRepository,
    private val chronologyRepository: ChronologyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SongListUiState())
    val uiState: StateFlow<SongListUiState> = _uiState.asStateFlow()

    private var startedKey: String? = null
    private var loadJob: Job? = null

    fun onStart(args: SongListPageArgs) {
        val key = buildKey(args)
        if (startedKey == key) return
        startedKey = key

        _uiState.value = SongListUiState(
            title = resolveTitle(args),
            subtitle = null,
            songs = emptyList(),
            isLoading = true,
            supportsSort = supportsSort(args.type),
            type = args.type,
        )

        load(args)
    }

    private fun load(args: SongListPageArgs) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            when (args.type) {
                Constants.MEDIA_BY_GENRE -> {
                    songRepository.getRandomSampleWithGenre(500, 0, 3000, args.genre?.genre.orEmpty())
                        .asFlow()
                        .collectLatest { songs -> updateSongs(songs ?: emptyList(), args) }
                }

                Constants.MEDIA_BY_ARTIST -> {
                    artistRepository.getTopSongs(args.artist?.name.orEmpty(), 50)
                        .asFlow()
                        .collectLatest { songs -> updateSongs(songs ?: emptyList(), args) }
                }

                Constants.MEDIA_BY_GENRES -> {
                    songRepository.getSongsByGenres(ArrayList(args.filters))
                        .asFlow()
                        .collectLatest { songs -> updateSongs(songs ?: emptyList(), args) }
                }

                Constants.MEDIA_BY_YEAR -> {
                    songRepository.getRandomSample(500, args.year, args.year + 10)
                        .asFlow()
                        .collectLatest { songs -> updateSongs(songs ?: emptyList(), args) }
                }

                Constants.MEDIA_STARRED -> {
                    songRepository.getStarredSongs(false, -1)
                        .asFlow()
                        .collectLatest { songs -> updateSongs(songs ?: emptyList(), args) }
                }

                Constants.MEDIA_DOWNLOADED -> {
                    downloadRepository.getLiveDownload()
                        .asFlow()
                        .collectLatest { downloads ->
                            updateSongs(downloads.map { it as Child }, args)
                        }
                }

                Constants.MEDIA_FROM_ALBUM -> {
                    val albumId = args.album?.id.orEmpty()
                    if (albumId.isBlank()) {
                        updateSongs(emptyList(), args)
                    } else {
                        albumRepository.getAlbumTracks(albumId)
                            .asFlow()
                            .collectLatest { songs -> updateSongs(songs ?: emptyList(), args) }
                    }
                }

                Constants.MEDIA_RECENTLY_PLAYED -> {
                    chronologyRepository.getChronology(
                        Preferences.getServerId().orEmpty(),
                        0L,
                        System.currentTimeMillis(),
                    ).asFlow().collectLatest { history ->
                        val songs = history
                            .filterIsInstance<Child>()
                            .sortedByDescending { (it as? com.cappielloantonio.tempo.model.Chronology)?.timestamp ?: 0L }
                            .distinctBy { it.id }
                        updateSongs(songs, args)
                    }
                }

                Constants.MEDIA_MOST_PLAYED -> {
                    chronologyRepository.getChronology(
                        Preferences.getServerId().orEmpty(),
                        0L,
                        System.currentTimeMillis(),
                    ).asFlow().collectLatest { history ->
                        val songs = history
                            .filterIsInstance<Child>()
                            .groupBy { it.id }
                            .values
                            .sortedByDescending { it.size }
                            .mapNotNull { entries -> entries.firstOrNull() }
                        updateSongs(songs, args)
                    }
                }

                Constants.MEDIA_RECENTLY_ADDED -> {
                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                    songRepository.getRandomSample(500, 1900, currentYear + 1)
                        .asFlow()
                        .collectLatest { songs ->
                            updateSongs(
                                songs
                                    ?.sortedByDescending { it.created?.time ?: 0L }
                                    ?: emptyList(),
                                args,
                            )
                        }
                }

                else -> updateSongs(emptyList(), args)
            }
        }
    }

    private fun updateSongs(songs: List<Child>, args: SongListPageArgs) {
        _uiState.update {
            it.copy(
                songs = songs,
                subtitle = resolveSubtitle(args, songs),
                isLoading = false,
            )
        }
    }

    private fun resolveTitle(args: SongListPageArgs): String {
        return when (args.type) {
            Constants.MEDIA_RECENTLY_PLAYED -> App.getContext().getString(R.string.song_list_page_recently_played)
            Constants.MEDIA_MOST_PLAYED -> App.getContext().getString(R.string.song_list_page_most_played)
            Constants.MEDIA_RECENTLY_ADDED -> App.getContext().getString(R.string.song_list_page_recently_added)
            Constants.MEDIA_BY_GENRE -> args.genre?.genre.orEmpty()
            Constants.MEDIA_BY_ARTIST -> args.artist?.name?.let {
                App.getContext().getString(R.string.song_list_page_top, it)
            }.orEmpty()
            Constants.MEDIA_BY_GENRES -> args.filterNames.takeIf { it.isNotEmpty() }?.joinToString(", ")
                ?: args.filters.joinToString(", ")
            Constants.MEDIA_BY_YEAR -> App.getContext().getString(R.string.song_list_page_year, args.year)
            Constants.MEDIA_STARRED -> App.getContext().getString(R.string.song_list_page_starred)
            Constants.MEDIA_DOWNLOADED -> App.getContext().getString(R.string.song_list_page_downloaded)
            Constants.MEDIA_FROM_ALBUM -> args.album?.name.orEmpty()
            else -> ""
        }
    }

    private fun resolveSubtitle(args: SongListPageArgs, songs: List<Child>): String? {
        return when (args.type) {
            Constants.MEDIA_BY_GENRE,
            Constants.MEDIA_BY_ARTIST,
            Constants.MEDIA_BY_GENRES,
            Constants.MEDIA_BY_YEAR,
            Constants.MEDIA_STARRED,
            Constants.MEDIA_DOWNLOADED,
            Constants.MEDIA_FROM_ALBUM,
            Constants.MEDIA_RECENTLY_PLAYED,
            Constants.MEDIA_MOST_PLAYED,
            Constants.MEDIA_RECENTLY_ADDED -> "(${songs.size})"
            else -> null
        }
    }

    private fun supportsSort(type: String): Boolean {
        return type in setOf(
            Constants.MEDIA_BY_ARTIST,
            Constants.MEDIA_BY_GENRES,
            Constants.MEDIA_STARRED,
            Constants.MEDIA_DOWNLOADED,
            Constants.MEDIA_FROM_ALBUM,
            Constants.MEDIA_RECENTLY_PLAYED,
            Constants.MEDIA_MOST_PLAYED,
            Constants.MEDIA_RECENTLY_ADDED,
        )
    }

    private fun buildKey(args: SongListPageArgs): String {
        return listOf(
            args.type,
            args.genre?.genre.orEmpty(),
            args.artist?.id.orEmpty(),
            args.album?.id.orEmpty(),
            args.year.toString(),
            args.filters.joinToString(","),
            args.filterNames.joinToString(","),
        ).joinToString("|")
    }
}
