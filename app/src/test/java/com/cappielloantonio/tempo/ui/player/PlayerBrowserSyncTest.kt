package com.cappielloantonio.tempo.ui.player

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.cappielloantonio.tempo.playback.PlaybackStateStore
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.Constants
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

class PlayerBrowserSyncTest {

    @Test
    fun syncFromPlayer_updatesPlaybackAndMetadataWhenTrackIsPlaying() {
        val player = mock<Player>()
        val miniPlayerViewModel = mock<PlayerBottomSheetViewModel>()
        val playbackStateStore = PlaybackStateStore()
        val mediaItem = createMediaItem()

        whenever(player.currentMediaItem).thenReturn(mediaItem)
        whenever(player.playbackState).thenReturn(Player.STATE_READY)
        whenever(player.playWhenReady).thenReturn(true)

        val syncedMediaId = PlayerBrowserSync.syncFromPlayer(
            player = player,
            playbackStateStore = playbackStateStore,
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
        assertEquals("song-id", playbackStateStore.state.value.currentSongId)
        assertTrue(playbackStateStore.state.value.isPlaying)
        assertEquals("Track Title", placeholderCaptor.value.title)
        assertEquals("Track Artist", placeholderCaptor.value.artist)
        assertEquals("song-id", placeholderCaptor.value.id)
    }

    @Test
    fun syncFromPlayer_marksPlaybackPausedWhenPlayWhenReadyIsFalse() {
        val player = mock<Player>()
        val miniPlayerViewModel = mock<PlayerBottomSheetViewModel>()
        val playbackStateStore = PlaybackStateStore()

        whenever(player.currentMediaItem).thenReturn(createMediaItem())
        whenever(player.playbackState).thenReturn(Player.STATE_READY)
        whenever(player.playWhenReady).thenReturn(false)

        PlayerBrowserSync.syncFromPlayer(
            player = player,
            playbackStateStore = playbackStateStore,
            playerBottomSheetViewModel = miniPlayerViewModel,
            lastSyncedMediaId = "song-id",
            forceMetadataSync = false
        )

        assertEquals("song-id", playbackStateStore.state.value.currentSongId)
        assertFalse(playbackStateStore.state.value.isPlaying)
        verifyNoMoreInteractions(miniPlayerViewModel)
    }

    @Test
    fun syncFromPlayer_clearsStateWhenCurrentItemIsNull() {
        val player = mock<Player>()
        val miniPlayerViewModel = mock<PlayerBottomSheetViewModel>()
        val playbackStateStore = PlaybackStateStore().apply { update("stale-id", true) }

        whenever(player.currentMediaItem).thenReturn(null)
        whenever(player.playbackState).thenReturn(Player.STATE_IDLE)
        whenever(player.playWhenReady).thenReturn(false)

        val syncedMediaId = PlayerBrowserSync.syncFromPlayer(
            player = player,
            playbackStateStore = playbackStateStore,
            playerBottomSheetViewModel = miniPlayerViewModel,
            lastSyncedMediaId = "stale-id",
            forceMetadataSync = true
        )

        verify(miniPlayerViewModel).clearLiveMedia()
        assertNull(syncedMediaId)
        assertNull(playbackStateStore.state.value.currentSongId)
        assertFalse(playbackStateStore.state.value.isPlaying)
    }

    @Test
    fun syncFromPlayer_clearsStaleStateAfterReconnect() {
        val playingPlayer = mock<Player>()
        val emptyPlayer = mock<Player>()
        val miniPlayerViewModel = mock<PlayerBottomSheetViewModel>()
        val playbackStateStore = PlaybackStateStore()

        whenever(playingPlayer.currentMediaItem).thenReturn(createMediaItem())
        whenever(playingPlayer.playbackState).thenReturn(Player.STATE_READY)
        whenever(playingPlayer.playWhenReady).thenReturn(true)

        val firstSyncedId = PlayerBrowserSync.syncFromPlayer(
            player = playingPlayer,
            playbackStateStore = playbackStateStore,
            playerBottomSheetViewModel = miniPlayerViewModel,
            lastSyncedMediaId = null,
            forceMetadataSync = true
        )

        whenever(emptyPlayer.currentMediaItem).thenReturn(null)
        whenever(emptyPlayer.playbackState).thenReturn(Player.STATE_IDLE)
        whenever(emptyPlayer.playWhenReady).thenReturn(false)

        val secondSyncedId = PlayerBrowserSync.syncFromPlayer(
            player = emptyPlayer,
            playbackStateStore = playbackStateStore,
            playerBottomSheetViewModel = miniPlayerViewModel,
            lastSyncedMediaId = firstSyncedId,
            forceMetadataSync = true
        )

        verify(miniPlayerViewModel).clearLiveMedia()
        assertEquals("song-id", firstSyncedId)
        assertNull(secondSyncedId)
        assertNull(playbackStateStore.state.value.currentSongId)
        assertFalse(playbackStateStore.state.value.isPlaying)
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
