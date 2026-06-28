package com.cappielloantonio.tempo.ui.activity.base

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.KoinViewModelFactory
import com.cappielloantonio.tempo.helper.ThemeHelper
import com.cappielloantonio.tempo.service.DownloaderService
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.ui.dialog.BatteryOptimizationDialog
import com.cappielloantonio.tempo.util.Flavors
import com.cappielloantonio.tempo.util.Preferences
import com.google.android.material.color.DynamicColors
import com.google.android.material.elevation.SurfaceColors
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
open class BaseActivity : AppCompatActivity() {

    var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null
        private set

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = KoinViewModelFactory()

    override fun onCreate(savedInstanceState: Bundle?) {
        val theme = Preferences.getTheme()
        val darkStyle = Preferences.getDarkThemeStyle()
        val isAmoled = ThemeHelper.AMOLED_MODE == darkStyle
        var applyAmoled = false

        if (ThemeHelper.DARK_MODE == theme || ThemeHelper.AMOLED_MODE == theme) {
            if (isAmoled) {
                setTheme(R.style.AppTheme_Amoled)
                applyAmoled = true
            }
        } else if (ThemeHelper.DEFAULT_MODE == theme) {
            val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES && isAmoled) {
                setTheme(R.style.AppTheme_Amoled)
                applyAmoled = true
            }
        }

        DynamicColors.applyToActivityIfAvailable(this)
        if (applyAmoled) {
            getTheme().applyStyle(R.style.ThemeOverlay_App_Amoled, true)
        }

        super.onCreate(savedInstanceState)
        Flavors.initializeCastContext(this)
        initializeDownloader()
        checkBatteryOptimization()
        checkPermission()
        checkAlwaysOnDisplay()
    }

    override fun onStart() {
        super.onStart()
        if (!isEdgeToEdgeEnabled()) {
            setNavigationBarColor()
        }
        initializeBrowser()
    }

    override fun onStop() {
        releaseBrowser()
        super.onStop()
    }

    private fun checkBatteryOptimization() {
        if (detectBatteryOptimization() && Preferences.askForOptimization()) {
            showBatteryOptimizationDialog()
        }
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun checkAlwaysOnDisplay() {
        if (Preferences.isDisplayAlwaysOn()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun detectBatteryOptimization(): Boolean {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        return !powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    private fun showBatteryOptimizationDialog() {
        val dialog = BatteryOptimizationDialog()
        dialog.show(supportFragmentManager, null)
    }

    private fun initializeBrowser() {
        mediaBrowserListenableFuture = MediaBrowser.Builder(this, SessionToken(this, ComponentName(this, MediaService::class.java))).buildAsync()
    }

    private fun releaseBrowser() {
        mediaBrowserListenableFuture?.let { MediaBrowser.releaseFuture(it) }
    }

    private fun initializeDownloader() {
        try {
            DownloadService.start(this, DownloaderService::class.java)
        } catch (e: IllegalStateException) {
            DownloadService.startForeground(this, DownloaderService::class.java)
        }
    }

    protected open fun isEdgeToEdgeEnabled(): Boolean {
        return false
    }

    private fun setNavigationBarColor() {
        val theme = Preferences.getTheme()
        val darkStyle = Preferences.getDarkThemeStyle()
        val isAmoled = ThemeHelper.AMOLED_MODE == darkStyle
        var applyAmoled = false

        if (ThemeHelper.DARK_MODE == theme || ThemeHelper.AMOLED_MODE == theme) {
            applyAmoled = isAmoled
        } else if (ThemeHelper.DEFAULT_MODE == theme) {
            val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            applyAmoled = nightModeFlags == Configuration.UI_MODE_NIGHT_YES && isAmoled
        }

        if (applyAmoled) {
            window.navigationBarColor = ContextCompat.getColor(this, android.R.color.black)
            window.statusBarColor = ContextCompat.getColor(this, android.R.color.black)
        } else {
            window.navigationBarColor = SurfaceColors.getColorForElevation(this, 8f)
            window.statusBarColor = SurfaceColors.getColorForElevation(this, 0f)
        }
    }

    companion object {
        private const val TAG = "BaseActivity"
    }
}
