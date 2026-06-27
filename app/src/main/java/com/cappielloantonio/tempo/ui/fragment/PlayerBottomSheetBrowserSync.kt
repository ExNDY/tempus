package com.cappielloantonio.tempo.ui.fragment

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.MappingUtil
import com.cappielloantonio.tempo.viewmodel.PlaybackViewModel
import com.cappielloantonio.tempo.viewmodel.PlayerBottomSheetViewModel

internal object PlayerBottomSheetBrowserSync {

    fun syncFromPlayer(
        player: Player,
        playbackViewModel: PlaybackViewModel,
        playerBottomSheetViewModel: PlayerBottomSheetViewModel,
        lastSyncedMediaId: String?,
        forceMetadataSync: Boolean = false,
    ): String? {
        val mediaItem = player.currentMediaItem
        val mediaId = mediaItem?.mediaId
        val isPlaying = player.playbackState == Player.STATE_READY && player.playWhenReady

        playbackViewModel.update(mediaId, isPlaying)

        if (mediaItem == null) {
            playerBottomSheetViewModel.clearLiveMedia()
            return null
        }

        if (forceMetadataSync || mediaId != lastSyncedMediaId) {
            syncMediaItem(mediaItem, playerBottomSheetViewModel)
        }

        return mediaId
    }

    private fun syncMediaItem(
        mediaItem: MediaItem,
        playerBottomSheetViewModel: PlayerBottomSheetViewModel,
    ) {
        val extras = mediaItem.mediaMetadata.extras ?: mediaItem.requestMetadata.extras
        val mediaId = extras?.getString("id") ?: mediaItem.mediaId
        val placeholder = (MappingUtil.mapToChild(mediaItem) ?: Child(mediaId)).apply {
            if (title == null) title = mediaItem.mediaMetadata.title?.toString()
            if (artist == null) artist = mediaItem.mediaMetadata.artist?.toString()
            if (album == null) album = mediaItem.mediaMetadata.albumTitle?.toString()
        }
        val mediaType = extras?.getString("type")

        playerBottomSheetViewModel.setLiveMedia(
            mediaType,
            mediaId,
            placeholder
        )
        playerBottomSheetViewModel.setLiveAlbum(mediaType, extras?.getString("albumId"))
        playerBottomSheetViewModel.setLiveArtist(mediaType, extras?.getString("artistId"))
        playerBottomSheetViewModel.setLiveDescription(extras?.getString("description"))
    }
}
