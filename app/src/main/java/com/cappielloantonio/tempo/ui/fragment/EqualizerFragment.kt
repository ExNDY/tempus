package com.cappielloantonio.tempo.ui.fragment

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.equalizer.EqualizerManager
import com.cappielloantonio.tempo.service.BaseMediaService
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.equalizer.EqualizerBandUiModel
import com.cappielloantonio.tempo.ui.equalizer.EqualizerScreen
import com.cappielloantonio.tempo.ui.equalizer.EqualizerUiState
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Preferences

@UnstableApi
class EqualizerFragment : Fragment() {

    private lateinit var activity: MainActivity
    private var equalizerManager: EqualizerManager? = null
    private var receiverRegistered = false
    private val _uiState = mutableStateOf(EqualizerUiState())

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = requireActivity() as MainActivity
    }

    private val equalizerUpdatedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BaseMediaService.ACTION_EQUALIZER_UPDATED) {
                updateUiStateFromManager()
            }
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as BaseMediaService.LocalBinder
            equalizerManager = binder.getEqualizerManager()
            updateUiStateFromManager()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            equalizerManager = null
            _uiState.value = _uiState.value.copy(isSupported = false)
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(requireContext(), MediaService::class.java).also { intent ->
            intent.action = BaseMediaService.ACTION_BIND_EQUALIZER
            requireActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
        if (!receiverRegistered) {
            ContextCompat.registerReceiver(
                requireContext(),
                equalizerUpdatedReceiver,
                IntentFilter(BaseMediaService.ACTION_EQUALIZER_UPDATED),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            receiverRegistered = true
        }
    }

    override fun onStop() {
        super.onStop()
        requireActivity().unbindService(connection)
        equalizerManager = null
        if (receiverRegistered) {
            try {
                requireContext().unregisterReceiver(equalizerUpdatedReceiver)
            } catch (_: Exception) {}
            receiverRegistered = false
        }
        activity.setBottomSheetVisibility(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                TempusTheme {
                    val state by _uiState
                    EqualizerScreen(
                        uiState = state,
                        onEnabledChange = { enabled ->
                            equalizerManager?.setEnabled(enabled)
                            Preferences.setEqualizerEnabled(enabled)
                            _uiState.value = _uiState.value.copy(isEnabled = enabled)
                        },
                        onBandLevelChange = { bandId, level ->
                            equalizerManager?.setBandLevel(bandId, level.toShort())
                            updateBandInState(bandId, level)
                        },
                        onBandLevelChangeFinished = { bandId, level ->
                            updateBandInState(bandId, level)
                            saveBandLevelsToPreferences()
                        },
                        onResetClick = {
                            resetEqualizer()
                        },
                        onNavigateBack = {
                            activity.navController.navigateUp()
                        }
                    )
                }
            }
        }
    }

    private fun updateUiStateFromManager() {
        val manager = equalizerManager
        if (manager == null || manager.getNumberOfBands().toInt() == 0) {
            _uiState.value = _uiState.value.copy(isSupported = false)
            return
        }

        val bandsCount = manager.getNumberOfBands()
        val range = manager.getBandLevelRange() ?: shortArrayOf(-1500, 1500)
        val isEnabled = Preferences.isEqualizerEnabled()
        manager.setEnabled(isEnabled)

        val savedLevels = Preferences.getEqualizerBandLevels(bandsCount)
        val bandModels = mutableListOf<EqualizerBandUiModel>()

        for (i in 0 until bandsCount) {
            val bandId = i.toShort()
            val freq = manager.getCenterFreq(bandId) ?: 0
            val level = savedLevels.getOrNull(i) ?: manager.getBandLevel(bandId) ?: 0
            manager.setBandLevel(bandId, level)
            bandModels.add(EqualizerBandUiModel(bandId, freq, level.toInt()))
        }

        _uiState.value = EqualizerUiState(
            isEnabled = isEnabled,
            bands = bandModels,
            isSupported = true,
            minLevel = range[0].toInt(),
            maxLevel = range[1].toInt()
        )
    }

    private fun updateBandInState(bandId: Short, level: Int) {
        val currentBands = _uiState.value.bands
        val newBands = currentBands.map {
            if (it.id == bandId) it.copy(level = level) else it
        }
        _uiState.value = _uiState.value.copy(bands = newBands)
    }

    private fun resetEqualizer() {
        val manager = equalizerManager ?: return
        val bandsCount = manager.getNumberOfBands()

        val newBands = _uiState.value.bands.map {
            manager.setBandLevel(it.id, 0.toShort())
            it.copy(level = 0)
        }

        Preferences.setEqualizerBandLevels(ShortArray(bandsCount.toInt()) { 0 })
        _uiState.value = _uiState.value.copy(bands = newBands)
    }

    private fun saveBandLevelsToPreferences() {
        val levels = _uiState.value.bands.map { it.level.toShort() }.toShortArray()
        Preferences.setEqualizerBandLevels(levels)
    }
}
