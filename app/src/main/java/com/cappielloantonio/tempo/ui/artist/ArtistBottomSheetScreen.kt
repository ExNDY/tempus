package com.cappielloantonio.tempo.ui.artist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getArtistBottomSheetViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.components.ActionBottomSheetContent
import com.cappielloantonio.tempo.ui.components.ActionItemUiModel
import com.cappielloantonio.tempo.ui.components.BottomSheetErrorContent
import com.cappielloantonio.tempo.ui.components.BottomSheetLoadingContent
import com.cappielloantonio.tempo.ui.components.AssetLinkChips
import com.cappielloantonio.tempo.ui.components.TempusImageType
import com.cappielloantonio.tempo.util.AssetLinkUtil
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.ExternalAudioWriter
import com.cappielloantonio.tempo.util.MappingUtil
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.viewmodel.ArtistBottomSheetUiState
import com.cappielloantonio.tempo.viewmodel.ArtistBottomSheetViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun ArtistBottomSheetRoute(
    artistId: String,
    onDismiss: () -> Unit,
    onNavigateToArtist: (ArtistID3) -> Unit,
    onShufflePlay: (List<Child>) -> Unit,
    onStartInstantMix: (List<Child>) -> Unit,
    onOpenAssetLink: (AssetLinkUtil.AssetLink, Boolean) -> Unit,
    onCopyAssetLink: (AssetLinkUtil.AssetLink) -> Unit,
    onRefreshShares: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: ArtistBottomSheetViewModel = getViewModel {
        getArtistBottomSheetViewModel()
    }
    LaunchedEffect(artistId) {
        viewModel.onStart(artistId)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> {
            BottomSheetLoadingContent()
            return
        }
        uiState.hasError -> {
            BottomSheetErrorContent(
                onRetry = { viewModel.retry(artistId) },
                onClose = onDismiss,
            )
            return
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.actions.collect { action ->
            when (action) {
                is ArtistBottomSheetViewModel.Action.RequestDownloads -> downloadSongs(context, action.songs)
            }
        }
    }

    ArtistBottomSheetContent(
        uiState = uiState,
        onFavoriteClick = { viewModel.setFavorite() },
        onShuffleClick = {
            scope.launch {
                val songs = viewModel.getRandomSongs().first().toMutableList()
                if (songs.isNotEmpty()) {
                    MusicUtil.ratingFilter(songs)
                    onShufflePlay(songs)
                }
                onDismiss()
            }
        },
        onInstantMixClick = {
            scope.launch {
                val songs = viewModel.getArtistInstantMix().first().toMutableList()
                if (songs.isNotEmpty()) {
                    MusicUtil.ratingFilter(songs)
                    onStartInstantMix(songs)
                }
                onDismiss()
            }
        },
        onGoToArtistClick = {
            uiState.artist?.let(onNavigateToArtist)
            onDismiss()
        },
        onDownloadAllClick = {
            scope.launch {
                val songs = viewModel.getAllSongs().first()
                if (songs.isNotEmpty()) {
                    downloadSongs(context, songs)
                }
                onDismiss()
            }
        },
        showShare = Preferences.isSharingEnabled(),
        onShareClick = {
            scope.launch {
                val share = viewModel.shareArtist()
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
            uiState.artist?.id?.let { artistId ->
                AssetLinkUtil.buildAssetLink(AssetLinkUtil.TYPE_ARTIST, artistId)?.let { assetLink ->
                    onOpenAssetLink(assetLink, false)
                }
            }
        },
        onCoverLongClick = {
            uiState.artist?.id?.let { artistId ->
                AssetLinkUtil.buildAssetLink(AssetLinkUtil.TYPE_ARTIST, artistId)?.let(onCopyAssetLink)
            }
        },
        onTitleClick = {
            uiState.artist?.id?.let { artistId ->
                AssetLinkUtil.buildAssetLink(AssetLinkUtil.TYPE_ARTIST, artistId)?.let { assetLink ->
                    onOpenAssetLink(assetLink, false)
                }
            }
        },
        onTitleLongClick = {
            uiState.artist?.id?.let { artistId ->
                AssetLinkUtil.buildAssetLink(AssetLinkUtil.TYPE_ARTIST, artistId)?.let(onCopyAssetLink)
            }
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
fun ArtistBottomSheetContent(
    uiState: ArtistBottomSheetUiState,
    onFavoriteClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onInstantMixClick: () -> Unit,
    onGoToArtistClick: () -> Unit,
    onDownloadAllClick: () -> Unit,
    showShare: Boolean,
    onShareClick: () -> Unit,
    onCoverClick: () -> Unit,
    onCoverLongClick: () -> Unit,
    onTitleClick: () -> Unit,
    onTitleLongClick: () -> Unit,
    onChipClick: (String, String) -> Unit,
    onChipLongClick: (String, String) -> Unit,
) {
    val artist = uiState.artist
    ActionBottomSheetContent(
        title = artist?.name.orEmpty(),
        coverArtId = artist?.coverArtId,
        imageType = TempusImageType.Artist,
        actions = listOf(
            ActionItemUiModel(
                icon = Icons.Default.Shuffle,
                label = stringResource(R.string.artist_bottom_sheet_shuffle),
                onClick = onShuffleClick
            ),
            ActionItemUiModel(
                icon = Icons.Default.AutoAwesome,
                label = stringResource(R.string.artist_bottom_sheet_instant_mix),
                onClick = onInstantMixClick
            ),
            ActionItemUiModel(
                icon = Icons.Default.Person,
                label = stringResource(R.string.song_bottom_sheet_go_to_artist),
                onClick = onGoToArtistClick
            ),
            ActionItemUiModel(
                icon = Icons.Default.Download,
                label = stringResource(R.string.menu_download_all_button),
                onClick = onDownloadAllClick
            ),
            ActionItemUiModel(
                icon = Icons.Default.Share,
                label = stringResource(R.string.song_bottom_sheet_share),
                onClick = onShareClick,
                isVisible = showShare,
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
                    imageVector = if (artist?.starred != null) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = if (artist?.starred != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        headerExtraContent = {
            AssetLinkChips(
                songId = null,
                albumId = null,
                artistId = artist?.id,
                onChipClick = onChipClick,
                onChipLongClick = onChipLongClick,
            )
        },
        onCoverClick = onCoverClick,
        onCoverLongClick = onCoverLongClick,
        onTitleClick = onTitleClick,
        onTitleLongClick = onTitleLongClick,
    )
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

private fun copyToClipboard(context: Context, label: String, value: String?) {
    if (value.isNullOrEmpty()) return
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboardManager.setPrimaryClip(ClipData.newPlainText(label, value))
}
