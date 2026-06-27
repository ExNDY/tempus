package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.cappielloantonio.tempo.subsonic.models.Child

data class DownloadedBottomSheetArgs(
    val songs: List<Child>,
    val title: String,
    val subtitle: String,
)

data class DownloadedBottomSheetUiState(
    val songs: List<Child> = emptyList(),
    val title: String = "",
    val subtitle: String = "",
) {
    val hasMultipleSongs: Boolean
        get() = songs.size > 1
}

class DownloadedBottomSheetViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadedBottomSheetUiState())
    val uiState = _uiState.asStateFlow()

    private var startedKey: String? = null

    fun onStart(args: DownloadedBottomSheetArgs) {
        val key = buildString {
            append(args.title)
            append('|')
            append(args.subtitle)
            append('|')
            append(args.songs.joinToString(",") { it.id })
        }

        if (startedKey == key) return
        startedKey = key

        _uiState.update {
            it.copy(
                songs = args.songs,
                title = args.title,
                subtitle = args.subtitle,
            )
        }
    }
}
