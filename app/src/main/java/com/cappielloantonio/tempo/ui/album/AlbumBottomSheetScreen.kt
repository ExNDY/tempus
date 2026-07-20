package com.cappielloantonio.tempo.ui.album

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getAlbumBottomSheetViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.components.ActionBottomSheetContent
import com.cappielloantonio.tempo.ui.components.ActionItemUiModel
import com.cappielloantonio.tempo.ui.components.BottomSheetErrorContent
import com.cappielloantonio.tempo.ui.components.BottomSheetLoadingContent
import com.cappielloantonio.tempo.ui.components.TempusImageType
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.ExternalAudioReader
import com.cappielloantonio.tempo.util.ExternalAudioWriter
import com.cappielloantonio.tempo.util.MappingUtil
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.viewmodel.AlbumBottomSheetUiState
import com.cappielloantonio.tempo.viewmodel.AlbumBottomSheetViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun AlbumBottomSheetRoute(
    albumId: String,
    onDismiss: () -> Unit,
    onOpenPlaylistChooser: (List<Child>) -> Unit,
    onNavigateToArtist: (ArtistID3) -> Unit,
    onPlayNext: (List<Child>) -> Unit,
    onAddToQueue: (List<Child>) -> Unit,
    onShufflePlay: (List<Child>) -> Unit,
    onStartInstantMix: (List<Child>) -> Unit,
    onRefreshShares: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: AlbumBottomSheetViewModel = getViewModel {
        getAlbumBottomSheetViewModel()
    }
    LaunchedEffect(albumId) {
        viewModel.onStart(albumId)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> {
            BottomSheetLoadingContent()
            return
        }
        uiState.hasError -> {
            BottomSheetErrorContent(
                onRetry = { viewModel.retry(albumId) },
                onClose = onDismiss,
            )
            return
        }
    }

    val refreshEvent by ExternalAudioReader.getRefreshEvents().observeAsState()
    val tracksState by viewModel.getAlbumTracks().collectAsState(initial = emptyList())
    val isAnyDownloaded = remember(tracksState, refreshEvent, Preferences.getDownloadDirectoryUri()) {
        hasAnyDownloaded(context, tracksState)
    }

    LaunchedEffect(viewModel) {
        viewModel.actions.collect { action ->
            when (action) {
                is AlbumBottomSheetViewModel.Action.RequestDownloads -> downloadSongs(context, action.songs)
            }
        }
    }

    AlbumBottomSheetContent(
        uiState = uiState,
        isRemoveAllVisible = isAnyDownloaded,
        showShare = Preferences.isSharingEnabled(),
        onFavoriteClick = { viewModel.setFavorite() },
        onInstantMixClick = {
            Toast.makeText(context, R.string.bottom_sheet_generating_instant_mix, Toast.LENGTH_SHORT).show()
            scope.launch {
                val media = viewModel.getAlbumInstantMix().first().toMutableList()
                if (media.isNotEmpty()) {
                    MusicUtil.ratingFilter(media)
                    onStartInstantMix(media)
                    onDismiss()
                }
            }
        },
        onShuffleClick = {
            scope.launch {
                val songs = viewModel.getAlbumTracks().first().toMutableList()
                if (songs.isNotEmpty()) {
                    songs.shuffle()
                    onShufflePlay(songs)
                    onDismiss()
                }
            }
        },
        onPlayNextClick = {
            scope.launch {
                val songs = viewModel.getAlbumTracks().first()
                if (songs.isNotEmpty()) {
                    onPlayNext(songs)
                    onDismiss()
                }
            }
        },
        onAddToQueueClick = {
            scope.launch {
                val songs = viewModel.getAlbumTracks().first()
                if (songs.isNotEmpty()) {
                    onAddToQueue(songs)
                    onDismiss()
                }
            }
        },
        onDownloadAllClick = {
            scope.launch {
                val songs = viewModel.getAlbumTracks().first()
                if (songs.isNotEmpty()) {
                    downloadSongs(context, songs)
                    onDismiss()
                }
            }
        },
        onRemoveAllClick = {
            removeSongs(context, tracksState)
            onDismiss()
        },
        onAddToPlaylistClick = {
            scope.launch {
                val songs = viewModel.getAlbumTracks().first()
                if (songs.isNotEmpty()) {
                    onOpenPlaylistChooser(songs)
                    onDismiss()
                }
            }
        },
        onGoToArtistClick = {
            scope.launch {
                val artist = viewModel.getArtist().first()
                if (artist != null) {
                    onNavigateToArtist(artist)
                } else {
                    Toast.makeText(context, R.string.album_error_retrieving_artist, Toast.LENGTH_SHORT).show()
                }
                onDismiss()
            }
        },
        onShareClick = {
            scope.launch {
                val share = viewModel.shareAlbum()
                if (share != null) {
                    copyToClipboard(context, context.getString(R.string.app_name), share.url)
                    onRefreshShares()
                } else {
                    Toast.makeText(context, R.string.share_unsupported_error, Toast.LENGTH_SHORT).show()
                }
                onDismiss()
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumBottomSheetContent(
    uiState: AlbumBottomSheetUiState,
    isRemoveAllVisible: Boolean,
    showShare: Boolean,
    onFavoriteClick: () -> Unit,
    onInstantMixClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onPlayNextClick: () -> Unit,
    onAddToQueueClick: () -> Unit,
    onDownloadAllClick: () -> Unit,
    onRemoveAllClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onGoToArtistClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    val album = uiState.album
    ActionBottomSheetContent(
        title = album?.name.orEmpty(),
        subtitle = album?.artist,
        coverArtId = album?.coverArtId,
        imageType = TempusImageType.Album,
        actions = listOf(
            ActionItemUiModel(
                icon = Icons.Default.AutoAwesome,
                label = stringResource(R.string.album_bottom_sheet_instant_mix),
                onClick = onInstantMixClick
            ),
            ActionItemUiModel(
                icon = Icons.Default.Shuffle,
                label = stringResource(R.string.album_bottom_sheet_shuffle),
                onClick = onShuffleClick
            ),
            ActionItemUiModel(
                icon = Icons.Default.PlayArrow,
                label = stringResource(R.string.album_bottom_sheet_play_next),
                onClick = onPlayNextClick
            ),
            ActionItemUiModel(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                label = stringResource(R.string.album_bottom_sheet_add_to_queue),
                onClick = onAddToQueueClick
            ),
            ActionItemUiModel(
                icon = Icons.Default.Download,
                label = stringResource(R.string.album_bottom_sheet_download_all),
                onClick = onDownloadAllClick,
            ),
            ActionItemUiModel(
                icon = Icons.Default.Delete,
                label = stringResource(R.string.album_bottom_sheet_remove_all),
                onClick = onRemoveAllClick,
                isVisible = isRemoveAllVisible,
                isDestructive = true
            ),
            ActionItemUiModel(
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                label = stringResource(R.string.album_bottom_sheet_add_to_playlist),
                onClick = onAddToPlaylistClick
            ),
            ActionItemUiModel(
                icon = Icons.Default.Person,
                label = stringResource(R.string.album_bottom_sheet_go_to_artist),
                onClick = onGoToArtistClick,
                isVisible = !album?.artistId.isNullOrEmpty()
            ),
            ActionItemUiModel(
                icon = Icons.Default.Share,
                label = stringResource(R.string.album_bottom_sheet_share),
                onClick = onShareClick,
                isVisible = showShare
            )
        ),
        trailingContent = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .combinedClickable(
                        onClick = onFavoriteClick,
                        onLongClick = null
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (album?.starred != null) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = if (album?.starred != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

private fun copyToClipboard(context: Context, label: String, value: String?) {
    if (value.isNullOrEmpty()) return
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboardManager.setPrimaryClip(ClipData.newPlainText(label, value))
}

private fun downloadSongs(context: Context, songs: List<Child>) {
    if (songs.isEmpty()) return
    if (Preferences.getDownloadDirectoryUri() == null) {
        DownloadUtil.getDownloadTracker(context).download(
            MappingUtil.mapDownloads(songs),
            songs.map(::Download)
        )
    } else {
        songs.forEach { ExternalAudioWriter.downloadToUserDirectory(context, it) }
    }
}

private fun removeSongs(context: Context, songs: List<Child>) {
    if (songs.isEmpty()) return
    if (Preferences.getDownloadDirectoryUri() == null) {
        DownloadUtil.getDownloadTracker(context).remove(
            MappingUtil.mapDownloads(songs),
            songs.map(::Download)
        )
    } else {
        songs.forEach(ExternalAudioReader::delete)
    }
}

private fun hasAnyDownloaded(context: Context, songs: List<Child>): Boolean {
    if (songs.isEmpty()) return false
    return if (Preferences.getDownloadDirectoryUri() == null) {
        val mediaItems: List<MediaItem> = MappingUtil.mapDownloads(songs)
        mediaItems.isNotEmpty() && DownloadUtil.getDownloadTracker(context).areDownloaded(mediaItems)
    } else {
        songs.any { ExternalAudioReader.getUri(it) != null }
    }
}
