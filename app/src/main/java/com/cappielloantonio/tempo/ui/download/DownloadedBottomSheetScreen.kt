package com.cappielloantonio.tempo.ui.download

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getDownloadedBottomSheetViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.components.ActionBottomSheetContent
import com.cappielloantonio.tempo.ui.components.ActionItemUiModel
import com.cappielloantonio.tempo.ui.components.BottomSheetErrorContent
import com.cappielloantonio.tempo.ui.components.BottomSheetLoadingContent
import com.cappielloantonio.tempo.ui.components.TempusImageType
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.ExternalAudioReader
import com.cappielloantonio.tempo.util.MappingUtil
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.viewmodel.DownloadedBottomSheetUiState
import com.cappielloantonio.tempo.viewmodel.DownloadedBottomSheetViewModel

@Composable
fun DownloadedBottomSheetRoute(
    groupType: String,
    groupValue: String,
    onDismiss: () -> Unit,
    onShufflePlay: (List<Child>) -> Unit,
    onPlayNext: (List<Child>) -> Unit,
    onAddToQueue: (List<Child>) -> Unit,
) {
    val context = LocalContext.current
    val viewModel: DownloadedBottomSheetViewModel = getViewModel {
        getDownloadedBottomSheetViewModel()
    }
    LaunchedEffect(groupType, groupValue) {
        viewModel.onStart(groupType, groupValue)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> {
            BottomSheetLoadingContent()
            return
        }
        uiState.hasError -> {
            BottomSheetErrorContent(
                onRetry = { viewModel.retry(groupType, groupValue) },
                onClose = onDismiss,
            )
            return
        }
    }

    DownloadedBottomSheetContent(
        uiState = uiState,
        onShuffleClick = {
            onShufflePlay(uiState.songs.shuffled())
            onDismiss()
        },
        onPlayNextClick = {
            onPlayNext(uiState.songs)
            onDismiss()
        },
        onAddToQueueClick = {
            onAddToQueue(uiState.songs)
            onDismiss()
        },
        onRemoveClick = {
            removeDownloads(context, uiState.songs)
            onDismiss()
        },
    )
}

@Composable
fun DownloadedBottomSheetContent(
    uiState: DownloadedBottomSheetUiState,
    onShuffleClick: () -> Unit,
    onPlayNextClick: () -> Unit,
    onAddToQueueClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    ActionBottomSheetContent(
        title = uiState.title,
        subtitle = stringResource(
            R.string.download_item_single_subtitle_formatter,
            uiState.songs.size,
        ),
        coverArtId = uiState.songs.firstOrNull()?.coverArtId,
        imageType = TempusImageType.Song,
        actions = listOf(
            ActionItemUiModel(
                icon = Icons.Default.Shuffle,
                label = stringResource(R.string.downloaded_bottom_sheet_shuffle),
                isVisible = uiState.hasMultipleSongs,
                onClick = onShuffleClick,
            ),
            ActionItemUiModel(
                icon = Icons.Default.PlayArrow,
                label = stringResource(R.string.downloaded_bottom_sheet_play_next),
                onClick = onPlayNextClick,
            ),
            ActionItemUiModel(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                label = stringResource(R.string.downloaded_bottom_sheet_add_to_queue),
                onClick = onAddToQueueClick,
            ),
            ActionItemUiModel(
                icon = Icons.Default.Delete,
                label = stringResource(
                    if (uiState.hasMultipleSongs) {
                        R.string.downloaded_bottom_sheet_remove_all
                    } else {
                        R.string.downloaded_bottom_sheet_remove
                    }
                ),
                isDestructive = true,
                onClick = onRemoveClick,
            ),
        ),
    )
}

private fun removeDownloads(context: Context, songs: List<Child>) {
    if (Preferences.getDownloadDirectoryUri() == null) {
        DownloadUtil.getDownloadTracker(context).remove(
            MappingUtil.mapDownloads(songs),
            songs.map(::Download),
        )
        return
    }

    songs.forEach(ExternalAudioReader::delete)
}
