package com.cappielloantonio.tempo.service
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.HeartRating
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.Rating
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepository
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.Preferences
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
private const val TAG = "BaseSessionCallback"
open class BaseSessionCallback(
    protected val context: Context,
    protected val service: BaseMediaService) :
    MediaLibraryService.MediaLibrarySession.Callback {
    private val subsonicRepository = App.get(SubsonicRepository::class.java)
    // ─────────────────────────────────────────────────────────────
    // CommandButtons
    // ─────────────────────────────────────────────────────────────
    @SuppressLint("PrivateResource")
    private val customCommandToggleShuffleModeOn =
        CommandButton.Builder(CommandButton.ICON_SHUFFLE_OFF)
            .setDisplayName(safeDisplayName(R.string.exo_controls_shuffle_on_description, "Shuffle On"))
            .setSessionCommand(SessionCommand(Constants.CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_ON, Bundle.EMPTY))
            .build()
    @SuppressLint("PrivateResource")
    private val customCommandToggleShuffleModeOff =
        CommandButton.Builder(CommandButton.ICON_SHUFFLE_ON)
            .setDisplayName(safeDisplayName(R.string.exo_controls_shuffle_off_description, "Shuffle Off"))
            .setSessionCommand(SessionCommand(Constants.CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_OFF, Bundle.EMPTY))
            .build()
    @SuppressLint("PrivateResource")
    private val customCommandToggleRepeatModeOff =
        CommandButton.Builder(CommandButton.ICON_REPEAT_OFF)
            .setDisplayName(safeDisplayName(R.string.exo_controls_repeat_off_description, "Repeat Off"))
            .setSessionCommand(SessionCommand(Constants.CUSTOM_COMMAND_TOGGLE_REPEAT_MODE_OFF, Bundle.EMPTY))
            .build()
    @SuppressLint("PrivateResource")
    private val customCommandToggleRepeatModeOne =
        CommandButton.Builder(CommandButton.ICON_REPEAT_ONE)
            .setDisplayName(safeDisplayName(R.string.exo_controls_repeat_one_description, "Repeat One"))
            .setSessionCommand(SessionCommand(Constants.CUSTOM_COMMAND_TOGGLE_REPEAT_MODE_ONE, Bundle.EMPTY))
            .build()
    @SuppressLint("PrivateResource")
    private val customCommandToggleRepeatModeAll =
        CommandButton.Builder(CommandButton.ICON_REPEAT_ALL)
            .setDisplayName(safeDisplayName(R.string.exo_controls_repeat_all_description, "Repeat All"))
            .setSessionCommand(SessionCommand(Constants.CUSTOM_COMMAND_TOGGLE_REPEAT_MODE_ALL, Bundle.EMPTY))
            .build()
    @Suppress("DEPRECATION")
    private val customCommandToggleHeartOn =
        CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setDisplayName(safeDisplayName(R.string.exo_controls_heart_on_description, "Favorite On"))
            .setSessionCommand(SessionCommand(Constants.CUSTOM_COMMAND_TOGGLE_HEART_ON, Bundle.EMPTY))
            .setIconResId(R.drawable.ic_favorite)
            .build()
    @Suppress("DEPRECATION")
    private val customCommandToggleHeartOff =
        CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setDisplayName(safeDisplayName(R.string.exo_controls_heart_off_description, "Favorite Off"))
            .setSessionCommand(SessionCommand(Constants.CUSTOM_COMMAND_TOGGLE_HEART_OFF, Bundle.EMPTY))
            .setIconResId(R.drawable.ic_favorites_outlined)
            .build()
    @Suppress("DEPRECATION")
    private val customCommandToggleHeartLoading =
        CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setDisplayName(safeDisplayName(R.string.cast_expanded_controller_loading, "Loading"))
            .setSessionCommand(SessionCommand(Constants.CUSTOM_COMMAND_TOGGLE_HEART_LOADING, Bundle.EMPTY))
            .setIconResId(R.drawable.ic_bookmark_sync)
            .build()
    @Suppress("DEPRECATION")
    private val customCommandInstantMixOn =
        CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setDisplayName("Instant Mix On")
            .setSessionCommand(SessionCommand(Constants.CUSTOM_COMMAND_INSTANT_MIX_ON, Bundle.EMPTY))
            .setIconResId(R.drawable.ic_instantmix_on)
            .build()
    @Suppress("DEPRECATION")
    private val customCommandInstantMixOff =
        CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setDisplayName("Instant Mix Off")
            .setSessionCommand(SessionCommand(Constants.CUSTOM_COMMAND_INSTANT_MIX_OFF, Bundle.EMPTY))
            .setIconResId(R.drawable.media3_icon_minus_circle_unfilled)
            .build()
    private val customLayoutCommandButtons = listOf(
        customCommandToggleShuffleModeOn,
        customCommandToggleShuffleModeOff,
        customCommandToggleRepeatModeOff,
        customCommandToggleRepeatModeOne,
        customCommandToggleRepeatModeAll,
        customCommandToggleHeartOn,
        customCommandToggleHeartOff,
        customCommandToggleHeartLoading,
        customCommandInstantMixOn,
        customCommandInstantMixOff
    )
    private val playerListener = object : Player.Listener {
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            currentSession?.let { updateMediaNotificationCustomLayout(it) }
        }
        override fun onRepeatModeChanged(repeatMode: Int) {
            currentSession?.let { updateMediaNotificationCustomLayout(it) }
        }
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            currentSession?.let { updateMediaNotificationCustomLayout(it) }
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            currentSession?.let { updateMediaNotificationCustomLayout(it) }
        }
    }
    private var currentSession: MediaSession? = null
    fun handlePlayerChanged(oldPlayer: Player?, newPlayer: Player) {
        oldPlayer?.removeListener(playerListener)
        if (currentSession != null) {
            newPlayer.addListener(playerListener)
        }
    }
    val mediaNotificationSessionCommands =
        MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
            .also { builder ->
                customLayoutCommandButtons.forEach { commandButton ->
                    commandButton.sessionCommand?.let { builder.add(it) }
                }
            }.build()
    // ─────────────────────────────────────────────────────────────
    // onConnect
    // ─────────────────────────────────────────────────────────────
    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        if (currentSession == null) {
            currentSession = session
            session.player.addListener(playerListener)
        }
        val previousButton =
            CommandButton.Builder(CommandButton.ICON_PREVIOUS)
                .setDisplayName(safeDisplayName(R.string.exo_controls_previous_description, "Previous"))
                .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .build()
        val playPauseButton =
            CommandButton.Builder(CommandButton.ICON_PLAY)
                .setDisplayName(safeDisplayName(R.string.exo_controls_play_description, "Play"))
                .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
                .build()
        val nextButton =
            CommandButton.Builder(CommandButton.ICON_NEXT)
                .setDisplayName(safeDisplayName(R.string.exo_controls_next_description, "Next"))
                .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .build()
        if (session.isMediaNotificationController(controller) ||
            session.isAutomotiveController(controller) ||
            session.isAutoCompanionController(controller)
        ) {
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(mediaNotificationSessionCommands)
                .setMediaButtonPreferences(
                    ImmutableList.of(
                        previousButton,
                        playPauseButton,
                        nextButton
                    )
                )
                .setCustomLayout(buildCustomLayout(session.player))
                .build()
        }
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session).build()
    }
    override fun onPlaybackResumption(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        isForPlayback: Boolean
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        Log.d(TAG, "onPlaybackResumption from ${controller.packageName}, isForPlayback: $isForPlayback")
        val settable = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
        val mediaItems = mutableListOf<MediaItem>()
        for (i in 0 until session.player.mediaItemCount) {
            mediaItems.add(session.player.getMediaItemAt(i))
        }
        val startIndex = if (session.player.currentMediaItemIndex != C.INDEX_UNSET) {
            session.player.currentMediaItemIndex
        } else {
            0
        }
        val startPosition = MediaSession.MediaItemsWithStartPosition(
            mediaItems,
            startIndex,
            session.player.currentPosition
        )
        settable.set(startPosition)
        return settable
    }
    // ─────────────────────────────────────────────────────────────
    // Custom layout
    // ─────────────────────────────────────────────────────────────
    protected fun updateMediaNotificationCustomLayout(
        session: MediaSession,
        isRatingPending: Boolean = false
    ) {
        val controller = session.mediaNotificationControllerInfo ?: return
        session.setCustomLayout(
            controller,
            buildCustomLayout(session.player, isRatingPending)
        )
    }
    protected fun buildCustomLayout(
        player: Player,
        isRatingPending: Boolean = false
    ): ImmutableList<CommandButton> {
        val customLayout = mutableListOf<CommandButton>()
        val allButtons = listOf(
            "[heartID]",
            "[repeatID]",
            "[shuffleID]",
            "[instantMixID]")
        val tabButton = listOfNotNull(
            Preferences.getCustomCommandFirstButton(),
            Preferences.getCustomCommandSecondButton()
        ).distinct()
        val remainingButtons = allButtons.filter { it !in tabButton }
        tabButton.forEach { id ->
            getCommandButton(id, player, isRatingPending)?.let { customLayout.add(it) }
        }
        remainingButtons.forEach { id ->
            getCommandButton(id, player, isRatingPending)?.let { customLayout.add(it) }
        }
        return ImmutableList.copyOf(customLayout)
    }
    private fun getCommandButton(id: String, player: Player, isRatingPending: Boolean): CommandButton? {
        return when (id) {
            "[heartID]" -> when {
                player.currentMediaItem == null || isRatingPending -> null
                (player.mediaMetadata.userRating as HeartRating?)?.isHeart == true -> customCommandToggleHeartOn
                else -> customCommandToggleHeartOff
            }
            "[shuffleID]" -> if (player.shuffleModeEnabled) customCommandToggleShuffleModeOff
            else customCommandToggleShuffleModeOn
            "[repeatID]" -> when (player.repeatMode) {
                Player.REPEAT_MODE_ONE -> customCommandToggleRepeatModeOne
                Player.REPEAT_MODE_ALL -> customCommandToggleRepeatModeAll
                else -> customCommandToggleRepeatModeOff
            }
            "[instantMixID]" -> if (!MediaManager.continuousPlayIsRunning.get()) customCommandInstantMixOn
            else customCommandInstantMixOff
            else -> null
        }
    }
    // ─────────────────────────────────────────────────────────────
    // Rating (heart)
    // ─────────────────────────────────────────────────────────────
    override fun onSetRating(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        rating: Rating
    ): ListenableFuture<SessionResult> {
        val currentItem = session.player.currentMediaItem
        if (currentItem == null) return Futures.immediateFuture(SessionResult(SessionError.ERROR_BAD_VALUE))
        
        return onSetRating(
            session,
            controller,
            currentItem.mediaId,
            rating
        )
    }
    override fun onSetRating(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaId: String,
        rating: Rating
    ): ListenableFuture<SessionResult> {
        val isStarring = (rating as HeartRating).isHeart
        val future = SettableFuture.create<SessionResult>()
        CoroutineScope(Dispatchers.IO).launch {
            val response = if (isStarring)
                subsonicRepository.star(mediaId, null, null)
            else
                subsonicRepository.unstar(mediaId, null, null)
            if (response != null && response.error == null) {
                withContext(Dispatchers.Main) {
                    for (i in 0 until session.player.mediaItemCount) {
                        val mediaItem = session.player.getMediaItemAt(i)
                        if (mediaItem.mediaId == mediaId) {
                            val newMetadata = mediaItem.mediaMetadata.buildUpon()
                                .setUserRating(HeartRating(isStarring)).build()
                            session.player.replaceMediaItem(
                                i,
                                mediaItem.buildUpon().setMediaMetadata(newMetadata).build()
                            )
                        }
                    }
                    updateMediaNotificationCustomLayout(session)
                    future.set(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            } else {
                withContext(Dispatchers.Main) {
                    updateMediaNotificationCustomLayout(session)
                    future.set(SessionResult(SessionError(SessionError.ERROR_UNKNOWN, "Network error")))
                }
            }
        }
        return future
    }
    // ─────────────────────────────────────────────────────────────
    // Custom commands dispatcher
    // ─────────────────────────────────────────────────────────────
    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        Log.d(TAG, "onCustomCommand: ${customCommand.customAction}")
        return when (customCommand.customAction) {
            Constants.CUSTOM_COMMAND_INSTANT_MIX_ON -> {
                if (!MediaManager.continuousPlayIsRunning.get() && Preferences.isInstantMixUsable()) {
                    Log.d(TAG, "onCustomCommand: start onInstantMix")
                    service.onInstantMix(session) { updateMediaNotificationCustomLayout(session) }
                }
                else
                    Log.d(TAG, "onCustomCommand: onInstantMix not usable")
                updateMediaNotificationCustomLayout(session)
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            Constants.CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_ON -> {
                session.player.shuffleModeEnabled = true
                updateMediaNotificationCustomLayout(session)
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            Constants.CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_OFF -> {
                session.player.shuffleModeEnabled = false
                updateMediaNotificationCustomLayout(session)
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            Constants.CUSTOM_COMMAND_TOGGLE_REPEAT_MODE_OFF,
            Constants.CUSTOM_COMMAND_TOGGLE_REPEAT_MODE_ONE,
            Constants.CUSTOM_COMMAND_TOGGLE_REPEAT_MODE_ALL -> {
                val nextMode = when (session.player.repeatMode) {
                    Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
                session.player.repeatMode = nextMode
                updateMediaNotificationCustomLayout(session)
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            Constants.CUSTOM_COMMAND_TOGGLE_HEART_ON,
            Constants.CUSTOM_COMMAND_TOGGLE_HEART_OFF -> {
                val currentRating = session.player.mediaMetadata.userRating as? HeartRating
                val isCurrentlyLiked = currentRating?.isHeart ?: false
                updateMediaNotificationCustomLayout(session, isRatingPending = true)
                onSetRating(session, controller, HeartRating(!isCurrentlyLiked))
            }
            else -> Futures.immediateFuture(
                SessionResult(SessionError(SessionError.ERROR_NOT_SUPPORTED, customCommand.customAction))
            )
        }
    }
    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>
    ): ListenableFuture<List<MediaItem>> {
        Log.d(TAG, "onAddMediaItems")
        val updatedMediaItems = mediaItems.map { mediaItem ->
            val mediaMetadata = mediaItem.mediaMetadata
            val newMetadata = mediaMetadata.buildUpon()
                .setArtist(
                    mediaMetadata.artist
                        ?: mediaMetadata.extras?.getString("uri")
                        ?: ""
                )
                .build()
            mediaItem.buildUpon()
                .setUri(mediaItem.requestMetadata.mediaUri)
                .setMediaMetadata(newMetadata)
                .setMimeType(MimeTypes.BASE_TYPE_AUDIO)
                .build()
        }
        return Futures.immediateFuture(updatedMediaItems)
    }
    private fun safeDisplayName(stringRes: Int, fallback: String): String {
        return runCatching { context.getString(stringRes) }
            .getOrDefault("")
            .trim()
            .ifEmpty { fallback }
    }
}
