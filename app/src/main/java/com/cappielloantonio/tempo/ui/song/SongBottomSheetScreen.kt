package com.cappielloantonio.tempo.ui.song

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getSongBottomSheetViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.components.ActionBottomSheetContent
import com.cappielloantonio.tempo.ui.components.ActionItemUiModel
import com.cappielloantonio.tempo.ui.components.AssetLinkChips
import com.cappielloantonio.tempo.ui.components.TempusImageType
import com.cappielloantonio.tempo.util.AssetLinkUtil
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.ExternalAudioReader
import com.cappielloantonio.tempo.util.ExternalAudioWriter
import com.cappielloantonio.tempo.util.MappingUtil
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.viewmodel.SongBottomSheetUiState
import com.cappielloantonio.tempo.viewmodel.SongBottomSheetViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SongBottomSheetRoute(
    song: Child,
    playlistId: String?,
    itemPosition: Int,
    onDismiss: () -> Unit,
    onOpenRatingDialog: (Child) -> Unit,
    onOpenPlaylistChooser: (List<Child>) -> Unit,
    onNavigateToAlbum: (AlbumID3) -> Unit,
    onNavigateToArtist: (ArtistID3) -> Unit,
    onPlayNext: (Child) -> Unit,
    onAddToQueue: (Child) -> Unit,
    onStartInstantMix: (List<Child>) -> Unit,
    onOpenAssetLink: (AssetLinkUtil.AssetLink, Boolean) -> Unit,
    onCopyAssetLink: (AssetLinkUtil.AssetLink) -> Unit,
    onRefreshShares: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: SongBottomSheetViewModel = getViewModel {
        getSongBottomSheetViewModel().apply { onStart(song) }
    }
    val uiState by viewModel.uiState.collectAsState()
    val refreshEvent by ExternalAudioReader.getRefreshEvents().observeAsState()
    val currentSong = uiState.song
    val isDownloaded = remember(currentSong?.id, refreshEvent, Preferences.getDownloadDirectoryUri()) {
        currentSong?.let { isSongDownloaded(context, it) } ?: false
    }

    LaunchedEffect(viewModel) {
        viewModel.actions.collect { action ->
            when (action) {
                is SongBottomSheetViewModel.Action.RequestDownload -> downloadSong(context, action.media)
            }
        }
    }

    SongBottomSheetContent(
        uiState = uiState,
        isDownloaded = isDownloaded,
        showRemoveFromPlaylist = !playlistId.isNullOrEmpty() && itemPosition >= 0,
        showShare = Preferences.isSharingEnabled(),
        onFavoriteClick = { viewModel.setFavorite() },
        onFavoriteLongClick = {
            currentSong?.let {
                onOpenRatingDialog(it)
                onDismiss()
            }
        },
        onInstantMixClick = {
            if (currentSong == null) return@SongBottomSheetContent
            Toast.makeText(context, R.string.bottom_sheet_generating_instant_mix, Toast.LENGTH_SHORT).show()
            scope.launch {
                val media = viewModel.getInstantMix().collectAsSingleList()
                if (media.isNotEmpty()) {
                    MusicUtil.ratingFilter(media)
                    onStartInstantMix(media)
                    onDismiss()
                }
            }
        },
        onPlayNextClick = {
            currentSong?.let {
                onPlayNext(it)
                onDismiss()
            }
        },
        onAddToQueueClick = {
            currentSong?.let {
                onAddToQueue(it)
                onDismiss()
            }
        },
        onRateClick = {
            currentSong?.let {
                onOpenRatingDialog(it)
                onDismiss()
            }
        },
        onDownloadClick = {
            currentSong?.let {
                downloadSong(context, it)
                onDismiss()
            }
        },
        onRemoveClick = {
            currentSong?.let {
                removeDownload(context, it)
                onDismiss()
            }
        },
        onRemoveFromPlaylistClick = {
            if (playlistId.isNullOrEmpty() || itemPosition < 0) return@SongBottomSheetContent
            scope.launch {
                when (viewModel.removeFromPlaylist(playlistId, itemPosition)) {
                    SongBottomSheetViewModel.PlaylistRemovalResult.Success -> {
                        Toast.makeText(context, R.string.playlist_chooser_dialog_toast_remove_success, Toast.LENGTH_SHORT).show()
                    }
                    SongBottomSheetViewModel.PlaylistRemovalResult.Failure -> {
                        Toast.makeText(context, R.string.playlist_chooser_dialog_toast_remove_failure, Toast.LENGTH_SHORT).show()
                    }
                    SongBottomSheetViewModel.PlaylistRemovalResult.AllSkipped -> Unit
                }
                onDismiss()
            }
        },
        onAddToPlaylistClick = {
            currentSong?.let {
                onOpenPlaylistChooser(listOf(it))
                onDismiss()
            }
        },
        onGoToAlbumClick = {
            scope.launch {
                val album = viewModel.getAlbum().collectSingleValue()
                if (album != null) {
                    onNavigateToAlbum(album)
                } else {
                    Toast.makeText(context, R.string.song_bottom_sheet_error_retrieving_album, Toast.LENGTH_SHORT).show()
                }
                onDismiss()
            }
        },
        onGoToArtistClick = {
            scope.launch {
                val artist = viewModel.getArtist().collectSingleValue()
                if (artist != null) {
                    onNavigateToArtist(artist)
                } else {
                    Toast.makeText(context, R.string.song_bottom_sheet_error_retrieving_artist, Toast.LENGTH_SHORT).show()
                }
                onDismiss()
            }
        },
        onShareClick = {
            scope.launch {
                val share = viewModel.shareTrack()
                if (share != null) {
                    copyToClipboard(context, context.getString(R.string.app_name), share.url)
                    onRefreshShares()
                } else {
                    Toast.makeText(context, R.string.share_unsupported_error, Toast.LENGTH_SHORT).show()
                }
                onDismiss()
            }
        },
        onCoverClick = {
            currentSong?.id?.let { songId ->
                AssetLinkUtil.buildAssetLink(AssetLinkUtil.TYPE_SONG, songId)?.let { assetLink ->
                    onOpenAssetLink(assetLink, false)
                }
            }
        },
        onCoverLongClick = {
            currentSong?.id?.let { songId ->
                AssetLinkUtil.buildAssetLink(AssetLinkUtil.TYPE_SONG, songId)?.let(onCopyAssetLink)
            }
        },
        onTitleClick = {
            currentSong?.id?.let { songId ->
                AssetLinkUtil.buildAssetLink(AssetLinkUtil.TYPE_SONG, songId)?.let { assetLink ->
                    onOpenAssetLink(assetLink, false)
                }
            }
        },
        onTitleLongClick = {
            currentSong?.id?.let { songId ->
                AssetLinkUtil.buildAssetLink(AssetLinkUtil.TYPE_SONG, songId)?.let(onCopyAssetLink)
            }
        },
        onSubtitleClick = {
            val assetLink = currentSong?.artistId?.let { artistId ->
                AssetLinkUtil.buildAssetLink(AssetLinkUtil.TYPE_ARTIST, artistId)
            } ?: currentSong?.id?.let { songId ->
                AssetLinkUtil.buildAssetLink(AssetLinkUtil.TYPE_SONG, songId)
            }
            assetLink?.let { onOpenAssetLink(it, it.type != AssetLinkUtil.TYPE_SONG) }
        },
        onSubtitleLongClick = {
            val assetLink = currentSong?.artistId?.let { artistId ->
                AssetLinkUtil.buildAssetLink(AssetLinkUtil.TYPE_ARTIST, artistId)
            } ?: currentSong?.id?.let { songId ->
                AssetLinkUtil.buildAssetLink(AssetLinkUtil.TYPE_SONG, songId)
            }
            assetLink?.let(onCopyAssetLink)
        },
        onChipClick = { type, id ->
            AssetLinkUtil.buildAssetLink(type, id)?.let { assetLink ->
                onOpenAssetLink(assetLink, true)
            }
        },
        onChipLongClick = { type, id ->
            AssetLinkUtil.buildAssetLink(type, id)?.let(onCopyAssetLink)
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongBottomSheetContent(
    uiState: SongBottomSheetUiState,
    isDownloaded: Boolean,
    showRemoveFromPlaylist: Boolean,
    showShare: Boolean,
    onFavoriteClick: () -> Unit,
    onFavoriteLongClick: () -> Unit,
    onInstantMixClick: () -> Unit,
    onPlayNextClick: () -> Unit,
    onAddToQueueClick: () -> Unit,
    onRateClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onRemoveFromPlaylistClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onGoToAlbumClick: () -> Unit,
    onGoToArtistClick: () -> Unit,
    onShareClick: () -> Unit,
    onCoverClick: () -> Unit,
    onCoverLongClick: () -> Unit,
    onTitleClick: () -> Unit,
    onTitleLongClick: () -> Unit,
    onSubtitleClick: () -> Unit,
    onSubtitleLongClick: () -> Unit,
    onChipClick: (String, String) -> Unit,
    onChipLongClick: (String, String) -> Unit,
) {
    val song = uiState.song
    val materialActions = listOf(
        ActionItemUiModel(
            icon = Icons.Default.AutoAwesome,
            label = stringResource(R.string.song_bottom_sheet_instant_mix),
            onClick = onInstantMixClick
        ),
        ActionItemUiModel(
            icon = Icons.Default.PlayArrow,
            label = stringResource(R.string.song_bottom_sheet_play_next),
            onClick = onPlayNextClick
        ),
        ActionItemUiModel(
            icon = Icons.Default.QueueMusic,
            label = stringResource(R.string.song_bottom_sheet_add_to_queue),
            onClick = onAddToQueueClick
        ),
        ActionItemUiModel(
            icon = Icons.Default.StarOutline,
            label = stringResource(R.string.song_bottom_sheet_rate),
            onClick = onRateClick
        ),
        ActionItemUiModel(
            icon = Icons.Default.Download,
            label = stringResource(R.string.song_bottom_sheet_download),
            onClick = onDownloadClick,
            isVisible = !isDownloaded
        ),
        ActionItemUiModel(
            icon = Icons.Default.Delete,
            label = stringResource(R.string.song_bottom_sheet_remove),
            onClick = onRemoveClick,
            isVisible = isDownloaded,
            isDestructive = true
        ),
        ActionItemUiModel(
            icon = Icons.Default.Delete,
            label = stringResource(R.string.song_bottom_sheet_remove_from_playlist),
            onClick = onRemoveFromPlaylistClick,
            isVisible = showRemoveFromPlaylist,
            isDestructive = true
        ),
        ActionItemUiModel(
            icon = Icons.Default.PlaylistAdd,
            label = stringResource(R.string.song_bottom_sheet_add_to_playlist),
            onClick = onAddToPlaylistClick
        ),
        ActionItemUiModel(
            icon = Icons.Default.Album,
            label = stringResource(R.string.song_bottom_sheet_go_to_album),
            onClick = onGoToAlbumClick,
            isVisible = !song?.albumId.isNullOrEmpty()
        ),
        ActionItemUiModel(
            icon = Icons.Default.Person,
            label = stringResource(R.string.song_bottom_sheet_go_to_artist),
            onClick = onGoToArtistClick,
            isVisible = !song?.artistId.isNullOrEmpty()
        ),
        ActionItemUiModel(
            icon = Icons.Default.Share,
            label = stringResource(R.string.song_bottom_sheet_share),
            onClick = onShareClick,
            isVisible = showShare
        )
    )

    ActionBottomSheetContent(
        title = song?.title.orEmpty(),
        subtitle = song?.artist,
        coverArtId = song?.coverArtId,
        imageType = TempusImageType.Song,
        actions = materialActions,
        trailingContent = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .combinedClickable(
                        onClick = onFavoriteClick,
                        onLongClick = onFavoriteLongClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (song?.starred != null) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = if (song?.starred != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        headerExtraContent = {
            AssetLinkChips(
                songId = song?.id,
                albumId = song?.albumId,
                artistId = song?.artistId,
                onChipClick = onChipClick,
                onChipLongClick = onChipLongClick
            )
        },
        onCoverClick = onCoverClick,
        onCoverLongClick = onCoverLongClick,
        onTitleClick = onTitleClick,
        onTitleLongClick = onTitleLongClick,
        onSubtitleClick = onSubtitleClick,
        onSubtitleLongClick = onSubtitleLongClick
    )
}

private suspend fun <T> Flow<T>.collectSingleValue(): T = first()

private suspend fun Flow<List<Child>>.collectAsSingleList(): MutableList<Child> =
    first().toMutableList()

private fun copyToClipboard(context: Context, label: String, value: String?) {
    if (value.isNullOrEmpty()) {
        return
    }
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboardManager.setPrimaryClip(ClipData.newPlainText(label, value))
}

private fun downloadSong(context: Context, song: Child) {
    if (Preferences.getDownloadDirectoryUri() == null) {
        DownloadUtil.getDownloadTracker(context).download(
            MappingUtil.mapDownload(song),
            Download(song)
        )
    } else {
        ExternalAudioWriter.downloadToUserDirectory(context, song)
    }
}

private fun removeDownload(context: Context, song: Child) {
    if (Preferences.getDownloadDirectoryUri() == null) {
        DownloadUtil.getDownloadTracker(context).remove(
            MappingUtil.mapDownload(song),
            Download(song)
        )
    } else {
        ExternalAudioReader.delete(song)
    }
}

private fun isSongDownloaded(context: Context, song: Child): Boolean {
    return if (Preferences.getDownloadDirectoryUri() == null) {
        DownloadUtil.getDownloadTracker(context).isDownloaded(song.id)
    } else {
        ExternalAudioReader.getUri(song) != null
    }
}
