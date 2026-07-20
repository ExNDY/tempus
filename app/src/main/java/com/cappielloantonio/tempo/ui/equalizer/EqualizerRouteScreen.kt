package com.cappielloantonio.tempo.ui.equalizer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.cappielloantonio.tempo.equalizer.EqualizerManager
import com.cappielloantonio.tempo.navigation.BottomMenuConfig
import com.cappielloantonio.tempo.navigation.DefaultScreenNameExtension.defaultScreenName
import com.cappielloantonio.tempo.navigation.Screen
import com.cappielloantonio.tempo.service.BaseMediaService
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.util.Preferences

object EqualizerRouteScreen : Screen.DefaultScreen {
    override val screenName: String = defaultScreenName()
    override fun bottomMenuConfig(): BottomMenuConfig = BottomMenuConfig.Hidden

    @Composable
    override fun Content(navController: NavController, args: Bundle?) {
        val context = LocalContext.current
        var equalizerManager by remember { mutableStateOf<EqualizerManager?>(null) }
        var uiState by remember { mutableStateOf(EqualizerUiState()) }
        DisposableEffect(context) {
            val receiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == BaseMediaService.ACTION_EQUALIZER_UPDATED) {
                        uiState = buildEqualizerUiState(equalizerManager)
                    }
                }
            }
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) {
                    val binder = service as BaseMediaService.LocalBinder
                    equalizerManager = binder.getEqualizerManager()
                    uiState = buildEqualizerUiState(equalizerManager)
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    equalizerManager = null
                    uiState = uiState.copy(isSupported = false)
                }
            }
            Intent(context, MediaService::class.java).also { intent ->
                intent.action = BaseMediaService.ACTION_BIND_EQUALIZER
                context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
            ContextCompat.registerReceiver(
                context,
                receiver,
                android.content.IntentFilter(BaseMediaService.ACTION_EQUALIZER_UPDATED),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            onDispose {
                runCatching { context.unbindService(connection) }
                runCatching { context.unregisterReceiver(receiver) }
            }
        }
        EqualizerScreen(
            uiState = uiState,
            onEnabledChange = { enabled ->
                equalizerManager?.setEnabled(enabled)
                Preferences.setEqualizerEnabled(enabled)
                uiState = uiState.copy(isEnabled = enabled)
            },
            onBandLevelChange = { bandId, level ->
                equalizerManager?.setBandLevel(bandId, level.toShort())
                uiState = uiState.copy(
                    bands = uiState.bands.map { band ->
                        if (band.id == bandId) band.copy(level = level) else band
                    },
                )
            },
            onBandLevelChangeFinished = { bandId, level ->
                equalizerManager?.setBandLevel(bandId, level.toShort())
                val bands = uiState.bands.map { band ->
                    if (band.id == bandId) band.copy(level = level) else band
                }
                Preferences.setEqualizerBandLevels(bands.map { it.level.toShort() }.toShortArray())
                uiState = uiState.copy(bands = bands)
            },
            onResetClick = {
                val manager = equalizerManager ?: return@EqualizerScreen
                val resetBands = uiState.bands.map { band ->
                    manager.setBandLevel(band.id, 0)
                    band.copy(level = 0)
                }
                Preferences.setEqualizerBandLevels(
                    ShortArray(manager.getNumberOfBands().toInt()),
                )
                uiState = uiState.copy(bands = resetBands)
            },
            onNavigateBack = { navController.navigateUp() },
        )
    }
}

private fun buildEqualizerUiState(
    manager: EqualizerManager?,
): EqualizerUiState {
    if (manager == null || manager.getNumberOfBands().toInt() == 0) {
        return EqualizerUiState(isSupported = false)
    }
    val bandsCount = manager.getNumberOfBands()
    val range = manager.getBandLevelRange() ?: shortArrayOf(-1500, 1500)
    val isEnabled = Preferences.isEqualizerEnabled()
    manager.setEnabled(isEnabled)
    val savedLevels = Preferences.getEqualizerBandLevels(bandsCount)
    val bandModels = buildList {
        for (i in 0 until bandsCount) {
            val bandId = i.toShort()
            val frequency = manager.getCenterFreq(bandId) ?: 0
            val level = savedLevels.getOrNull(i) ?: manager.getBandLevel(bandId) ?: 0
            manager.setBandLevel(bandId, level)
            add(
                EqualizerBandUiModel(
                    id = bandId,
                    frequency = frequency,
                    level = level.toInt(),
                ),
            )
        }
    }
    return EqualizerUiState(
        isEnabled = isEnabled,
        bands = bandModels,
        isSupported = true,
        minLevel = range[0].toInt(),
        maxLevel = range[1].toInt(),
    )
}
