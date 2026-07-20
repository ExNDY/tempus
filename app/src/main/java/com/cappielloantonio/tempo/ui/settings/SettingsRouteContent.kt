package com.cappielloantonio.tempo.ui.settings

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Requirements
import androidx.navigation.NavController
import androidx.preference.PreferenceManager
import com.cappielloantonio.tempo.BuildConfig
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getSettingViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.equalizer.EqualizerManager
import com.cappielloantonio.tempo.helper.ThemeHelper
import com.cappielloantonio.tempo.interfaces.ScanCallback
import com.cappielloantonio.tempo.service.BaseMediaService
import com.cappielloantonio.tempo.service.DownloaderService
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.playback.PlaybackDiagnosticsMetadata
import com.cappielloantonio.tempo.playback.PlaybackDiagnosticsStore
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.util.requireActivity
import com.cappielloantonio.tempo.ui.dialog.DialogRouteScreen
import com.cappielloantonio.tempo.ui.equalizer.EqualizerRouteScreen
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.ExternalAudioReader
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.util.ReplayGainUtil
import com.cappielloantonio.tempo.util.UIUtil
import com.cappielloantonio.tempo.viewmodel.SettingViewModel
import java.util.Locale

@Composable
fun SettingsRouteContent(
    navController: NavController,
) {
    val activity = LocalContext.current.requireActivity<MainActivity>()
    val context = activity
    val configuration = LocalConfiguration.current
    val settingViewModel = getViewModel<SettingViewModel> { getSettingViewModel() }

    var mediaServiceBinder by remember { mutableStateOf<BaseMediaService.LocalBinder?>(null) }
    var scanSummary by remember { mutableStateOf<String?>(null) }
    var expandedSections by remember { mutableStateOf<Set<String>>(emptySet()) }
    var uiState by remember { mutableStateOf(SettingsUiState()) }

    fun refreshUiState() {
        uiState = uiState.copy(
            sections = buildSettingsSections(
                activity = activity,
                configuration = configuration,
                expandedSections = expandedSections,
                scanSummary = scanSummary,
                mediaServiceBinder = mediaServiceBinder,
            ),
        )
    }

    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@rememberLauncherForActivityResult
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            Preferences.setDownloadDirectoryUri(uri.toString())
            ExternalAudioReader.refreshCache()
            Toast.makeText(context, R.string.settings_download_folder_set, Toast.LENGTH_SHORT).show()
            refreshUiState()
        }
    }

    DisposableEffect(activity, configuration.orientation) {
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        activity.setBottomNavigationBarVisibility(false)
        activity.setBottomSheetVisibility(false)
        activity.setNavigationDrawerLock(true)
        activity.setSystemBarsVisibility(!isLandscape)

        onDispose {
            activity.setBottomSheetVisibility(true)
            activity.toggleNavigationDrawerLockOnOrientationChange()
            activity.toggleBottomNavigationBarVisibilityOnOrientationChange()
        }
    }

    DisposableEffect(context) {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                mediaServiceBinder = service as? BaseMediaService.LocalBinder
                refreshUiState()
            }

            override fun onServiceDisconnected(name: ComponentName) {
                mediaServiceBinder = null
                refreshUiState()
            }
        }

        val intent = Intent(context, MediaService::class.java).apply {
            action = BaseMediaService.ACTION_BIND_EQUALIZER
        }
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        onDispose {
            mediaServiceBinder = null
            runCatching { context.unbindService(connection) }
        }
    }

    LaunchedEffect(configuration.orientation) {
        refreshUiState()
    }

    fun toggleSection(key: String) {
        expandedSections = if (key in expandedSections) {
            expandedSections - key
        } else {
            expandedSections + key
        }
        refreshUiState()
    }

    fun launchScan() {
        settingViewModel.launchScan(object : ScanCallback {
            override fun onError(exception: Exception) {
                scanSummary = exception.message
                refreshUiState()
            }

            override fun onSuccess(isScanning: Boolean, count: Long) {
                scanSummary = context.getString(R.string.settings_scan_result, count)
                refreshUiState()
                if (isScanning) {
                    launchScanStatusLoop(settingViewModel, context, onUpdate = {
                        scanSummary = it
                        refreshUiState()
                    })
                }
            }
        })
    }

    fun showStreamingCacheStorageDialog() {
        navController.navigate(
            DialogRouteScreen.route { _, onClose ->
                StreamingCacheStorageRouteDialog(
                    onDismiss = onClose,
                    onSettingsChanged = ::refreshUiState,
                )
            },
        )
    }

    fun showDownloadStorageDialog() {
        navController.navigate(
            DialogRouteScreen.route { _, onClose ->
                DownloadStorageRouteDialog(
                    onDismiss = onClose,
                    onSettingsChanged = ::refreshUiState,
                )
            },
        )
    }

    fun showDeleteDownloadStorageDialog() {
        navController.navigate(
            DialogRouteScreen.route { _, onClose ->
                DeleteDownloadStorageRouteDialog(
                    onDismiss = onClose,
                    onSettingsChanged = ::refreshUiState,
                )
            },
        )
    }

    fun showStarredSyncDialog() {
        navController.navigate(
            DialogRouteScreen.route { _, onClose ->
                StarredTracksSyncRouteDialog(
                    onDismiss = onClose,
                    onSettingsChanged = ::refreshUiState,
                )
            },
        )
    }

    fun showStarredAlbumSyncDialog() {
        navController.navigate(
            DialogRouteScreen.route { _, onClose ->
                StarredAlbumsSyncRouteDialog(
                    onDismiss = onClose,
                    onSettingsChanged = ::refreshUiState,
                )
            },
        )
    }

    fun showStarredArtistSyncDialog() {
        navController.navigate(
            DialogRouteScreen.route { _, onClose ->
                StarredArtistsSyncRouteDialog(
                    onDismiss = onClose,
                    onSettingsChanged = ::refreshUiState,
                )
            },
        )
    }

    fun handleDownloadDirectoryAction() {
        val current = Preferences.getDownloadDirectoryUri()
        if (current != null) {
            Preferences.setDownloadDirectoryUri(null)
            Preferences.setDownloadStoragePreference(0)
            ExternalAudioReader.refreshCache()
            Toast.makeText(context, R.string.settings_download_folder_cleared, Toast.LENGTH_SHORT).show()
            refreshUiState()
            return
        }

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        directoryPickerLauncher.launch(intent)
    }

    fun openSystemEqualizer() {
        context.startActivity(Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL))
    }

    fun openBuiltinEqualizer() {
        activity.setBottomNavigationBarVisibility(true)
        activity.setBottomSheetVisibility(true)
        navController.navigate(EqualizerRouteScreen.screenName)
    }

    fun openExternalLink(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    fun sharePlaybackDiagnostics() {
        val aggregate = PlaybackDiagnosticsStore(context).snapshot()
        if (!aggregate.hasData) {
            Toast.makeText(
                context,
                context.getString(R.string.settings_playback_diagnostics_empty),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        val report = aggregate.toReport(
            PlaybackDiagnosticsMetadata(
                appVersion = BuildConfig.VERSION_NAME,
                androidSdk = Build.VERSION.SDK_INT,
                deviceModel = listOf(Build.MANUFACTURER, Build.MODEL)
                    .filter { it.isNotBlank() }
                    .joinToString(" "),
            ),
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, report)
        }
        context.startActivity(
            Intent.createChooser(
                shareIntent,
                context.getString(R.string.settings_playback_diagnostics_share_title),
            ),
        )
    }

    fun clearPlaybackDiagnostics() {
        PlaybackDiagnosticsStore(context).clear()
        Toast.makeText(
            context,
            context.getString(R.string.settings_playback_diagnostics_cleared),
            Toast.LENGTH_SHORT,
        ).show()
    }

    fun handleActionClick(item: SettingsActionItemUiModel) {
        when (item.key) {
            KEY_LOGOUT -> activity.quit()
            KEY_SCAN_LIBRARY -> launchScan()
            KEY_STREAMING_CACHE_STORAGE -> showStreamingCacheStorageDialog()
            KEY_DOWNLOAD_STORAGE -> showDownloadStorageDialog()
            KEY_SET_DOWNLOAD_DIRECTORY -> handleDownloadDirectoryAction()
            KEY_DELETE_DOWNLOAD_STORAGE -> showDeleteDownloadStorageDialog()
            KEY_SYSTEM_EQUALIZER -> openSystemEqualizer()
            KEY_BUILTIN_EQUALIZER -> openBuiltinEqualizer()
            KEY_SHARE_PLAYBACK_DIAGNOSTICS -> sharePlaybackDiagnostics()
            KEY_CLEAR_PLAYBACK_DIAGNOSTICS -> clearPlaybackDiagnostics()
            KEY_ABOUT_GITHUB -> openExternalLink(context.getString(R.string.settings_github_link))
            KEY_ABOUT_SUPPORT -> openExternalLink(context.getString(R.string.settings_support_discussion_link))
            KEY_ABOUT_UNDRAW -> openExternalLink(context.getString(R.string.undraw_url))
        }
    }

    fun handleToggleChange(item: SettingsToggleItemUiModel, checked: Boolean) {
        when (item.key) {
            KEY_ALWAYS_ON_DISPLAY -> {
                putBooleanPref(context, item.key, checked)
                if (checked) {
                    activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            KEY_DOWNLOAD_WIFI_ONLY -> {
                Preferences.setDownloadWifiOnly(checked)
                val requirements = if (checked) {
                    Requirements(Requirements.NETWORK_UNMETERED)
                } else {
                    Requirements(0)
                }
                DownloadService.sendSetRequirements(
                    context,
                    DownloaderService::class.java,
                    requirements,
                    false,
                )
            }

            KEY_AUTO_DOWNLOAD_LYRICS -> Preferences.setAutoDownloadLyricsEnabled(checked)
            KEY_MINI_SHUFFLE_BUTTON_VISIBILITY -> Preferences.setShuffleInsteadOfHeart(checked)
            KEY_SYNC_STARRED_TRACKS -> {
                Preferences.setStarredSyncEnabled(checked)
                if (checked) {
                    showStarredSyncDialog()
                }
            }

            KEY_SYNC_STARRED_ALBUMS -> {
                Preferences.setStarredAlbumsSyncEnabled(checked)
                if (checked) {
                    showStarredAlbumSyncDialog()
                }
            }

            KEY_SYNC_STARRED_ARTISTS -> {
                Preferences.setStarredArtistsSyncEnabled(checked)
                if (checked) {
                    showStarredArtistSyncDialog()
                }
            }

            else -> putBooleanPref(context, item.key, checked)
        }

        refreshUiState()
    }

    fun handleSliderChange(item: SettingsSliderItemUiModel, value: Int) {
        when (item.key) {
            KEY_LOUDNESS_PREAMP -> {
                Preferences.setLoudnessPreamp(value.toFloat())
                mediaServiceBinder?.getPlayer()?.let { player ->
                    ReplayGainUtil.reapplyCurrentTrackGain(player)
                }
            }

            KEY_MIN_STAR_RATING -> putIntPref(context, item.key, value)
        }
        refreshUiState()
    }

    fun showSelectionDialog(item: SettingsSelectItemUiModel) {
        uiState = uiState.copy(
            selectionDialog = SettingsSelectionDialogState(
                key = item.key,
                title = item.title,
                options = selectionOptionsFor(context, item.key),
                selectedValue = currentSelectedValueFor(context, item.key),
            ),
        )
    }

    fun showTextInputDialog(item: SettingsInputItemUiModel) {
        uiState = uiState.copy(
            textInputDialog = SettingsTextInputDialogState(
                key = item.key,
                title = item.title,
                value = item.value,
                supportingText = item.summary,
            ),
        )
    }

    fun handleSelectionConfirmed(key: String, value: String) {
        when (key) {
            KEY_LANGUAGE -> {
                putStringPref(context, key, value)
                if (value == DEFAULT_LANGUAGE) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                } else {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(value))
                }
            }

            KEY_THEME -> {
                putStringPref(context, key, value)
                ThemeHelper.applyTheme(value)
                activity.recreate()
            }

            KEY_DARK_THEME_STYLE -> {
                putStringPref(context, key, value)
                activity.recreate()
            }

            KEY_SELECTED_EQUALIZER -> {
                Preferences.setSelectedEqualizer(value)
                val intent = Intent(context.applicationContext, MediaService::class.java).apply {
                    action = BaseMediaService.ACTION_RELOAD_EQUALIZER
                }
                ContextCompat.startForegroundService(context.applicationContext, intent)
            }

            else -> putStringPref(context, key, value)
        }

        uiState = uiState.copy(selectionDialog = null)
        refreshUiState()
    }

    fun handleTextInputConfirmed(key: String, value: String) {
        if (key == KEY_NETWORK_PING_TIMEOUT && value.isNotBlank()) {
            Preferences.setNetworkPingTimeout(value)
        }
        uiState = uiState.copy(textInputDialog = null)
        refreshUiState()
    }

    SettingsScreen(
        uiState = uiState,
        onNavigateBack = { navController.navigateUp() },
        onSectionToggle = ::toggleSection,
        onActionClick = ::handleActionClick,
        onToggleChange = ::handleToggleChange,
        onSelectClick = ::showSelectionDialog,
        onSliderChange = ::handleSliderChange,
        onInputClick = ::showTextInputDialog,
        onSelectionDismiss = {
            uiState = uiState.copy(selectionDialog = null)
        },
        onSelectionConfirm = ::handleSelectionConfirmed,
        onTextInputDismiss = {
            uiState = uiState.copy(textInputDialog = null)
        },
        onTextInputConfirm = ::handleTextInputConfirmed,
    )
}

private fun launchScanStatusLoop(
    settingViewModel: SettingViewModel,
    context: Context,
    onUpdate: (String?) -> Unit,
) {
    settingViewModel.getScanStatus(object : ScanCallback {
        override fun onError(exception: Exception) {
            onUpdate(exception.message)
        }

        override fun onSuccess(isScanning: Boolean, count: Long) {
            onUpdate(context.getString(R.string.settings_scan_result, count))
            if (isScanning) {
                launchScanStatusLoop(settingViewModel, context, onUpdate)
            }
        }
    })
}

private fun buildSettingsSections(
    activity: MainActivity,
    configuration: Configuration,
    expandedSections: Set<String>,
    scanSummary: String?,
    mediaServiceBinder: BaseMediaService.LocalBinder?,
): List<SettingsSectionUiModel> = buildList {
    add(
        section(
            key = SECTION_GENERAL,
            title = activity.getString(R.string.settings_title_general),
            groups = listOf(
                SettingsGroupUiModel(
                    items = listOf(
                        SettingsActionItemUiModel(
                            key = KEY_LOGOUT,
                            title = activity.getString(R.string.settings_logout_title),
                            destructive = true,
                        ),
                        SettingsActionItemUiModel(
                            key = KEY_SCAN_LIBRARY,
                            title = activity.getString(R.string.settings_scan_title),
                            summary = scanSummary,
                        ),
                        SettingsInputItemUiModel(
                            key = KEY_NETWORK_PING_TIMEOUT,
                            title = activity.getString(R.string.settings_ping_timeout_title),
                            summary = activity.getString(R.string.settings_ping_timeout_summary),
                            value = Preferences.getNetworkPingTimeout().toString(),
                        ),
                    ),
                ),
            ),
            expandedSections = expandedSections,
        ),
    )
    add(
        section(
            key = SECTION_UI,
            title = activity.getString(R.string.settings_title_ui),
            groups = listOf(
                SettingsGroupUiModel(
                    items = buildList {
                        add(buildLanguageItem(activity))
                        add(
                            selectItem(
                                context = activity,
                                key = KEY_THEME,
                                titleRes = R.string.settings_theme,
                                entriesRes = R.array.theme_list_titles,
                                valuesRes = R.array.theme_list_values,
                                selectedValue = getStringPref(activity, KEY_THEME, ThemeHelper.DEFAULT_MODE),
                            ),
                        )
                        if (isDarkThemeVisible(configuration, getStringPref(activity, KEY_THEME, ThemeHelper.DEFAULT_MODE))) {
                            add(
                                selectItem(
                                    context = activity,
                                    key = KEY_DARK_THEME_STYLE,
                                    titleRes = R.string.settings_dark_theme_style,
                                    entriesRes = R.array.dark_theme_style_titles,
                                    valuesRes = R.array.dark_theme_style_values,
                                    selectedValue = getStringPref(activity, KEY_DARK_THEME_STYLE, DEFAULT_DARK_THEME_STYLE),
                                ),
                            )
                        }
                        add(toggleItem(activity, KEY_ALWAYS_ON_DISPLAY, R.string.settings_always_on_display, null, getBooleanPref(activity, KEY_ALWAYS_ON_DISPLAY, false)))
                        add(selectItem(activity, KEY_TILE_SIZE, R.string.settings_tile_size, R.array.tile_size_titles, R.array.tile_size_divisor, getStringPref(activity, KEY_TILE_SIZE, DEFAULT_TILE_SIZE)))
                        add(toggleItem(activity, KEY_ENABLE_DRAWER_ON_PORTRAIT, R.string.settings_enable_drawer_on_landscape, R.string.settings_enable_drawer_on_landscape_summary, getBooleanPref(activity, KEY_ENABLE_DRAWER_ON_PORTRAIT, false)))
                        add(toggleItem(activity, KEY_HIDE_BOTTOM_NAVBAR_ON_PORTRAIT, R.string.settings_hide_bottom_navbar_on_portrait, R.string.settings_hide_bottom_navbar_on_portrait_summary, getBooleanPref(activity, KEY_HIDE_BOTTOM_NAVBAR_ON_PORTRAIT, false)))
                        add(toggleItem(activity, KEY_ROUNDED_CORNER, R.string.settings_rounded_corner, R.string.settings_rounded_corner_summary, getBooleanPref(activity, KEY_ROUNDED_CORNER, true)))
                        add(selectItem(activity, KEY_ROUNDED_CORNER_SIZE, R.string.settings_rounded_corner_size, R.array.rounded_corner_size_titles, R.array.rounded_corner_size_values, getStringPref(activity, KEY_ROUNDED_CORNER_SIZE, DEFAULT_ROUNDED_CORNER_SIZE)))
                        add(toggleItem(activity, KEY_AUDIO_QUALITY_PER_ITEM, R.string.settings_audio_quality, R.string.settings_audio_quality_summary, getBooleanPref(activity, KEY_AUDIO_QUALITY_PER_ITEM, false)))
                        add(toggleItem(activity, KEY_SONG_RATING_PER_ITEM, R.string.settings_song_rating, R.string.settings_song_rating_summary, getBooleanPref(activity, KEY_SONG_RATING_PER_ITEM, false)))
                        add(toggleItem(activity, KEY_TRACK_NUMBER_VISIBLE, R.string.settings_track_number, R.string.settings_track_number_summary, getBooleanPref(activity, KEY_TRACK_NUMBER_VISIBLE, false)))
                        add(toggleItem(activity, KEY_RATING_PER_ITEM, R.string.settings_item_rating, R.string.settings_item_rating_summary, getBooleanPref(activity, KEY_RATING_PER_ITEM, false)))
                        add(toggleItem(activity, KEY_AUTO_DOWNLOAD_LYRICS, R.string.settings_auto_download_lyrics, R.string.settings_auto_download_lyrics_summary, Preferences.isAutoDownloadLyricsEnabled()))
                        add(toggleItem(activity, KEY_MUSIC_DIRECTORY_SECTION_VISIBILITY, R.string.settings_music_directory, R.string.settings_music_directory_summary, getBooleanPref(activity, KEY_MUSIC_DIRECTORY_SECTION_VISIBILITY, true)))
                        add(toggleItem(activity, KEY_ALBUM_DETAIL, R.string.settings_album_detail, R.string.settings_album_detail_summary, getBooleanPref(activity, KEY_ALBUM_DETAIL, false)))
                        add(toggleItem(activity, KEY_ARTIST_SORT_BY_ALBUM_COUNT, R.string.settings_artist_sort_by_album_count, R.string.settings_artist_sort_by_album_count_summary, getBooleanPref(activity, KEY_ARTIST_SORT_BY_ALBUM_COUNT, false)))
                        add(toggleItem(activity, KEY_SORT_SEARCH_CHRONOLOGICALLY, R.string.search_sort_title, R.string.search_sort_summary, getBooleanPref(activity, KEY_SORT_SEARCH_CHRONOLOGICALLY, false)))
                    },
                ),
            ),
            expandedSections = expandedSections,
        ),
    )
    add(
        section(
            key = SECTION_SOUND,
            title = activity.getString(R.string.settings_title_sound),
            groups = buildList {
                add(
                    SettingsGroupUiModel(
                        title = activity.getString(R.string.settings_title_equalizer),
                        items = buildList {
                            add(selectItem(activity, KEY_SELECTED_EQUALIZER, R.string.settings_equalizer_select, R.array.selected_equalizer_entries, R.array.selected_equalizer_values, getStringPref(activity, KEY_SELECTED_EQUALIZER, DEFAULT_SELECTED_EQUALIZER)))
                            if (isSystemEqualizerAvailable(activity)) {
                                add(
                                    SettingsActionItemUiModel(
                                        key = KEY_SYSTEM_EQUALIZER,
                                        title = activity.getString(R.string.settings_system_equalizer_title),
                                        summary = activity.getString(R.string.settings_system_equalizer_summary),
                                    ),
                                )
                            }
                            if (isBuiltinEqualizerVisible(mediaServiceBinder)) {
                                add(
                                    SettingsActionItemUiModel(
                                        key = KEY_BUILTIN_EQUALIZER,
                                        title = activity.getString(R.string.settings_app_equalizer),
                                        summary = activity.getString(R.string.settings_app_equalizer_summary),
                                    ),
                                )
                            }
                        },
                    ),
                )
                add(
                    SettingsGroupUiModel(
                        title = activity.getString(R.string.settings_title_replay_gain),
                        items = listOf(
                            SettingsInfoItemUiModel("replay_gain_info", activity.getString(R.string.settings_summary_replay_gain)),
                            selectItem(activity, KEY_REPLAY_GAIN_MODE, R.string.settings_replay_gain, R.array.replay_gain_titles, R.array.replay_gain_values, getStringPref(activity, KEY_REPLAY_GAIN_MODE, DEFAULT_REPLAY_GAIN_MODE)),
                            toggleItem(activity, KEY_REPLAY_GAIN_PREVENT_CLIPPING, R.string.settings_replay_gain_prevent_clipping_title, R.string.settings_replay_gain_prevent_clipping_summary, getBooleanPref(activity, KEY_REPLAY_GAIN_PREVENT_CLIPPING, true)),
                        ),
                    ),
                )
                add(
                    SettingsGroupUiModel(
                        title = activity.getString(R.string.settings_title_loudness),
                        items = listOf(
                            SettingsInfoItemUiModel("loudness_info", activity.getString(R.string.settings_summary_loudness)),
                            SettingsSliderItemUiModel(
                                key = KEY_LOUDNESS_PREAMP,
                                title = activity.getString(R.string.settings_loudness_preamp_title),
                                summary = activity.getString(R.string.settings_loudness_preamp_summary),
                                value = Preferences.getLoudnessPreamp().toInt(),
                                valueRange = -15..15,
                                steps = 29,
                                trailingValue = "${Preferences.getLoudnessPreamp().toInt()} dB",
                            ),
                        ),
                    ),
                )
            },
            expandedSections = expandedSections,
        ),
    )
    add(
        section(
            key = SECTION_PLAYERS,
            title = activity.getString(R.string.settings_title_players),
            groups = listOf(
                SettingsGroupUiModel(
                    items = listOf(
                        toggleItem(activity, KEY_CONTINUOUS_PLAY, R.string.settings_continuous_play_title, R.string.settings_continuous_play_summary, getBooleanPref(activity, KEY_CONTINUOUS_PLAY, true)),
                        selectItem(activity, KEY_CUSTOM_COMMAND_FIRST_BUTTON, R.string.settings_custom_command_first_button, R.array.custom_commands_titles, R.array.custom_commands_values, getStringPref(activity, KEY_CUSTOM_COMMAND_FIRST_BUTTON, DEFAULT_FIRST_CUSTOM_COMMAND)),
                        selectItem(activity, KEY_CUSTOM_COMMAND_SECOND_BUTTON, R.string.settings_custom_command_second_button, R.array.custom_commands_titles, R.array.custom_commands_values, getStringPref(activity, KEY_CUSTOM_COMMAND_SECOND_BUTTON, DEFAULT_SECOND_CUSTOM_COMMAND)),
                        SettingsSliderItemUiModel(
                            key = KEY_MIN_STAR_RATING,
                            title = activity.getString(R.string.settings_title_skip_min_star_rating),
                            summary = activity.getString(R.string.settings_summary_skip_min_star_rating),
                            value = getIntPref(activity, KEY_MIN_STAR_RATING, 0),
                            valueRange = 0..4,
                            steps = 3,
                            trailingValue = getIntPref(activity, KEY_MIN_STAR_RATING, 0).toString(),
                        ),
                        SettingsActionItemUiModel(
                            key = KEY_SHARE_PLAYBACK_DIAGNOSTICS,
                            title = activity.getString(R.string.settings_playback_diagnostics_share_title),
                            summary = activity.getString(R.string.settings_playback_diagnostics_share_summary),
                        ),
                        SettingsActionItemUiModel(
                            key = KEY_CLEAR_PLAYBACK_DIAGNOSTICS,
                            title = activity.getString(R.string.settings_playback_diagnostics_clear_title),
                            summary = activity.getString(R.string.settings_playback_diagnostics_clear_summary),
                            destructive = true,
                        ),
                    ),
                ),
            ),
            expandedSections = expandedSections,
        ),
    )
    add(
        section(
            key = SECTION_PLAYLIST,
            title = activity.getString(R.string.settings_title_playlist),
            groups = listOf(
                SettingsGroupUiModel(
                    items = listOf(
                        toggleItem(activity, KEY_ALLOW_PLAYLIST_DUPLICATES, R.string.settings_allow_playlist_duplicates, R.string.settings_allow_playlist_duplicates_summary, getBooleanPref(activity, KEY_ALLOW_PLAYLIST_DUPLICATES, false)),
                        selectItem(activity, KEY_HOME_SORT_PLAYLISTS, R.string.settings_playlist_sort, R.array.playlist_sort_option_titles, R.array.playlist_sort_option_values, getStringPref(activity, KEY_HOME_SORT_PLAYLISTS, DEFAULT_PLAYLIST_SORT)),
                    ),
                ),
            ),
            expandedSections = expandedSections,
        ),
    )
    add(
        section(
            key = SECTION_DATA,
            title = activity.getString(R.string.settings_title_data),
            groups = listOf(
                SettingsGroupUiModel(
                    items = buildList {
                        add(
                            selectItem(
                                context = activity,
                                key = KEY_STREAMING_CACHE_SIZE,
                                titleRes = R.string.settings_streaming_cache_size,
                                entriesRes = R.array.streaming_cache_size_titles,
                                valuesRes = R.array.streaming_cache_size_values,
                                selectedValue = getStringPref(activity, KEY_STREAMING_CACHE_SIZE, DEFAULT_STREAMING_CACHE_SIZE),
                                summary = activity.getString(
                                    R.string.settings_summary_streaming_cache_size,
                                    resolveOptionLabel(
                                        context = activity,
                                        entriesRes = R.array.streaming_cache_size_titles,
                                        valuesRes = R.array.streaming_cache_size_values,
                                        selectedValue = getStringPref(activity, KEY_STREAMING_CACHE_SIZE, DEFAULT_STREAMING_CACHE_SIZE),
                                    ),
                                    (DownloadUtil.getStreamingCacheSize(activity) / (1024 * 1024)).toString(),
                                ),
                            ),
                        )
                        add(selectItem(activity, KEY_IMAGE_CACHE_SIZE, R.string.settings_covers_cache, R.array.pref_cache_size_titles, R.array.pref_cache_size_values, getStringPref(activity, KEY_IMAGE_CACHE_SIZE, DEFAULT_IMAGE_CACHE_SIZE)))
                        add(selectItem(activity, KEY_IMAGE_SIZE, R.string.settings_image_size, R.array.pref_image_size_titles, R.array.pref_image_size_values, getStringPref(activity, KEY_IMAGE_SIZE, DEFAULT_IMAGE_SIZE)))
                        add(toggleItem(activity, KEY_WIFI_ONLY, R.string.settings_wifi_only_title, R.string.settings_wifi_only_summary, getBooleanPref(activity, KEY_WIFI_ONLY, false)))
                        add(toggleItem(activity, KEY_DOWNLOAD_WIFI_ONLY, R.string.settings_download_wifi_only_title, R.string.settings_download_wifi_only_summary, getBooleanPref(activity, KEY_DOWNLOAD_WIFI_ONLY, false)))
                        add(toggleItem(activity, KEY_DATA_SAVING_MODE, R.string.settings_data_saving_mode_title, R.string.settings_data_saving_mode_summary, getBooleanPref(activity, KEY_DATA_SAVING_MODE, false)))
                        add(toggleItem(activity, KEY_SYNC_STARRED_TRACKS, R.string.settings_sync_starred_tracks_for_offline_use_title, R.string.settings_sync_starred_tracks_for_offline_use_summary, getBooleanPref(activity, KEY_SYNC_STARRED_TRACKS, false)))
                        add(toggleItem(activity, KEY_SYNC_STARRED_ALBUMS, R.string.settings_sync_starred_albums_for_offline_use_title, R.string.settings_sync_starred_albums_for_offline_use_summary, getBooleanPref(activity, KEY_SYNC_STARRED_ALBUMS, false)))
                        add(toggleItem(activity, KEY_SYNC_STARRED_ARTISTS, R.string.settings_sync_starred_artists_for_offline_use_title, R.string.settings_sync_starred_artists_for_offline_use_summary, getBooleanPref(activity, KEY_SYNC_STARRED_ARTISTS, false)))
                        val preloadValue = getStringPref(activity, KEY_SONG_PRELOAD_BUFFER, DEFAULT_SONG_PRELOAD_BUFFER)
                        add(
                            selectItem(
                                context = activity,
                                key = KEY_SONG_PRELOAD_BUFFER,
                                titleRes = R.string.settings_song_preload_buffer,
                                entriesRes = R.array.song_preload_buffer_titles,
                                valuesRes = R.array.song_preload_buffer_values,
                                selectedValue = preloadValue,
                                summary = activity.getString(
                                    R.string.settings_song_preload_buffer_summary,
                                    resolveOptionLabel(
                                        context = activity,
                                        entriesRes = R.array.song_preload_buffer_titles,
                                        valuesRes = R.array.song_preload_buffer_values,
                                        selectedValue = preloadValue,
                                    ),
                                ),
                            ),
                        )
                        if (hasExternalStorage(activity)) {
                            add(
                                SettingsActionItemUiModel(
                                    key = KEY_STREAMING_CACHE_STORAGE,
                                    title = activity.getString(R.string.settings_streaming_cache_storage_title),
                                    value = getStreamingCacheStorageSummary(activity),
                                ),
                            )
                            if (Preferences.getDownloadDirectoryUri() == null) {
                                add(
                                    SettingsActionItemUiModel(
                                        key = KEY_DOWNLOAD_STORAGE,
                                        title = activity.getString(R.string.settings_download_storage_title),
                                        value = getDownloadStorageSummary(activity),
                                    ),
                                )
                            }
                        }
                        buildDownloadDirectoryItem(activity)?.let(::add)
                        add(
                            SettingsActionItemUiModel(
                                key = KEY_DELETE_DOWNLOAD_STORAGE,
                                title = activity.getString(R.string.settings_delete_download_storage_title),
                                summary = activity.getString(R.string.settings_delete_download_storage_summary),
                                destructive = true,
                            ),
                        )
                    },
                ),
            ),
            expandedSections = expandedSections,
        ),
    )
    add(
        section(
            key = SECTION_TRANSCODING,
            title = activity.getString(R.string.settings_title_transcoding),
            groups = listOf(
                SettingsGroupUiModel(
                    items = listOf(
                        SettingsInfoItemUiModel("transcoding_info", activity.getString(R.string.settings_summary_transcoding)),
                        toggleItem(activity, KEY_AUDIO_TRANSCODE_PRIORITY, R.string.settings_audio_transcode_priority_title, R.string.settings_audio_transcode_priority_summary, getBooleanPref(activity, KEY_AUDIO_TRANSCODE_PRIORITY, false)),
                        selectItem(activity, KEY_AUDIO_TRANSCODE_FORMAT_WIFI, R.string.settings_audio_transcode_format_wifi, R.array.audio_transcode_format_wifi_list_titles, R.array.audio_transcode_format_wifi_list_values, getStringPref(activity, KEY_AUDIO_TRANSCODE_FORMAT_WIFI, DEFAULT_AUDIO_TRANSCODE_FORMAT)),
                        selectItem(activity, KEY_MAX_BITRATE_WIFI, R.string.settings_max_bitrate_wifi, R.array.max_bitrate_wifi_list_titles, R.array.max_bitrate_wifi_list_values, getStringPref(activity, KEY_MAX_BITRATE_WIFI, DEFAULT_MAX_BITRATE)),
                        selectItem(activity, KEY_AUDIO_TRANSCODE_FORMAT_MOBILE, R.string.settings_audio_transcode_format_mobile, R.array.audio_transcode_format_mobile_list_titles, R.array.audio_transcode_format_mobile_list_values, getStringPref(activity, KEY_AUDIO_TRANSCODE_FORMAT_MOBILE, DEFAULT_AUDIO_TRANSCODE_FORMAT)),
                        selectItem(activity, KEY_MAX_BITRATE_MOBILE, R.string.settings_max_bitrate_mobile, R.array.max_bitrate_mobile_list_titles, R.array.max_bitrate_mobile_list_values, getStringPref(activity, KEY_MAX_BITRATE_MOBILE, DEFAULT_MAX_BITRATE)),
                    ),
                ),
                SettingsGroupUiModel(
                    title = activity.getString(R.string.settings_title_transcoding_download),
                    items = listOf(
                        SettingsInfoItemUiModel("transcoding_download_info", activity.getString(R.string.settings_summary_transcoding_download)),
                        toggleItem(activity, KEY_AUDIO_TRANSCODE_DOWNLOAD, R.string.settings_audio_transcode_download_title, R.string.settings_audio_transcode_download_summary, getBooleanPref(activity, KEY_AUDIO_TRANSCODE_DOWNLOAD, false)),
                        toggleItem(activity, KEY_AUDIO_TRANSCODE_DOWNLOAD_PRIORITY, R.string.settings_audio_transcode_download_priority_title, R.string.settings_audio_transcode_download_priority_summary, getBooleanPref(activity, KEY_AUDIO_TRANSCODE_DOWNLOAD_PRIORITY, false)),
                        selectItem(activity, KEY_AUDIO_TRANSCODE_FORMAT_DOWNLOAD, R.string.settings_audio_transcode_format_download, R.array.audio_transcode_format_download_list_titles, R.array.audio_transcode_format_download_list_values, getStringPref(activity, KEY_AUDIO_TRANSCODE_FORMAT_DOWNLOAD, DEFAULT_AUDIO_TRANSCODE_FORMAT)),
                        selectItem(activity, KEY_MAX_BITRATE_DOWNLOAD, R.string.settings_max_bitrate_download, R.array.max_bitrate_download_list_titles, R.array.max_bitrate_download_list_values, getStringPref(activity, KEY_MAX_BITRATE_DOWNLOAD, DEFAULT_MAX_BITRATE)),
                    ),
                ),
            ),
            expandedSections = expandedSections,
        ),
    )
    add(
        section(
            key = SECTION_MISC,
            title = activity.getString(R.string.settings_title_misc),
            groups = listOf(
                SettingsGroupUiModel(
                    title = activity.getString(R.string.settings_title_scrobble),
                    items = listOf(
                        SettingsInfoItemUiModel("scrobble_info_1", activity.getString(R.string.settings_summary_scrobble)),
                        SettingsInfoItemUiModel("scrobble_info_2", activity.getString(R.string.settings_sub_summary_scrobble)),
                        toggleItem(activity, KEY_SCROBBLING, R.string.settings_scrobble_title, null, getBooleanPref(activity, KEY_SCROBBLING, true)),
                    ),
                ),
                SettingsGroupUiModel(
                    title = activity.getString(R.string.settings_title_share),
                    items = listOf(
                        SettingsInfoItemUiModel("share_info", activity.getString(R.string.settings_summary_share)),
                        toggleItem(activity, KEY_SHARE, R.string.settings_share_title, null, getBooleanPref(activity, KEY_SHARE, false)),
                    ),
                ),
                SettingsGroupUiModel(
                    title = activity.getString(R.string.settings_title_syncing),
                    items = listOf(
                        SettingsInfoItemUiModel("queue_sync_info", activity.getString(R.string.settings_summary_syncing)),
                        toggleItem(activity, KEY_QUEUE_SYNCING, R.string.settings_queue_syncing_title, R.string.settings_queue_syncing_summary, getBooleanPref(activity, KEY_QUEUE_SYNCING, false)),
                        selectItem(activity, KEY_QUEUE_SYNCING_COUNTDOWN, R.string.settings_queue_syncing_countdown, R.array.queue_syncing_countdown_titles, R.array.queue_syncing_countdown_values, getStringPref(activity, KEY_QUEUE_SYNCING_COUNTDOWN, DEFAULT_QUEUE_SYNCING_COUNTDOWN)),
                    ),
                ),
            ),
            expandedSections = expandedSections,
        ),
    )
    if (BuildConfig.FLAVOR != "degoogled") {
        add(
            section(
                key = SECTION_ANDROID_AUTO,
                title = activity.getString(R.string.settings_androidauto),
                groups = listOf(
                    SettingsGroupUiModel(
                        items = listOf(
                            SettingsInfoItemUiModel("android_auto_info", activity.getString(R.string.home_rearrangement_dialog_subtitle)),
                            selectItem(activity, KEY_ANDROID_AUTO_FIRST_TAB, R.string.settings_androidauto_first_tab, R.array.aa_tab_titles, R.array.aa_tab_values, getStringPref(activity, KEY_ANDROID_AUTO_FIRST_TAB, DEFAULT_ANDROID_AUTO_FIRST_TAB)),
                            selectItem(activity, KEY_ANDROID_AUTO_SECOND_TAB, R.string.settings_androidauto_second_tab, R.array.aa_tab_titles, R.array.aa_tab_values, getStringPref(activity, KEY_ANDROID_AUTO_SECOND_TAB, DEFAULT_ANDROID_AUTO_SECOND_TAB)),
                            selectItem(activity, KEY_ANDROID_AUTO_THIRD_TAB, R.string.settings_androidauto_third_tab, R.array.aa_tab_titles, R.array.aa_tab_values, getStringPref(activity, KEY_ANDROID_AUTO_THIRD_TAB, DEFAULT_ANDROID_AUTO_THIRD_TAB)),
                            selectItem(activity, KEY_ANDROID_AUTO_FOURTH_TAB, R.string.settings_androidauto_fourth_tab, R.array.aa_tab_titles, R.array.aa_tab_values, getStringPref(activity, KEY_ANDROID_AUTO_FOURTH_TAB, DEFAULT_ANDROID_AUTO_FOURTH_TAB)),
                            toggleItem(activity, KEY_ANDROID_AUTO_HOME_VIEW, R.string.settings_androidauto_home_view, null, getBooleanPref(activity, KEY_ANDROID_AUTO_HOME_VIEW, false)),
                            toggleItem(activity, KEY_ANDROID_AUTO_ALBUM_VIEW, R.string.settings_androidauto_album_view, null, getBooleanPref(activity, KEY_ANDROID_AUTO_ALBUM_VIEW, true)),
                            toggleItem(activity, KEY_ANDROID_AUTO_PLAYLIST_VIEW, R.string.settings_androidauto_playlist_view, null, getBooleanPref(activity, KEY_ANDROID_AUTO_PLAYLIST_VIEW, false)),
                            toggleItem(activity, KEY_ANDROID_AUTO_SHUFFLE_GENRE_SONGS, R.string.settings_androidauto_shuffle_genre_songs, null, getBooleanPref(activity, KEY_ANDROID_AUTO_SHUFFLE_GENRE_SONGS, false)),
                            toggleItem(activity, KEY_ANDROID_AUTO_SHUFFLE_STARRED_TRACKS, R.string.settings_androidauto_shuffle_starred_tracks, null, getBooleanPref(activity, KEY_ANDROID_AUTO_SHUFFLE_STARRED_TRACKS, false)),
                            toggleItem(activity, KEY_ANDROID_AUTO_SHUFFLE_PLAYLISTS, R.string.settings_androidauto_shuffle_playlists, null, getBooleanPref(activity, KEY_ANDROID_AUTO_SHUFFLE_PLAYLISTS, false)),
                            selectItem(activity, KEY_ANDROID_AUTO_STARRED_FOR_MADE_FOR_YOU, R.string.settings_androidauto_starred_for_made_for_you, R.array.aa_starred_for_made_for_you_titles, R.array.aa_starred_for_made_for_you_values, getStringPref(activity, KEY_ANDROID_AUTO_STARRED_FOR_MADE_FOR_YOU, DEFAULT_ANDROID_AUTO_STARRED)),
                        ),
                    ),
                ),
                expandedSections = expandedSections,
            ),
        )
    }
    if (BuildConfig.FLAVOR == "tempus") {
        add(
            section(
                key = SECTION_GITHUB_UPDATE,
                title = activity.getString(R.string.settings_github_update),
                groups = listOf(
                    SettingsGroupUiModel(
                        items = listOf(
                            SettingsInfoItemUiModel("github_update_info", activity.getString(R.string.settings_github_update_summary)),
                            toggleItem(activity, KEY_GITHUB_UPDATE_CHECK, R.string.settings_github_update_title, null, getBooleanPref(activity, KEY_GITHUB_UPDATE_CHECK, true)),
                        ),
                    ),
                ),
                expandedSections = expandedSections,
            ),
        )
    }
    add(
        section(
            key = SECTION_ABOUT,
            title = activity.getString(R.string.settings_about_title),
            groups = listOf(
                SettingsGroupUiModel(
                    items = listOf(
                        SettingsInfoItemUiModel("about_info", activity.getString(R.string.settings_about_summary)),
                        SettingsValueItemUiModel(
                            key = KEY_VERSION,
                            title = activity.getString(R.string.settings_version_title),
                            value = BuildConfig.VERSION_NAME,
                        ),
                        SettingsActionItemUiModel(
                            key = KEY_ABOUT_GITHUB,
                            title = activity.getString(R.string.settings_github_title),
                            summary = activity.getString(R.string.settings_github_summary),
                        ),
                        SettingsActionItemUiModel(
                            key = KEY_ABOUT_SUPPORT,
                            title = activity.getString(R.string.settings_support_title),
                            summary = activity.getString(R.string.settings_support_summary),
                        ),
                        SettingsActionItemUiModel(
                            key = KEY_ABOUT_UNDRAW,
                            title = activity.getString(R.string.undraw_page),
                            summary = activity.getString(R.string.undraw_thanks),
                        ),
                    ),
                ),
            ),
            expandedSections = expandedSections,
        ),
    )
}

private fun section(
    key: String,
    title: String,
    groups: List<SettingsGroupUiModel>,
    expandedSections: Set<String>,
) = SettingsSectionUiModel(
    key = key,
    title = title,
    expanded = expandedSections.contains(key),
    groups = groups,
)

private fun toggleItem(
    context: Context,
    key: String,
    titleRes: Int,
    summaryRes: Int?,
    checked: Boolean,
) = SettingsToggleItemUiModel(
    key = key,
    title = context.getString(titleRes),
    summary = summaryRes?.let(context::getString),
    checked = checked,
)

private fun selectItem(
    context: Context,
    key: String,
    titleRes: Int,
    entriesRes: Int,
    valuesRes: Int,
    selectedValue: String,
    summary: String? = null,
) = SettingsSelectItemUiModel(
    key = key,
    title = context.getString(titleRes),
    summary = summary,
    selectedLabel = resolveOptionLabel(
        context = context,
        entriesRes = entriesRes,
        valuesRes = valuesRes,
        selectedValue = selectedValue,
    ),
)

private fun buildLanguageItem(
    context: Context,
): SettingsSelectItemUiModel {
    val locales = UIUtil.getLangPreferenceDropdownEntries(context)
    val currentValue = getStringPref(context, KEY_LANGUAGE, DEFAULT_LANGUAGE)
    val label = if (currentValue == DEFAULT_LANGUAGE) {
        context.getString(R.string.settings_system_language)
    } else {
        locales.entries.firstOrNull { it.value == currentValue }?.key
            ?: Locale.forLanguageTag(currentValue).displayName
    }
    return SettingsSelectItemUiModel(
        key = KEY_LANGUAGE,
        title = context.getString(R.string.settings_language),
        selectedLabel = label,
    )
}

private fun resolveOptionLabel(
    context: Context,
    entriesRes: Int,
    valuesRes: Int,
    selectedValue: String,
): String {
    val entries = context.resources.getStringArray(entriesRes)
    val values = context.resources.getStringArray(valuesRes)
    val index = values.indexOf(selectedValue)
    return entries.getOrElse(index.coerceAtLeast(0)) { entries.firstOrNull().orEmpty() }
}

private fun selectionOptionsFor(
    context: Context,
    key: String,
): List<SettingsOptionUiModel> {
    return when (key) {
        KEY_LANGUAGE -> UIUtil.getLangPreferenceDropdownEntries(context)
            .map { (title, value) -> SettingsOptionUiModel(title = title, value = value) }

        KEY_THEME -> arrayOptions(context, R.array.theme_list_titles, R.array.theme_list_values)
        KEY_DARK_THEME_STYLE -> arrayOptions(context, R.array.dark_theme_style_titles, R.array.dark_theme_style_values)
        KEY_TILE_SIZE -> arrayOptions(context, R.array.tile_size_titles, R.array.tile_size_divisor)
        KEY_ROUNDED_CORNER_SIZE -> arrayOptions(context, R.array.rounded_corner_size_titles, R.array.rounded_corner_size_values)
        KEY_SELECTED_EQUALIZER -> arrayOptions(context, R.array.selected_equalizer_entries, R.array.selected_equalizer_values)
        KEY_REPLAY_GAIN_MODE -> arrayOptions(context, R.array.replay_gain_titles, R.array.replay_gain_values)
        KEY_CUSTOM_COMMAND_FIRST_BUTTON, KEY_CUSTOM_COMMAND_SECOND_BUTTON -> arrayOptions(context, R.array.custom_commands_titles, R.array.custom_commands_values)
        KEY_HOME_SORT_PLAYLISTS -> arrayOptions(context, R.array.playlist_sort_option_titles, R.array.playlist_sort_option_values)
        KEY_STREAMING_CACHE_SIZE -> arrayOptions(context, R.array.streaming_cache_size_titles, R.array.streaming_cache_size_values)
        KEY_IMAGE_CACHE_SIZE -> arrayOptions(context, R.array.pref_cache_size_titles, R.array.pref_cache_size_values)
        KEY_IMAGE_SIZE -> arrayOptions(context, R.array.pref_image_size_titles, R.array.pref_image_size_values)
        KEY_SONG_PRELOAD_BUFFER -> arrayOptions(context, R.array.song_preload_buffer_titles, R.array.song_preload_buffer_values)
        KEY_AUDIO_TRANSCODE_FORMAT_WIFI -> arrayOptions(context, R.array.audio_transcode_format_wifi_list_titles, R.array.audio_transcode_format_wifi_list_values)
        KEY_MAX_BITRATE_WIFI -> arrayOptions(context, R.array.max_bitrate_wifi_list_titles, R.array.max_bitrate_wifi_list_values)
        KEY_AUDIO_TRANSCODE_FORMAT_MOBILE -> arrayOptions(context, R.array.audio_transcode_format_mobile_list_titles, R.array.audio_transcode_format_mobile_list_values)
        KEY_MAX_BITRATE_MOBILE -> arrayOptions(context, R.array.max_bitrate_mobile_list_titles, R.array.max_bitrate_mobile_list_values)
        KEY_AUDIO_TRANSCODE_FORMAT_DOWNLOAD -> arrayOptions(context, R.array.audio_transcode_format_download_list_titles, R.array.audio_transcode_format_download_list_values)
        KEY_MAX_BITRATE_DOWNLOAD -> arrayOptions(context, R.array.max_bitrate_download_list_titles, R.array.max_bitrate_download_list_values)
        KEY_QUEUE_SYNCING_COUNTDOWN -> arrayOptions(context, R.array.queue_syncing_countdown_titles, R.array.queue_syncing_countdown_values)
        KEY_ANDROID_AUTO_FIRST_TAB, KEY_ANDROID_AUTO_SECOND_TAB, KEY_ANDROID_AUTO_THIRD_TAB, KEY_ANDROID_AUTO_FOURTH_TAB -> arrayOptions(context, R.array.aa_tab_titles, R.array.aa_tab_values)
        KEY_ANDROID_AUTO_STARRED_FOR_MADE_FOR_YOU -> arrayOptions(context, R.array.aa_starred_for_made_for_you_titles, R.array.aa_starred_for_made_for_you_values)
        else -> emptyList()
    }
}

private fun currentSelectedValueFor(
    context: Context,
    key: String,
): String {
    val default = when (key) {
        KEY_THEME -> ThemeHelper.DEFAULT_MODE
        KEY_DARK_THEME_STYLE -> DEFAULT_DARK_THEME_STYLE
        KEY_TILE_SIZE -> DEFAULT_TILE_SIZE
        KEY_SELECTED_EQUALIZER -> DEFAULT_SELECTED_EQUALIZER
        KEY_REPLAY_GAIN_MODE -> DEFAULT_REPLAY_GAIN_MODE
        KEY_CUSTOM_COMMAND_FIRST_BUTTON -> DEFAULT_FIRST_CUSTOM_COMMAND
        KEY_CUSTOM_COMMAND_SECOND_BUTTON -> DEFAULT_SECOND_CUSTOM_COMMAND
        KEY_HOME_SORT_PLAYLISTS -> DEFAULT_PLAYLIST_SORT
        KEY_STREAMING_CACHE_SIZE -> DEFAULT_STREAMING_CACHE_SIZE
        KEY_IMAGE_CACHE_SIZE -> DEFAULT_IMAGE_CACHE_SIZE
        KEY_IMAGE_SIZE -> DEFAULT_IMAGE_SIZE
        KEY_SONG_PRELOAD_BUFFER -> DEFAULT_SONG_PRELOAD_BUFFER
        KEY_AUDIO_TRANSCODE_FORMAT_WIFI,
        KEY_AUDIO_TRANSCODE_FORMAT_MOBILE,
        KEY_AUDIO_TRANSCODE_FORMAT_DOWNLOAD -> DEFAULT_AUDIO_TRANSCODE_FORMAT
        KEY_MAX_BITRATE_WIFI,
        KEY_MAX_BITRATE_MOBILE,
        KEY_MAX_BITRATE_DOWNLOAD -> DEFAULT_MAX_BITRATE
        KEY_QUEUE_SYNCING_COUNTDOWN -> DEFAULT_QUEUE_SYNCING_COUNTDOWN
        KEY_ANDROID_AUTO_FIRST_TAB -> DEFAULT_ANDROID_AUTO_FIRST_TAB
        KEY_ANDROID_AUTO_SECOND_TAB -> DEFAULT_ANDROID_AUTO_SECOND_TAB
        KEY_ANDROID_AUTO_THIRD_TAB -> DEFAULT_ANDROID_AUTO_THIRD_TAB
        KEY_ANDROID_AUTO_FOURTH_TAB -> DEFAULT_ANDROID_AUTO_FOURTH_TAB
        KEY_ANDROID_AUTO_STARRED_FOR_MADE_FOR_YOU -> DEFAULT_ANDROID_AUTO_STARRED
        KEY_LANGUAGE -> DEFAULT_LANGUAGE
        else -> ""
    }
    return getStringPref(context, key, default)
}

private fun arrayOptions(
    context: Context,
    entriesRes: Int,
    valuesRes: Int,
): List<SettingsOptionUiModel> {
    val entries = context.resources.getStringArray(entriesRes)
    val values = context.resources.getStringArray(valuesRes)
    return entries.zip(values).map { (title, value) ->
        SettingsOptionUiModel(title = title, value = value)
    }
}

private fun isDarkThemeVisible(
    configuration: Configuration,
    themeOption: String,
): Boolean {
    return when (themeOption) {
        ThemeHelper.DARK_MODE, ThemeHelper.AMOLED_MODE -> true
        ThemeHelper.DEFAULT_MODE -> {
            val nightMode = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            nightMode == Configuration.UI_MODE_NIGHT_YES
        }

        else -> false
    }
}

private fun hasExternalStorage(
    context: Context,
): Boolean {
    return try {
        context.getExternalFilesDirs(null).getOrNull(1) != null
    } catch (_: Exception) {
        false
    }
}

private fun isSystemEqualizerAvailable(
    context: Context,
): Boolean {
    val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
    return intent.resolveActivity(context.packageManager) != null
}

private fun isBuiltinEqualizerVisible(
    mediaServiceBinder: BaseMediaService.LocalBinder?,
): Boolean {
    val equalizerManager: EqualizerManager = mediaServiceBinder?.getEqualizerManager() ?: return false
    return equalizerManager.getNumberOfBands() > 0
}

private fun getStreamingCacheStorageSummary(
    context: Context,
): String {
    return if (Preferences.getStreamingCacheStoragePreference() == 0) {
        context.getString(R.string.streaming_cache_storage_internal_dialog_negative_button)
    } else {
        context.getString(R.string.streaming_cache_storage_external_dialog_positive_button)
    }
}

private fun getDownloadStorageSummary(
    context: Context,
): String {
    return when (Preferences.getDownloadStoragePreference()) {
        0 -> context.getString(R.string.download_storage_internal_dialog_negative_button)
        1 -> context.getString(R.string.download_storage_external_dialog_positive_button)
        else -> context.getString(R.string.download_storage_directory_dialog_neutral_button)
    }
}

private fun buildDownloadDirectoryItem(
    context: Context,
): SettingsActionItemUiModel? {
    val current = Preferences.getDownloadDirectoryUri()
    if (current != null) {
        return SettingsActionItemUiModel(
            key = KEY_SET_DOWNLOAD_DIRECTORY,
            title = context.getString(R.string.settings_clear_download_folder),
            summary = current,
            destructive = true,
        )
    }

    if (Preferences.getDownloadStoragePreference() != 2) {
        return null
    }

    return SettingsActionItemUiModel(
        key = KEY_SET_DOWNLOAD_DIRECTORY,
        title = context.getString(R.string.settings_set_download_folder),
        summary = context.getString(R.string.settings_choose_download_folder),
    )
}

private fun getBooleanPref(
    context: Context,
    key: String,
    defaultValue: Boolean,
): Boolean {
    return PreferenceManager.getDefaultSharedPreferences(context)
        .getBoolean(key, defaultValue)
}

private fun putBooleanPref(
    context: Context,
    key: String,
    value: Boolean,
) {
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putBoolean(key, value)
        .apply()
}

private fun getStringPref(
    context: Context,
    key: String,
    defaultValue: String,
): String {
    return PreferenceManager.getDefaultSharedPreferences(context)
        .getString(key, defaultValue)
        ?: defaultValue
}

private fun putStringPref(
    context: Context,
    key: String,
    value: String,
) {
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putString(key, value)
        .apply()
}

private fun getIntPref(
    context: Context,
    key: String,
    defaultValue: Int,
): Int {
    return PreferenceManager.getDefaultSharedPreferences(context)
        .getInt(key, defaultValue)
}

private fun putIntPref(
    context: Context,
    key: String,
    value: Int,
) {
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putInt(key, value)
        .apply()
}

private const val DEFAULT_LANGUAGE = "default"
private const val DEFAULT_DARK_THEME_STYLE = "standard"
private const val DEFAULT_TILE_SIZE = "2"
private const val DEFAULT_ROUNDED_CORNER_SIZE = "6"
private const val DEFAULT_SELECTED_EQUALIZER = "0"
private const val DEFAULT_REPLAY_GAIN_MODE = "disabled"
private const val DEFAULT_FIRST_CUSTOM_COMMAND = "[heartID]"
private const val DEFAULT_SECOND_CUSTOM_COMMAND = "[repeatID]"
private const val DEFAULT_PLAYLIST_SORT = "ORDER_BY_NAME"
private const val DEFAULT_STREAMING_CACHE_SIZE = "256"
private const val DEFAULT_IMAGE_CACHE_SIZE = "500"
private const val DEFAULT_IMAGE_SIZE = "-1"
private const val DEFAULT_SONG_PRELOAD_BUFFER = "60"
private const val DEFAULT_AUDIO_TRANSCODE_FORMAT = "raw"
private const val DEFAULT_MAX_BITRATE = "0"
private const val DEFAULT_QUEUE_SYNCING_COUNTDOWN = "5"
private const val DEFAULT_ANDROID_AUTO_FIRST_TAB = "0"
private const val DEFAULT_ANDROID_AUTO_SECOND_TAB = "1"
private const val DEFAULT_ANDROID_AUTO_THIRD_TAB = "2"
private const val DEFAULT_ANDROID_AUTO_FOURTH_TAB = "3"
private const val DEFAULT_ANDROID_AUTO_STARRED = "0"

private const val SECTION_GENERAL = "general"
private const val SECTION_UI = "ui"
private const val SECTION_SOUND = "sound"
private const val SECTION_PLAYERS = "players"
private const val SECTION_PLAYLIST = "playlist"
private const val SECTION_DATA = "data"
private const val SECTION_TRANSCODING = "transcoding"
private const val SECTION_MISC = "misc"
private const val SECTION_ANDROID_AUTO = "android_auto"
private const val SECTION_GITHUB_UPDATE = "github_update"
private const val SECTION_ABOUT = "about"

private const val KEY_LOGOUT = "logout"
private const val KEY_SCAN_LIBRARY = "scan_library"
private const val KEY_NETWORK_PING_TIMEOUT = "network_ping_timeout_base"
private const val KEY_LANGUAGE = "language"
private const val KEY_THEME = "theme"
private const val KEY_DARK_THEME_STYLE = "dark_theme_style"
private const val KEY_ALWAYS_ON_DISPLAY = "always_on_display"
private const val KEY_TILE_SIZE = "tile_size"
private const val KEY_ENABLE_DRAWER_ON_PORTRAIT = "enable_drawer_on_portrait"
private const val KEY_HIDE_BOTTOM_NAVBAR_ON_PORTRAIT = "hide_bottom_navbar_on_portrait"
private const val KEY_ROUNDED_CORNER = "rounded_corner"
private const val KEY_ROUNDED_CORNER_SIZE = "rounded_corner_size"
private const val KEY_AUDIO_QUALITY_PER_ITEM = "audio_quality_per_item"
private const val KEY_SONG_RATING_PER_ITEM = "song_rating_per_item"
private const val KEY_TRACK_NUMBER_VISIBLE = "track_number_visible"
private const val KEY_RATING_PER_ITEM = "rating_per_item"
private const val KEY_AUTO_DOWNLOAD_LYRICS = "auto_download_lyrics"
private const val KEY_MUSIC_DIRECTORY_SECTION_VISIBILITY = "music_directory_section_visibility"
private const val KEY_ALBUM_DETAIL = "album_detail"
private const val KEY_ARTIST_SORT_BY_ALBUM_COUNT = "artist_sort_by_album_count"
private const val KEY_SORT_SEARCH_CHRONOLOGICALLY = "sort_search_chronologically"
private const val KEY_SELECTED_EQUALIZER = "selected_equalizer"
private const val KEY_SYSTEM_EQUALIZER = "system_equalizer"
private const val KEY_BUILTIN_EQUALIZER = "builtin_equalizer"
private const val KEY_REPLAY_GAIN_MODE = "replay_gain_mode"
private const val KEY_REPLAY_GAIN_PREVENT_CLIPPING = "replay_gain_prevent_clipping"
private const val KEY_LOUDNESS_PREAMP = "loudness_preamp"
private const val KEY_CONTINUOUS_PLAY = "continuous_play"
private const val KEY_CUSTOM_COMMAND_FIRST_BUTTON = "custom_command_first_button"
private const val KEY_CUSTOM_COMMAND_SECOND_BUTTON = "custom_command_second_button"
private const val KEY_MIN_STAR_RATING = "min_star_rating"
private const val KEY_SHARE_PLAYBACK_DIAGNOSTICS = "share_playback_diagnostics"
private const val KEY_CLEAR_PLAYBACK_DIAGNOSTICS = "clear_playback_diagnostics"
private const val KEY_ALLOW_PLAYLIST_DUPLICATES = "allow_playlist_duplicates"
private const val KEY_HOME_SORT_PLAYLISTS = "home_sort_playlists"
private const val KEY_STREAMING_CACHE_SIZE = "streaming_cache_size"
private const val KEY_IMAGE_CACHE_SIZE = "image_cache_size"
private const val KEY_IMAGE_SIZE = "image_size"
private const val KEY_WIFI_ONLY = "wifi_only"
private const val KEY_DOWNLOAD_WIFI_ONLY = "download_wifi_only"
private const val KEY_DATA_SAVING_MODE = "data_saving_mode"
private const val KEY_SYNC_STARRED_TRACKS = "sync_starred_tracks_for_offline_use"
private const val KEY_SYNC_STARRED_ALBUMS = "sync_starred_albums_for_offline_use"
private const val KEY_SYNC_STARRED_ARTISTS = "sync_starred_artists_for_offline_use"
private const val KEY_SONG_PRELOAD_BUFFER = "song_preload_buffer"
private const val KEY_STREAMING_CACHE_STORAGE = "streaming_cache_storage"
private const val KEY_DOWNLOAD_STORAGE = "download_storage"
private const val KEY_SET_DOWNLOAD_DIRECTORY = "set_download_directory"
private const val KEY_DELETE_DOWNLOAD_STORAGE = "delete_download_storage"
private const val KEY_AUDIO_TRANSCODE_PRIORITY = "audio_transcode_priority"
private const val KEY_AUDIO_TRANSCODE_FORMAT_WIFI = "audio_transcode_format_wifi"
private const val KEY_MAX_BITRATE_WIFI = "max_bitrate_wifi"
private const val KEY_AUDIO_TRANSCODE_FORMAT_MOBILE = "audio_transcode_format_mobile"
private const val KEY_MAX_BITRATE_MOBILE = "max_bitrate_mobile"
private const val KEY_AUDIO_TRANSCODE_DOWNLOAD = "audio_transcode_download"
private const val KEY_AUDIO_TRANSCODE_DOWNLOAD_PRIORITY = "audio_transcode_download_priority"
private const val KEY_AUDIO_TRANSCODE_FORMAT_DOWNLOAD = "audio_transcode_format_download"
private const val KEY_MAX_BITRATE_DOWNLOAD = "max_bitrate_download"
private const val KEY_SCROBBLING = "scrobbling"
private const val KEY_SHARE = "share"
private const val KEY_QUEUE_SYNCING = "queue_syncing"
private const val KEY_QUEUE_SYNCING_COUNTDOWN = "queue_syncing_countdown"
private const val KEY_ANDROID_AUTO_FIRST_TAB = "androidauto_first_tab"
private const val KEY_ANDROID_AUTO_SECOND_TAB = "androidauto_second_tab"
private const val KEY_ANDROID_AUTO_THIRD_TAB = "androidauto_third_tab"
private const val KEY_ANDROID_AUTO_FOURTH_TAB = "androidauto_fourth_tab"
private const val KEY_ANDROID_AUTO_HOME_VIEW = "androidauto_home_view"
private const val KEY_ANDROID_AUTO_ALBUM_VIEW = "androidauto_album_view"
private const val KEY_ANDROID_AUTO_PLAYLIST_VIEW = "androidauto_playlist_view"
private const val KEY_ANDROID_AUTO_SHUFFLE_GENRE_SONGS = "androidauto_shuffle_genre_songs"
private const val KEY_ANDROID_AUTO_SHUFFLE_STARRED_TRACKS = "androidauto_shuffle_starred_tracks"
private const val KEY_ANDROID_AUTO_SHUFFLE_PLAYLISTS = "androidauto_shuffle_playlists"
private const val KEY_ANDROID_AUTO_STARRED_FOR_MADE_FOR_YOU = "androidauto_starred_for_made_for_you"
private const val KEY_GITHUB_UPDATE_CHECK = "github_update_check"
private const val KEY_VERSION = "version"
private const val KEY_ABOUT_GITHUB = "about_github"
private const val KEY_ABOUT_SUPPORT = "about_support"
private const val KEY_ABOUT_UNDRAW = "about_undraw"
private const val KEY_MINI_SHUFFLE_BUTTON_VISIBILITY = "mini_shuffle_button_visibility"
