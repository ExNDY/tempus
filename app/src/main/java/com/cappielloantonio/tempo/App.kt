package com.cappielloantonio.tempo

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.media3.common.util.UnstableApi
import cat.ereza.customactivityoncrash.config.CaocConfig
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.request.crossfade
import com.cappielloantonio.tempo.di.startDI
import com.cappielloantonio.tempo.di.networkModule
import com.cappielloantonio.tempo.helper.ThemeHelper
import com.cappielloantonio.tempo.subsonic.Subsonic
import com.cappielloantonio.tempo.ui.activity.CrashActivity
import com.cappielloantonio.tempo.util.ClientCertManager
import com.cappielloantonio.tempo.util.Preferences
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules

@UnstableApi
class App : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        instance = this
        appContext = applicationContext

        // Capture crash logs
        CaocConfig.Builder.create()
            .backgroundMode(CaocConfig.BACKGROUND_MODE_SHOW_CUSTOM)
            .enabled(true)
            .showErrorDetails(true)
            .showRestartButton(true)
            .logErrorOnRestart(true)
            .trackActivities(false)
            .minTimeBetweenCrashesMs(3000)
            .errorDrawable(R.drawable.ui_crash)
            .restartActivity(null)
            .errorActivity(CrashActivity::class.java)
            .apply()

        // Koin
        startDI {
            androidLogger()
            androidContext(this@App)
        }

        val prefs = get<Preferences>()
        val themePref = prefs.getTheme()
        ThemeHelper.applyTheme(themePref)

        ClientCertManager.setupSslSocketFactory(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .allowHardware(false)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(Preferences.getImageCacheSize().toLong() * 1024L * 1024L)
                    .build()
            }
            .components {
                val client = OkHttpClient.Builder().apply {
                    ClientCertManager.sslSocketFactory?.let { sslSocketFactory ->
                        sslSocketFactory(sslSocketFactory, ClientCertManager.trustManager)
                    }
                }.build()
                add(OkHttpNetworkFetcherFactory(callFactory = { client }))
            }
            .build()
    }

    companion object {
        private var instance: App? = null
        private var appContext: Context? = null

        @JvmStatic
        fun getInstance(): App {
            return instance ?: throw IllegalStateException("App not initialized")
        }

        @JvmStatic
        fun getContext(): Context {
            return appContext ?: throw IllegalStateException("Context not initialized")
        }

        @JvmStatic
        fun <T : Any> get(clazz: Class<T>): T {
            return org.koin.java.KoinJavaComponent.get(clazz)
        }

        // Java compatibility for static clients - these will now use Koin under the hood
        @JvmStatic
        fun getSubsonicClientInstance(override: Boolean): Subsonic {
            return getInstance().get<Subsonic>()
        }

        @JvmStatic
        fun getSubsonicPublicClientInstance(override: Boolean): Subsonic {
            return getInstance().get<Subsonic>()
        }

        @JvmStatic
        fun refreshSubsonicClient() {
            // Unload and reload network module to pick up new preferences
            unloadKoinModules(networkModule)
            loadKoinModules(networkModule)
        }
        
        @JvmStatic
        fun getPreferences(): Preferences {
            return getInstance().get<Preferences>()
        }

        @JvmStatic
        fun getSharedPreferences(): android.content.SharedPreferences {
            return androidx.preference.PreferenceManager.getDefaultSharedPreferences(getContext())
        }
    }
}
