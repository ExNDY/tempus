package com.cappielloantonio.tempo.ui.fragment

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Requirements
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.*
import com.cappielloantonio.tempo.BuildConfig
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.helper.ThemeHelper
import com.cappielloantonio.tempo.interfaces.DialogClickCallback
import com.cappielloantonio.tempo.interfaces.ScanCallback
import com.cappielloantonio.tempo.service.BaseMediaService
import com.cappielloantonio.tempo.service.DownloaderService
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.dialog.*
import com.cappielloantonio.tempo.util.*
import com.cappielloantonio.tempo.viewmodel.SettingViewModel
import java.util.*

@OptIn(UnstableApi::class)
class SettingsContainerFragment : PreferenceFragmentCompat() {

    private lateinit var activity: MainActivity
    private lateinit var settingViewModel: SettingViewModel
    private lateinit var directoryPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var equalizerResultLauncher: ActivityResultLauncher<Intent>

    private var mediaServiceBinder: BaseMediaService.LocalBinder? = null
    private var isServiceBound = false

    private val expandedCategories = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        equalizerResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { }

        directoryPickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    requireContext().contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    Preferences.setDownloadDirectoryUri(uri.toString())
                    ExternalAudioReader.refreshCache()
                    Toast.makeText(requireContext(), R.string.settings_download_folder_set, Toast.LENGTH_SHORT).show()
                    checkDownloadDirectory()
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        activity = getActivity() as MainActivity
        val view = super.onCreateView(inflater, container, savedInstanceState)
        settingViewModel = ViewModelProvider(requireActivity()).get(SettingViewModel::class.java)

        view.let {
            listView.setPadding(0, 0, 0, resources.getDimension(R.dimen.global_padding_bottom).toInt())
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        activity.setBottomNavigationBarVisibility(false)
        activity.setBottomSheetVisibility(false)
    }

    override fun onResume() {
        super.onResume()
        expandedCategories.clear()

        setStreamingCacheSize()
        setAppLanguage()
        setVersion()
        setNetworkPingTimeoutBase()

        actionLogout()
        actionScan()
        actionSyncStarredAlbums()
        actionSyncStarredTracks()
        actionSyncStarredArtists()
        actionChangeStreamingCacheStorage()
        actionChangeDownloadStorage()
        actionSetSongPreloadBuffer()
        actionSetDownloadDirectory()
        actionDeleteDownloadStorage()
        actionKeepScreenOn()
        actionAutoDownloadLyrics()
        actionMiniPlayerHeart()

        bindMediaService()
        actionBuiltinEqualizer()
        actionEqualizerSelector()
        actionReplayGainPreamp()

        applyAccordionState()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.global_preferences, rootKey)

        if (BuildConfig.FLAVOR != "tempus") {
            findPreference<PreferenceCategory>("settings_github_update_category_key")?.let {
                preferenceScreen.removePreference(it)
            }
        }

        if (BuildConfig.FLAVOR == "degoogled") {
            findPreference<ClickablePreferenceCategory>("settings_androidauto_category_key")?.let {
                preferenceScreen.removePreference(it)
            }
        }

        preferenceScreen?.let { screen ->
            for (i in 0 until screen.preferenceCount) {
                val pref = screen.getPreference(i)
                if (pref is ClickablePreferenceCategory) {
                    if (pref.key == null) {
                        pref.key = "category_key_$i"
                    }
                    pref.setOnClickListener { cat ->
                        val key = cat.key
                        if (expandedCategories.contains(key)) {
                            expandedCategories.remove(key)
                        } else {
                            expandedCategories.add(key)
                        }
                        applyAccordionState()
                    }
                }
            }
        }

        val themePreference = findPreference<ListPreference>(Preferences.THEME)
        val darkThemeStylePreference = findPreference<ListPreference>("dark_theme_style")

        themePreference?.let { pref ->
            darkThemeStylePreference?.let { stylePref ->
                updateDarkThemeStyleVisibility(pref.value, stylePref)
            }
            pref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                val themeOption = newValue as String
                ThemeHelper.applyTheme(themeOption)
                darkThemeStylePreference?.let { stylePref ->
                    updateDarkThemeStyleVisibility(themeOption, stylePref)
                }
                getActivity()?.recreate()
                true
            }
        }

        darkThemeStylePreference?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
            getActivity()?.recreate()
            true
        }

        findPreference<SwitchPreference>("download_wifi_only")?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                if (newValue is Boolean) {
                    Preferences.setDownloadWifiOnly(newValue)
                    val requirements = if (newValue) Requirements(Requirements.NETWORK_UNMETERED) else Requirements(0)
                    DownloadService.sendSetRequirements(requireContext(), DownloaderService::class.java, requirements, false)
                }
                true
            }
    }

    private fun updateDarkThemeStyleVisibility(themeOption: String?, darkThemeStylePreference: Preference?) {
        darkThemeStylePreference ?: return

        val isDark = when {
            ThemeHelper.DARK_MODE == themeOption || ThemeHelper.AMOLED_MODE == themeOption -> true
            ThemeHelper.DEFAULT_MODE == themeOption -> {
                val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                currentNightMode == Configuration.UI_MODE_NIGHT_YES
            }
            else -> false
        }
        darkThemeStylePreference.isVisible = isDark
    }

    private fun checkDarkThemeStyle() {
        findPreference<Preference>("dark_theme_style")?.let {
            updateDarkThemeStyleVisibility(Preferences.getTheme(), it)
        }
    }

    private fun applyAccordionState() {
        val screen = preferenceScreen ?: return

        for (i in 0 until screen.preferenceCount) {
            val pref = screen.getPreference(i)
            if (pref is PreferenceCategory) {
                for (j in 0 until pref.preferenceCount) {
                    pref.getPreference(j).isVisible = true
                }
            }
        }

        checkSystemEqualizer()
        checkCacheStorage()
        checkStorage()
        checkDownloadDirectory()
        checkEqualizerBands()
        checkDarkThemeStyle()

        for (i in 0 until screen.preferenceCount) {
            val pref = screen.getPreference(i)
            if (pref is PreferenceCategory) {
                val expanded = expandedCategories.contains(pref.key)
                pref.setIcon(if (expanded) R.drawable.ic_arrow_down else R.drawable.ic_navigate_next)
                if (!expanded) {
                    for (j in 0 until pref.preferenceCount) {
                        pref.getPreference(j).isVisible = false
                    }
                }
            }
        }

        listView?.adapter?.notifyDataSetChanged()
    }

    private fun checkSystemEqualizer() {
        val equalizer = findPreference<Preference>("system_equalizer") ?: return
        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)

        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            equalizer.setOnPreferenceClickListener {
                equalizerResultLauncher.launch(intent)
                true
            }
        } else {
            equalizer.isVisible = false
        }
    }

    private fun checkCacheStorage() {
        val storage = findPreference<Preference>("streaming_cache_storage") ?: return
        try {
            val externalFilesDirs = requireContext().getExternalFilesDirs(null)
            if (externalFilesDirs.size < 2 || externalFilesDirs[1] == null) {
                storage.isVisible = false
            } else {
                storage.setSummary(if (Preferences.getStreamingCacheStoragePreference() == 0) R.string.download_storage_internal_dialog_negative_button else R.string.download_storage_external_dialog_positive_button)
            }
        } catch (e: Exception) {
            storage.isVisible = false
        }
    }

    private fun checkStorage() {
        val storage = findPreference<Preference>("download_storage") ?: return
        try {
            val externalFilesDirs = requireContext().getExternalFilesDirs(null)
            if (externalFilesDirs.size < 2 || externalFilesDirs[1] == null) {
                storage.isVisible = false
            } else {
                val pref = Preferences.getDownloadStoragePreference()
                storage.summary = when (pref) {
                    0 -> getString(R.string.download_storage_internal_dialog_negative_button)
                    1 -> getString(R.string.download_storage_external_dialog_positive_button)
                    else -> getString(R.string.download_storage_directory_dialog_neutral_button)
                }
            }
        } catch (e: Exception) {
            storage.isVisible = false
        }
    }

    private fun checkDownloadDirectory() {
        val storage = findPreference<Preference>("download_storage")
        val directory = findPreference<Preference>("set_download_directory") ?: return

        val current = Preferences.getDownloadDirectoryUri()
        if (current != null) {
            storage?.isVisible = false
            directory.isVisible = true
            directory.setIcon(R.drawable.ic_close)
            directory.setTitle(R.string.settings_clear_download_folder)
            directory.summary = current
        } else {
            storage?.isVisible = true
            if (Preferences.getDownloadStoragePreference() == 2) {
                directory.isVisible = true
                directory.setIcon(R.drawable.ic_folder)
                directory.setTitle(R.string.settings_set_download_folder)
                directory.setSummary(R.string.settings_choose_download_folder)
            } else {
                directory.isVisible = false
            }
        }
    }

    private fun setNetworkPingTimeoutBase() {
        val networkPingTimeoutBase = findPreference<EditTextPreference>("network_ping_timeout_base") ?: return
        networkPingTimeoutBase.summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
        networkPingTimeoutBase.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.filters = arrayOf(InputFilter { source, start, end, _, _, _ ->
                for (i in start until end) {
                    if (!Character.isDigit(source[i])) return@InputFilter ""
                }
                null
            })
        }
        networkPingTimeoutBase.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val input = newValue as? String
            input?.isNotEmpty() == true
        }
    }

    private fun setStreamingCacheSize() {
        findPreference<ListPreference>("streaming_cache_size")?.summaryProvider =
            Preference.SummaryProvider<ListPreference> { preference ->
                val entry = preference.entry ?: return@SummaryProvider null
                val currentSizeMb = DownloadUtil.getStreamingCacheSize(requireActivity()) / (1024 * 1024)
                getString(R.string.settings_summary_streaming_cache_size, entry, currentSizeMb.toString())
            }
    }

    private fun setAppLanguage() {
        val localePref = findPreference<ListPreference>("language") ?: return
        val locales = UIUtil.getLangPreferenceDropdownEntries(requireContext())

        localePref.entries = locales.keys.toTypedArray<CharSequence>()
        localePref.entryValues = locales.values.toTypedArray<CharSequence>()

        val value = localePref.value
        if ("default" == value) {
            localePref.setSummary(R.string.settings_system_language)
        } else {
            localePref.summary = Locale.forLanguageTag(value ?: "").displayName
        }

        localePref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            val newLang = newValue as String
            if ("default" == newLang) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                preference.setSummary(R.string.settings_system_language)
            } else {
                val appLocale = LocaleListCompat.forLanguageTags(newLang)
                AppCompatDelegate.setApplicationLocales(appLocale)
                preference.summary = Locale.forLanguageTag(newLang).displayName
            }
            true
        }
    }

    private fun setVersion() {
        findPreference<Preference>("version")?.summary = BuildConfig.VERSION_NAME
    }

    private fun actionLogout() {
        findPreference<Preference>("logout")?.setOnPreferenceClickListener {
            activity.quit()
            true
        }
    }

    private fun actionScan() {
        findPreference<Preference>("scan_library")?.setOnPreferenceClickListener {
            settingViewModel.launchScan(object : ScanCallback {
                override fun onError(exception: Exception) {
                    findPreference<Preference>("scan_library")?.summary = exception.message
                }

                override fun onSuccess(isScanning: Boolean, count: Long) {
                    findPreference<Preference>("scan_library")?.summary = getString(R.string.settings_scan_result, count)
                    if (isScanning) getScanStatus()
                }
            })
            true
        }
    }

    private fun actionSyncStarredTracks() {
        findPreference<SwitchPreference>("sync_starred_tracks_for_offline_use")?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                if (newValue is Boolean && newValue) {
                    val dialog = StarredSyncDialog {
                        (preference as SwitchPreference).isChecked = false
                    }
                    dialog.show(activity.supportFragmentManager, null)
                }
                true
            }
    }

    private fun actionSyncStarredAlbums() {
        findPreference<SwitchPreference>("sync_starred_albums_for_offline_use")?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                if (newValue is Boolean && newValue) {
                    val dialog = StarredAlbumSyncDialog {
                        (preference as SwitchPreference).isChecked = false
                    }
                    dialog.show(activity.supportFragmentManager, null)
                }
                true
            }
    }

    private fun actionSyncStarredArtists() {
        findPreference<SwitchPreference>("sync_starred_artists_for_offline_use")?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                if (newValue is Boolean && newValue) {
                    val dialog = StarredArtistSyncDialog {
                        (preference as SwitchPreference).isChecked = false
                    }
                    dialog.show(activity.supportFragmentManager, null)
                }
                true
            }
    }

    private fun actionChangeStreamingCacheStorage() {
        findPreference<Preference>("streaming_cache_storage")?.setOnPreferenceClickListener {
            val dialog = StreamingCacheStorageDialog(object : DialogClickCallback {
                override fun onPositiveClick() {
                    findPreference<Preference>("streaming_cache_storage")?.setSummary(R.string.streaming_cache_storage_external_dialog_positive_button)
                }

                override fun onNegativeClick() {
                    findPreference<Preference>("streaming_cache_storage")?.setSummary(R.string.streaming_cache_storage_internal_dialog_negative_button)
                }
            })
            dialog.show(activity.supportFragmentManager, null)
            true
        }
    }

    private fun actionChangeDownloadStorage() {
        findPreference<Preference>("download_storage")?.setOnPreferenceClickListener {
            val dialog = DownloadStorageDialog(object : DialogClickCallback {
                override fun onPositiveClick() {
                    findPreference<Preference>("download_storage")?.setSummary(R.string.download_storage_external_dialog_positive_button)
                    checkDownloadDirectory()
                }

                override fun onNegativeClick() {
                    findPreference<Preference>("download_storage")?.setSummary(R.string.download_storage_internal_dialog_negative_button)
                    checkDownloadDirectory()
                }

                override fun onNeutralClick() {
                    findPreference<Preference>("download_storage")?.setSummary(R.string.download_storage_directory_dialog_neutral_button)
                    checkDownloadDirectory()
                }
            })
            dialog.show(activity.supportFragmentManager, null)
            true
        }
    }

    private fun actionSetSongPreloadBuffer() {
        val pref = findPreference<ListPreference>("song_preload_buffer") ?: return
        val ctx = requireContext()
        val res = resources

        val titles = res.getStringArray(R.array.song_preload_buffer_titles)
        val values = res.getStringArray(R.array.song_preload_buffer_values)

        val defaultIdx = 2
        val defaultVal = values[defaultIdx]

        val selectedValue = PreferenceManager.getDefaultSharedPreferences(ctx)
            .getString("song_preload_buffer", defaultVal)

        val valueIdx = values.indexOf(selectedValue)
        val titleIdx = if (valueIdx == -1) defaultIdx else valueIdx

        pref.summary = ctx.getString(R.string.settings_song_preload_buffer_summary, titles[titleIdx])
    }

    private fun actionSetDownloadDirectory() {
        findPreference<Preference>("set_download_directory")?.setOnPreferenceClickListener {
            val current = Preferences.getDownloadDirectoryUri()

            if (current != null) {
                Preferences.setDownloadDirectoryUri(null)
                Preferences.setDownloadStoragePreference(0)
                ExternalAudioReader.refreshCache()
                Toast.makeText(requireContext(), R.string.settings_download_folder_cleared, Toast.LENGTH_SHORT).show()
                checkStorage()
                checkDownloadDirectory()
            } else {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    addFlags(
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
                directoryPickerLauncher.launch(intent)
            }
            true
        }
    }

    private fun actionDeleteDownloadStorage() {
        findPreference<Preference>("delete_download_storage")?.setOnPreferenceClickListener {
            DeleteDownloadStorageDialog().show(activity.supportFragmentManager, null)
            true
        }
    }

    private fun actionMiniPlayerHeart() {
        val preference = findPreference<SwitchPreference>("mini_shuffle_button_visibility") ?: return
        preference.isChecked = Preferences.showShuffleInsteadOfHeart()
        preference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            if (newValue is Boolean) {
                Preferences.setShuffleInsteadOfHeart(newValue)
            }
            true
        }
    }

    private fun actionAutoDownloadLyrics() {
        val preference = findPreference<SwitchPreference>("auto_download_lyrics") ?: return
        preference.isChecked = Preferences.isAutoDownloadLyricsEnabled()
        preference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            if (newValue is Boolean) {
                Preferences.setAutoDownloadLyricsEnabled(newValue)
            }
            true
        }
    }

    private fun getScanStatus() {
        settingViewModel.getScanStatus(object : ScanCallback {
            override fun onError(exception: Exception) {
                findPreference<Preference>("scan_library")?.summary = exception.message
            }

            override fun onSuccess(isScanning: Boolean, count: Long) {
                findPreference<Preference>("scan_library")?.summary = getString(R.string.settings_scan_result, count)
                if (isScanning) getScanStatus()
            }
        })
    }

    private fun actionKeepScreenOn() {
        findPreference<SwitchPreference>("always_on_display")?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                if (newValue is Boolean) {
                    if (newValue) {
                        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
                true
            }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mediaServiceBinder = service as? BaseMediaService.LocalBinder
            isServiceBound = true
            checkEqualizerBands()
            applyAccordionState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mediaServiceBinder = null
            isServiceBound = false
        }
    }

    private fun bindMediaService() {
        val intent = Intent(requireActivity(), MediaService::class.java).apply {
            action = BaseMediaService.ACTION_BIND_EQUALIZER
        }
        requireActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        isServiceBound = true
    }

    private fun checkEqualizerBands() {
        mediaServiceBinder?.let { binder ->
            val numBands = binder.getEqualizerManager().getNumberOfBands()
            findPreference<Preference>("app_equalizer")?.isVisible = numBands > 0
        }
    }

    private fun actionReplayGainPreamp() {
        val preampPref = findPreference<SeekBarPreference>("replay_gain_preamp") ?: return
        preampPref.value = Preferences.getLoudnessPreamp().toInt()
        preampPref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            if (newValue is Int) {
                Preferences.setLoudnessPreamp(newValue.toFloat())
                if (isServiceBound) {
                    mediaServiceBinder?.getPlayer()?.let { player ->
                        ReplayGainUtil.reapplyCurrentTrackGain(player)
                    }
                }
            }
            true
        }
    }

    private fun actionEqualizerSelector() {
        val selectedEqualizer = findPreference<ListPreference>("selected_equalizer") ?: return
        selectedEqualizer.summary = selectedEqualizer.entry
        selectedEqualizer.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val newValStr = newValue as String
            val idx = selectedEqualizer.findIndexOfValue(newValStr)
            val newEntry = if (idx >= 0) selectedEqualizer.entries?.get(idx) else ""
            selectedEqualizer.summary = newEntry
            Preferences.setSelectedEqualizer(newValStr)

            val intent = Intent(requireContext().applicationContext, MediaService::class.java).apply {
                action = BaseMediaService.ACTION_RELOAD_EQUALIZER
            }
            ContextCompat.startForegroundService(requireContext().applicationContext, intent)
            true
        }
    }

    private fun actionBuiltinEqualizer() {
        findPreference<Preference>("builtin_equalizer")?.setOnPreferenceClickListener {
            val navController = NavHostFragment.findNavController(this)
            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.equalizerFragment, true)
                .build()
            activity.setBottomNavigationBarVisibility(true)
            activity.setBottomSheetVisibility(true)
            navController.navigate(R.id.equalizerFragment, null, navOptions)
            true
        }
    }

    override fun onPause() {
        super.onPause()
        if (isServiceBound) {
            requireActivity().unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    companion object {
        private const val TAG = "SettingsFragment"
    }
}
