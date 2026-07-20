package com.cappielloantonio.tempo.service

import androidx.core.content.ContextCompat
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import com.cappielloantonio.tempo.repository.AutomotiveRepository
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

class MediaService : BaseMediaService(), SessionAvailabilityListener {
    private val automotiveRepository = AutomotiveRepository()
    private lateinit var castPlayer: CastPlayer

    private fun initializeCastPlayer() {
        if (GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS
        ) {
            CastContext.getSharedInstance(this, ContextCompat.getMainExecutor(this))
                .addOnSuccessListener { castContext ->
                    castPlayer = createCastPlayer(castContext)
                    castPlayer.setSessionAvailabilityListenerCompat(this@MediaService)
                    initializePlayerListener(castPlayer)
                    if (castPlayer.isCastSessionAvailableCompat())
                        setPlayer(mediaLibrarySession.player, castPlayer)
                }
        }
    }

    override fun getMediaLibrarySessionCallback(): MediaLibrarySession.Callback {
        if (sessionCallback == null) {
            sessionCallback = MediaLibrarySessionCallback(baseContext, this, automotiveRepository)
        }
        return sessionCallback!!
    }

    override fun playerInitHook() {
        super.playerInitHook()
        initializeCastPlayer()
        if (this::castPlayer.isInitialized && castPlayer.isCastSessionAvailableCompat())
            setPlayer(null, castPlayer)
    }

    override fun releasePlayers() {
        if (this::castPlayer.isInitialized) {
            castPlayer.setSessionAvailabilityListenerCompat(null)
            castPlayer.release()
        }
        automotiveRepository.deleteMetadata()
        super.releasePlayers()
    }

    override fun onCastSessionAvailable() {
        setPlayer(exoplayer, castPlayer)
    }

    override fun onCastSessionUnavailable() {
        setPlayer(castPlayer, exoplayer)
    }

    @Suppress("DEPRECATION")
    private fun CastPlayer.isCastSessionAvailableCompat(): Boolean = isCastSessionAvailable

    @Suppress("DEPRECATION")
    private fun CastPlayer.setSessionAvailabilityListenerCompat(listener: SessionAvailabilityListener?) {
        setSessionAvailabilityListener(listener)
    }

    @Suppress("DEPRECATION")
    private fun createCastPlayer(castContext: CastContext): CastPlayer = CastPlayer(castContext)
}
