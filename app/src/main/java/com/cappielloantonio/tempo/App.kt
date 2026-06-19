package com.cappielloantonio.tempo

import android.app.Application
import android.content.Context
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.di.appModule
import com.cappielloantonio.tempo.di.networkModule
import com.cappielloantonio.tempo.di.repositoryModule
import com.cappielloantonio.tempo.di.startDI
import com.cappielloantonio.tempo.di.viewModelModule
import com.cappielloantonio.tempo.helper.ThemeHelper
import com.cappielloantonio.tempo.subsonic.Subsonic
import com.cappielloantonio.tempo.util.ClientCertManager
import com.cappielloantonio.tempo.util.Preferences
import cat.ereza.customactivityoncrash.config.CaocConfig
import com.cappielloantonio.tempo.ui.activity.CrashActivity
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.core.parameter.ParametersHolder
import org.koin.core.qualifier.Qualifier
import kotlin.reflect.KClass

@UnstableApi
class App : Application() {

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
