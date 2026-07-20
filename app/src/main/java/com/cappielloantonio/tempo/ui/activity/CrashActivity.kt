package com.cappielloantonio.tempo.ui.activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import cat.ereza.customactivityoncrash.CustomActivityOnCrash
import cat.ereza.customactivityoncrash.config.CaocConfig
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.ui.crash.CrashScreen
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.google.android.material.color.DynamicColors
class CrashActivity : AppCompatActivity() {
    var stackTrace: String? = null
        private set
    var configFromIntent: CaocConfig? = null
        private set
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        window.isNavigationBarContrastEnforced = false
        stackTrace = CustomActivityOnCrash.getStackTraceFromIntent(intent)
        configFromIntent = CustomActivityOnCrash.getConfigFromIntent(intent)
        setContent {
            TempusTheme {
                CrashScreen(
                    stackTrace = stackTrace.orEmpty(),
                    onRestartClick = {
                        configFromIntent?.let { config ->
                            CustomActivityOnCrash.restartApplication(this, config)
                        }
                    },
                    onCloseClick = {
                        configFromIntent?.let { config ->
                            CustomActivityOnCrash.closeApplication(this, config)
                        }
                    },
                    onCopyClick = ::copyStackTrace,
                    onShareClick = ::shareStackTrace,
                )
            }
        }
    }
    private fun copyStackTrace() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(getString(R.string.ca_export_clipboard_label), stackTrace.orEmpty())
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.ca_export_toast_log_copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }
    private fun shareStackTrace() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, stackTrace.orEmpty())
        }
        startActivity(Intent.createChooser(intent, getString(R.string.ca_export_button_share)))
    }
    companion object {
        private const val TAG = "MainActivityLogs"
    }
}
