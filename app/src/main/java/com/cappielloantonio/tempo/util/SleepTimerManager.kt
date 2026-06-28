package com.cappielloantonio.tempo.util

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.Player
import java.util.Locale
import com.cappielloantonio.tempo.App

/**
 * Singleton that manages a sleep timer countdown.
 *
 * <h3>Rotation survival</h3>
 * The timer survives fragment recreation (e.g. rotation) because it lives
 * in a singleton. Callers reconnect their tick/expiry logic by calling
 * [.setTickListener] on resume and clearing it on stop.
 *
 * <h3>Process-death survival</h3>
 * [.startTimer] and [.startEndOfTrack] persist their state to
 * [SharedPreferences] via [App.getInstance].  When Android
 * kills the process and the singleton is re-created, the constructor
 * restores whatever was saved and, for a countdown timer, resumes the
 * in-process tick loop from the correct wall-clock end time.
 *
 * <h3>End-of-track mode</h3>
 * [.startEndOfTrack] arms a one-shot stop.  Once armed,
 * [.armEndOfTrackFadePoller] polls playback position and
 * triggers a fade-out when the track is about to end.
 *
 * <h3>Fade-out</h3>
 * [.startFadeOutThenPause] and [.armEndOfTrackFadePoller]
 * live here (not in BaseMediaService) so that all sleep-timer logic is
 * consolidated in one place and BaseMediaService stays free of auxiliary
 * sleep-timer state.
 */
class SleepTimerManager private constructor() {
    interface TickListener {
        /**
         * Called on the main thread every second while a countdown is
         * active (expired=false), once more when the countdown reaches zero
         * (expired=true), and once when end-of-track fires (expired=true).
         */
        fun onTick(expired: Boolean)
    }

    /**
     * Listener registered by [com.cappielloantonio.tempo.service.BaseMediaService]
     * to drive the fade-out and pause actions from the service process.
     * Receives the same expired signal as [TickListener] but is kept
     * separate so UI concerns (tick label refresh) and playback actions
     * (fade, pause) are decoupled.
     */
    interface ServiceActionListener {
        /** Called every second for countdown updates, and with expired=true when the timer fires.  */
        fun onTick(expired: Boolean)

        /**
         * Called immediately when end-of-track mode is armed, so the service can
         * call [SleepTimerManager.armEndOfTrackFadePoller] against the live player.
         */
        fun onEndOfTrackArmed()
    }

    private val handler = Handler(Looper.getMainLooper())
    private var scheduledTick: Runnable? = null

    /** Set to true to abort an in-progress fade (e.g. track transition fired early).  */
    @Volatile
    private var abortCurrentFade = false

    private var endOfTrackPoller: Runnable? = null
    private var endTimeMs: Long = 0
    private var active = false
    private var endOfTrack = false

    private var tickListener: TickListener? = null
    private var serviceActionListener: ServiceActionListener? = null

    init {
        restoreFromPreferences()
    }

    /** Start (or restart) the timer for the given number of minutes.  */
    fun startTimer(minutes: Int) {
        cancelInternal(false)
        endOfTrack = false
        endTimeMs = System.currentTimeMillis() + minutes.toLong() * 60 * 1000
        active = true
        persistState()
        scheduleNextTick()
    }

    /**
     * Arm "stop after this song" mode.  The timer fires the next time the
     * caller invokes [.notifyTrackEnded].
     */
    fun startEndOfTrack() {
        cancelInternal(false)
        endOfTrack = true
        active = true
        endTimeMs = 0
        persistState()
        // Notify immediately so the UI can reflect the active state.
        tickListener?.onTick(false)
        serviceActionListener?.onEndOfTrackArmed()
    }

    /**
     * Cancel the timer and notify the listener so the UI resets.
     * Safe to call even when no timer is running.
     */
    fun cancelTimer() {
        cancelInternal(true)
    }

    /** Whether a countdown or end-of-track timer is currently armed.  */
    fun isActive(): Boolean {
        return active
    }

    /** Whether the active timer is in end-of-track (not countdown) mode.  */
    fun isEndOfTrack(): Boolean {
        return endOfTrack
    }

    /**
     * Remaining countdown time formatted as "MM:SS".
     * Returns an empty string when inactive or in end-of-track mode.
     */
    fun getRemainingFormatted(): String {
        if (!active || endOfTrack) return ""
        val ms = getRemainingMs()
        val minutes = ms / 60_000
        val seconds = (ms % 60_000) / 1000
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }

    /**
     * Attach or detach the service-side action listener.
     * Pass `null` when the service is destroyed.
     */
    fun setServiceActionListener(listener: ServiceActionListener?) {
        serviceActionListener = listener
    }

    /**
     * Attach a listener that receives ticks and the expiry event.
     * Pass `null` to disconnect (do this in onStop to avoid leaks).
     * Immediately fires [TickListener.onTick] with the current
     * state so the UI can sync right away.
     */
    fun setTickListener(listener: TickListener?) {
        tickListener = listener
        listener?.onTick(false)
    }

    /**
     * Gradually lowers the player volume to zero over [FADE_DURATION_MS], then
     * pauses and restores full volume. Respects [abortCurrentFade].
     */
    fun startFadeOutThenPause(player: Player) {
        val stepMs = FADE_DURATION_MS / FADE_STEPS
        val decrement = 1f / FADE_STEPS
        val volume = floatArrayOf(1f)
        abortCurrentFade = false
        val fadeStep: Runnable = object : Runnable {
            override fun run() {
                if (abortCurrentFade) {
                    player.volume = 1f
                    return
                }
                volume[0] = Math.max(0f, volume[0] - decrement)
                player.volume = volume[0]
                if (volume[0] > 0f) {
                    handler.postDelayed(this, stepMs)
                } else {
                    // Fade complete — cancel the timer and pause.
                    cancelTimer()
                    player.pause()
                    handler.postDelayed({ player.volume = 1f }, 300)
                }
            }
        }
        handler.post(fadeStep)
    }

    /**
     * Polls playback position every 500 ms while end-of-track is armed.
     * Kicks off [.startFadeOutThenPause] when [FADE_DURATION_MS]
     * or fewer milliseconds remain on the current track.
     */
    fun armEndOfTrackFadePoller(player: Player) {
        stopEndOfTrackPoller()
        abortCurrentFade = false
        endOfTrackPoller = object : Runnable {
            var fadeStarted = false
            override fun run() {
                if (!isEndOfTrack()) return
                if (fadeStarted) return
                val duration = player.duration
                val position = player.currentPosition
                if (duration > 0 && duration != C.TIME_UNSET) {
                    val remaining = duration - position
                    if (remaining in 1..FADE_DURATION_MS) {
                        fadeStarted = true
                        startFadeOutThenPause(player)
                        return
                    }
                }
                handler.postDelayed(this, 500)
            }
        }
        handler.post(endOfTrackPoller!!)
    }

    /** Cancels any running end-of-track position poller.  */
    fun stopEndOfTrackPoller() {
        if (endOfTrackPoller != null) {
            handler.removeCallbacks(endOfTrackPoller!!)
            endOfTrackPoller = null
        }
    }

    private fun getRemainingMs(): Long {
        if (!active || endOfTrack) return 0
        return Math.max(0, endTimeMs - System.currentTimeMillis())
    }

    private fun cancelInternal(notifyListener: Boolean) {
        active = false
        endOfTrack = false
        endTimeMs = 0
        abortCurrentFade = true
        if (scheduledTick != null) {
            handler.removeCallbacks(scheduledTick!!)
            scheduledTick = null
        }
        stopEndOfTrackPoller()
        clearPersistedState()
        if (notifyListener) {
            // expired=false: player keeps playing after a manual cancel.
            tickListener?.onTick(false)
        }
    }

    private fun scheduleNextTick() {
        scheduledTick = Runnable {
            if (!active || endOfTrack) return@Runnable
            val remaining = getRemainingMs()
            if (remaining <= 0) {
                active = false
                scheduledTick = null
                clearPersistedState()
                tickListener?.onTick(true)
                serviceActionListener?.onTick(true)
            } else {
                tickListener?.onTick(false)
                serviceActionListener?.onTick(false)
                scheduleNextTick()
            }
        }
        handler.postDelayed(scheduledTick!!, 1000)
    }

    private fun persistState() {
        val prefs = prefs ?: return
        prefs.edit()
            .putLong(PREF_END_TIME_MS, endTimeMs)
            .putBoolean(PREF_END_OF_TRACK, endOfTrack)
            .apply()
    }

    private fun clearPersistedState() {
        val prefs = prefs ?: return
        prefs.edit()
            .remove(PREF_END_TIME_MS)
            .remove(PREF_END_OF_TRACK)
            .apply()
    }

    private fun restoreFromPreferences() {
        val prefs = prefs ?: return
        val savedEndOfTrack = prefs.getBoolean(PREF_END_OF_TRACK, false)
        val savedEndTime = prefs.getLong(PREF_END_TIME_MS, 0)
        if (savedEndOfTrack) {
            // Restore end-of-track mode — no ticking, just re-arm the flag.
            endOfTrack = true
            active = true
        } else if (savedEndTime > System.currentTimeMillis()) {
            // Restore countdown: the end time is still in the future.
            endTimeMs = savedEndTime
            active = true
            scheduleNextTick()
        } else {
            // Stale data (e.g. timer expired while process was dead).
            clearPersistedState()
        }
    }

    private val prefs: SharedPreferences?
        get() = try {
            App.getSharedPreferences()
        } catch (e: Exception) {
            null
        }

    companion object {
        // SharedPreferences keys
        private const val PREF_END_TIME_MS = "sleep_timer_end_time_ms"
        private const val PREF_END_OF_TRACK = "sleep_timer_end_of_track"

        /** Duration of the volume fade-out in milliseconds.  */
        private const val FADE_DURATION_MS = 10_000L

        /** Number of discrete volume steps during the fade.  */
        private const val FADE_STEPS = 40

        private var instance: SleepTimerManager? = null

        @JvmStatic
        fun getInstance(): SleepTimerManager {
            if (instance == null) {
                instance = SleepTimerManager()
            }
            return instance!!
        }
    }
}
