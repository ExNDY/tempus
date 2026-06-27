package com.cappielloantonio.tempo.ui.playlist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getPlaylistBottomSheetViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.ui.components.ActionBottomSheetContent
import com.cappielloantonio.tempo.ui.components.ActionItemUiModel
import com.cappielloantonio.tempo.ui.components.TempusImageType
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.ExternalAudioReader
import com.cappielloantonio.tempo.util.ExternalAudioWriter
import com.cappielloantonio.tempo.util.MappingUtil
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.viewmodel.PlaylistBottomSheetUiState
import com.cappielloantonio.tempo.viewmodel.PlaylistBottomSheetViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun PlaylistBottomSheetRoute(
    playlist: Playlist,
    onDismiss: () -> Unit,
    onPlay: (List<Child>) -> Unit,
    onAddToQueue: (List<Child>) -> Unit,
    onShufflePlay: (List<Child>) -> Unit,
    onOpenEditor: (Playlist) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onRefreshAfterMutation: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: PlaylistBottomSheetViewModel = getViewModel {
        getPlaylistBottomSheetViewModel().apply { onStart(playlist) }
    }
    val uiState by viewModel.uiState.collectAsState()
    val refreshEvent by ExternalAudioReader.getRefreshEvents().observeAsState()
    val editable = viewModel.isEditableByCurrentUser()
    val hasLocalDownloads = remember(uiState.songs, refreshEvent, Preferences.getDownloadDirectoryUri()) {
        hasAnyDownloaded(context, uiState.songs)
    }

    PlaylistBottomSheetContent(
        uiState = uiState,
        isEditable = editable,
        hasLocalDownloads = hasLocalDownloads,
        showShare = Preferences.isSharingEnabled(),
        onPlayClick = {
            if (uiState.songs.isNotEmpty()) {
                onPlay(uiState.songs)
                onDismiss()
            }
        },
        onAddToQueueClick = {
            if (uiState.songs.isNotEmpty()) {
                onAddToQueue(uiState.songs)
                onDismiss()
            }
        },
        onShuffleClick = {
            if (uiState.songs.isNotEmpty()) {
                onShufflePlay(uiState.songs.shuffled())
                onDismiss()
            }
        },
        onPinToggleClick = { viewModel.togglePin() },
        onDownloadAllClick = {
            downloadSongs(context, uiState.songs, playlist)
            onDismiss()
        },
        onRemoveAllClick = {
            removeSongs(context, uiState.songs)
            onDismiss()
        },
        onEditClick = {
            onOpenEditor(playlist)
            onDismiss()
        },
        onDeleteClick = {
            onDeletePlaylist(playlist)
            onDismiss()
        },
        onShareClick = {
            scope.launch {
                val share = viewModel.sharePlaylist()
                val shareUrl = share?.url
                if (shareUrl != null) {
                    copyToClipboard(context, context.getString(R.string.app_name), shareUrl)
                    onRefreshAfterMutation()
                }
                onDismiss()
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistBottomSheetContent(
    uiState: PlaylistBottomSheetUiState,
    isEditable: Boolean,
    hasLocalDownloads: Boolean,
    showShare: Boolean,
    onPlayClick: () -> Unit,
    onAddToQueueClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onPinToggleClick: () -> Unit,
    onDownloadAllClick: () -> Unit,
    onRemoveAllClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    val playlist = uiState.playlist
    ActionBottomSheetContent(
        title = playlist?.name.orEmpty(),
        subtitle = playlist?.owner,
        coverArtId = playlist?.coverArtId,
        imageType = TempusImageType.Playlist,
        actions = listOf(
            ActionItemUiModel(
                icon = Icons.Default.PlayArrow,
                label = stringResource(R.string.playlist_page_play_button),
                onClick = onPlayClick
            ),
            ActionItemUiModel(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                label = stringResource(R.string.song_bottom_sheet_add_to_queue),
                onClick = onAddToQueueClick
            ),
            ActionItemUiModel(
                icon = Icons.Default.Shuffle,
                label = stringResource(R.string.playlist_page_shuffle_button),
                onClick = onShuffleClick
            ),
            ActionItemUiModel(
                icon = if (uiState.isPinned) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                label = if (uiState.isPinned) stringResource(R.string.menu_unpin_button) else stringResource(R.string.menu_pin_button),
                onClick = onPinToggleClick
            ),
            ActionItemUiModel(
                icon = Icons.Default.Download,
                label = stringResource(R.string.menu_download_all_button),
                onClick = onDownloadAllClick,
                isVisible = uiState.songs.isNotEmpty()
            ),
            ActionItemUiModel(
                icon = Icons.Default.Delete,
                label = stringResource(R.string.album_bottom_sheet_remove_all),
                onClick = onRemoveAllClick,
                isVisible = hasLocalDownloads,
                isDestructive = true
            ),
            ActionItemUiModel(
                icon = Icons.Default.Edit,
                label = stringResource(R.string.playlist_editor_dialog_title),
                onClick = onEditClick,
                isVisible = isEditable
            ),
            ActionItemUiModel(
                icon = Icons.Default.Delete,
                label = stringResource(R.string.playlist_editor_dialog_neutral_button),
                onClick = onDeleteClick,
                isVisible = isEditable,
                isDestructive = true
            ),
            ActionItemUiModel(
                icon = Icons.Default.Share,
                label = stringResource(R.string.song_bottom_sheet_share),
                onClick = onShareClick,
                isVisible = showShare
            )
        ),
        trailingContent = {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (uiState.isPinned) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (uiState.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

private fun copyToClipboard(context: Context, label: String, value: String) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboardManager.setPrimaryClip(ClipData.newPlainText(label, value))
}

private fun downloadSongs(context: Context, songs: List<Child>, playlist: Playlist) {
    if (songs.isEmpty()) return
    if (Preferences.getDownloadDirectoryUri() == null) {
        DownloadUtil.getDownloadTracker(context).download(
            MappingUtil.mapDownloads(songs),
            songs.map { child ->
                Download(child).apply {
                    playlistId = playlist.id
                    playlistName = playlist.name
                }
            }
        )
    } else {
        songs.forEach { song ->
            ExternalAudioWriter.downloadToUserDirectory(context, song, playlist.id, playlist.name)
        }
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
