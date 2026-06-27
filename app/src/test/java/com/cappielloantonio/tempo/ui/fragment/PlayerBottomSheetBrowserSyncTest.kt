package com.cappielloantonio.tempo.ui.fragment

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.PlaybackViewModel
import com.cappielloantonio.tempo.viewmodel.PlayerBottomSheetViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

class PlayerBottomSheetBrowserSyncTest {

    @Test
    fun syncFromPlayer_updatesPlaybackAndMetadataWhenTrackIsPlaying() {
        val player = mock<Player>()
        val miniPlayerViewModel = mock<PlayerBottomSheetViewModel>()
        val playbackViewModel = PlaybackViewModel()
        val mediaItem = createMediaItem()

        whenever(player.currentMediaItem).thenReturn(mediaItem)
        whenever(player.playbackState).thenReturn(Player.STATE_READY)
        whenever(player.playWhenReady).thenReturn(true)

        val syncedMediaId = PlayerBottomSheetBrowserSync.syncFromPlayer(
            player = player,
            playbackViewModel = playbackViewModel,
            playerBottomSheetViewModel = miniPlayerViewModel,
            lastSyncedMediaId = null,
            forceMetadataSync = true
        )

        val placeholderCaptor = ArgumentCaptor.forClass(Child::class.java)
        verify(miniPlayerViewModel).setLiveMedia(
            anyOrNull(),
            eq("song-id"),
            placeholderCaptor.capture()
        )

        assertEquals("song-id", syncedMediaId)
        assertEquals("song-id", playbackViewModel.currentSongId.value)
        assertTrue(playbackViewModel.isPlaying.value)
        assertEquals("Track Title", placeholderCaptor.value.title)
        assertEquals("Track Artist", placeholderCaptor.value.artist)
        assertEquals("song-id", placeholderCaptor.value.id)
    }

    @Test
    fun syncFromPlayer_marksPlaybackPausedWhenPlayWhenReadyIsFalse() {
        val player = mock<Player>()
        val miniPlayerViewModel = mock<PlayerBottomSheetViewModel>()
        val playbackViewModel = PlaybackViewModel()

        whenever(player.currentMediaItem).thenReturn(createMediaItem())
        whenever(player.playbackState).thenReturn(Player.STATE_READY)
        whenever(player.playWhenReady).thenReturn(false)

        PlayerBottomSheetBrowserSync.syncFromPlayer(
            player = player,
            playbackViewModel = playbackViewModel,
            playerBottomSheetViewModel = miniPlayerViewModel,
            lastSyncedMediaId = "song-id",
            forceMetadataSync = false
        )

        assertEquals("song-id", playbackViewModel.currentSongId.value)
        assertFalse(playbackViewModel.isPlaying.value)
        verifyNoMoreInteractions(miniPlayerViewModel)
    }

    @Test
    fun syncFromPlayer_clearsStateWhenCurrentItemIsNull() {
        val player = mock<Player>()
        val miniPlayerViewModel = mock<PlayerBottomSheetViewModel>()
        val playbackViewModel = PlaybackViewModel().apply { update("stale-id", true) }

        whenever(player.currentMediaItem).thenReturn(null)
        whenever(player.playbackState).thenReturn(Player.STATE_IDLE)
        whenever(player.playWhenReady).thenReturn(false)

        val syncedMediaId = PlayerBottomSheetBrowserSync.syncFromPlayer(
            player = player,
            playbackViewModel = playbackViewModel,
            playerBottomSheetViewModel = miniPlayerViewModel,
            lastSyncedMediaId = "stale-id",
            forceMetadataSync = true
        )

        verify(miniPlayerViewModel).clearLiveMedia()
        assertNull(syncedMediaId)
        assertNull(playbackViewModel.currentSongId.value)
        assertFalse(playbackViewModel.isPlaying.value)
    }

    @Test
    fun syncFromPlayer_clearsStaleStateAfterReconnect() {
        val playingPlayer = mock<Player>()
        val emptyPlayer = mock<Player>()
        val miniPlayerViewModel = mock<PlayerBottomSheetViewModel>()
        val playbackViewModel = PlaybackViewModel()

        whenever(playingPlayer.currentMediaItem).thenReturn(createMediaItem())
        whenever(playingPlayer.playbackState).thenReturn(Player.STATE_READY)
        whenever(playingPlayer.playWhenReady).thenReturn(true)

        val firstSyncedId = PlayerBottomSheetBrowserSync.syncFromPlayer(
            player = playingPlayer,
            playbackViewModel = playbackViewModel,
            playerBottomSheetViewModel = miniPlayerViewModel,
            lastSyncedMediaId = null,
            forceMetadataSync = true
        )

        whenever(emptyPlayer.currentMediaItem).thenReturn(null)
        whenever(emptyPlayer.playbackState).thenReturn(Player.STATE_IDLE)
        whenever(emptyPlayer.playWhenReady).thenReturn(false)

        val secondSyncedId = PlayerBottomSheetBrowserSync.syncFromPlayer(
            player = emptyPlayer,
            playbackViewModel = playbackViewModel,
            playerBottomSheetViewModel = miniPlayerViewModel,
            lastSyncedMediaId = firstSyncedId,
            forceMetadataSync = true
        )

        verify(miniPlayerViewModel).clearLiveMedia()
        assertEquals("song-id", firstSyncedId)
        assertNull(secondSyncedId)
        assertNull(playbackViewModel.currentSongId.value)
        assertFalse(playbackViewModel.isPlaying.value)
    }

    private fun createMediaItem(): MediaItem {
        val extras = Bundle().apply {
            putString("id", "song-id")
            putString("type", Constants.MEDIA_TYPE_MUSIC)
            putString("title", "Track Title")
            putString("artist", "Track Artist")
            putString("albumId", "album-id")
            putString("artistId", "artist-id")
            putString("description", "Track description")
        }

        return MediaItem.Builder()
            .setMediaId("song-id")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Track Title")
                    .setArtist("Track Artist")
                    .setExtras(extras)
                    .build()
            )
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(Uri.EMPTY)
                    .setExtras(extras)
                    .build()
            )
            .build()
    }
}
