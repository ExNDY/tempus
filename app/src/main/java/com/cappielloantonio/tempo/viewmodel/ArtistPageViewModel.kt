package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.asFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.interfaces.StarCallback
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.FavoriteRepository
import com.cappielloantonio.tempo.subsonic.models.*
import com.cappielloantonio.tempo.util.NetworkUtil
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume

data class ArtistPageUiState(
    val artist: ArtistID3? = null,
    val artistInfo: ArtistInfo2? = null,
    val topSongs: List<Child> = emptyList(),
    val albums: Map<String, List<AlbumID3>> = emptyMap(),
    val isLoading: Boolean = true
)

@UnstableApi
class ArtistPageViewModel(
    private val artistRepository: ArtistRepository,
    private val albumRepository: AlbumRepository,
    private val favoriteRepository: FavoriteRepository,
) : ViewModel() {
}
