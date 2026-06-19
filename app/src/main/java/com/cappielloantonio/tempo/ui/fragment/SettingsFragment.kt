package com.cappielloantonio.tempo.ui.fragment

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.content.res.Resources
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Requirements
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager
import com.cappielloantonio.tempo.BuildConfig
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.equalizer.EqualizerManager
import com.cappielloantonio.tempo.helper.ThemeHelper
import com.cappielloantonio.tempo.interfaces.DialogClickCallback
import com.cappielloantonio.tempo.interfaces.ScanCallback
import com.cappielloantonio.tempo.service.DownloaderService
import com.cappielloantonio.tempo.service.BaseMediaService
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.dialog.DeleteDownloadStorageDialog
import com.cappielloantonio.tempo.ui.dialog.DownloadStorageDialog
import com.cappielloantonio.tempo.ui.dialog.StarredAlbumSyncDialog
import com.cappielloantonio.tempo.ui.dialog.StarredArtistSyncDialog
import com.cappielloantonio.tempo.ui.dialog.StarredSyncDialog
import com.cappielloantonio.tempo.ui.dialog.StreamingCacheStorageDialog
import com.cappielloantonio.tempo.ui.settings.SettingsActionItemUiModel
import com.cappielloantonio.tempo.ui.settings.SettingsGroupUiModel
import com.cappielloantonio.tempo.ui.settings.SettingsInfoItemUiModel
import com.cappielloantonio.tempo.ui.settings.SettingsInputItemUiModel
import com.cappielloantonio.tempo.ui.settings.SettingsItemUiModel
import com.cappielloantonio.tempo.ui.settings.SettingsOptionUiModel
import com.cappielloantonio.tempo.ui.settings.SettingsScreen
import com.cappielloantonio.tempo.ui.settings.SettingsSectionUiModel
import com.cappielloantonio.tempo.ui.settings.SettingsSelectItemUiModel
import com.cappielloantonio.tempo.ui.settings.SettingsSelectionDialogState
import com.cappielloantonio.tempo.ui.settings.SettingsSliderItemUiModel
import com.cappielloantonio.tempo.ui.settings.SettingsTextInputDialogState
import com.cappielloantonio.tempo.ui.settings.SettingsToggleItemUiModel
import com.cappielloantonio.tempo.ui.settings.SettingsUiState
import com.cappielloantonio.tempo.ui.settings.SettingsValueItemUiModel
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.ExternalAudioReader
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.util.ReplayGainUtil
import com.cappielloantonio.tempo.util.UIUtil
import com.cappielloantonio.tempo.viewmodel.SettingViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale

@OptIn(UnstableApi::class)
class SettingsFragment : Fragment() {

    private val settingViewModel: SettingViewModel by viewModel()

    private lateinit var directoryPickerLauncher: ActivityResultLauncher<Intent>

    private var mediaServiceBinder: BaseMediaService.LocalBinder? = null
    private var isServiceBound = false
    private val expandedSections = linkedSetOf<String>()
    private var scanSummary: String? = null
    private var uiState by mutableStateOf(SettingsUiState())

    private val isLandscape: Boolean
        get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    private val hostActivity: MainActivity
        get() = requireActivity() as MainActivity

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mediaServiceBinder = service as? BaseMediaService.LocalBinder
            isServiceBound = true
            refreshUiState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mediaServiceBinder = null
            isServiceBound = false
            refreshUiState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        directoryPickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data ?: return@registerForActivityResult
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                Preferences.setDownloadDirectoryUri(uri.toString())
                ExternalAudioReader.refreshCache()
                Toast.makeText(requireContext(), R.string.settings_download_folder_set, Toast.LENGTH_SHORT).show()
                refreshUiState()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        refreshUiState()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    SettingsScreen(
                        uiState = uiState,
                        onNavigateBack = ::navigateBack,
                        onActionClick = ::handleActionClick,
                        onToggleChange = ::handleToggleChange,
                        onSelectClick = ::showSelectionDialog,
                        onSliderChange = ::handleSliderChange,
                        onInputClick = ::showTextInputDialog,
                        onSelectionDismiss = { uiState = uiState.copy(selectionDialog = null) },
                        onSelectionConfirm = ::handleSelectionConfirmed,
                        onTextInputDismiss = { uiState = uiState.copy(textInputDialog = null) },
                        onTextInputConfirm = ::handleTextInputConfirmed,
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        hostActivity.setBottomNavigationBarVisibility(false)
        hostActivity.setBottomSheetVisibility(false)
        hostActivity.setNavigationDrawerLock(true)
        hostActivity.setSystemBarsVisibility(!isLandscape)
    }

    override fun onResume() {
        super.onResume()
        bindMediaService()
        refreshUiState()
    }

    override fun onPause() {
        if (isServiceBound) {
            requireActivity().unbindService(serviceConnection)
            isServiceBound = false
        }
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        hostActivity.setBottomSheetVisibility(true)
        if (isLandscape || Preferences.getEnableDrawerOnPortrait()) {
            hostActivity.setNavigationDrawerLock(false)
        }
    }

    private fun refreshUiState() {
        uiState = uiState.copy(
            sections = buildSections()
        )
    }

    private fun buildSections(): List<SettingsSectionUiModel> = buildList {
        add(buildGeneralSection())
        add(buildUiSection())
        add(buildSoundSection())
        add(buildPlayersSection())
        add(buildPlaylistSection())
        add(buildDataSection())
        add(buildTranscodingSection())
        add(buildMiscSection())
        if (BuildConfig.FLAVOR != "degoogled") {
            add(buildAndroidAutoSection())
        }
        if (BuildConfig.FLAVOR == "tempus") {
            add(buildGithubUpdateSection())
        }
        add(buildAboutSection())
    }

    private fun buildGeneralSection(): SettingsSectionUiModel {
        return section(
            key = SECTION_GENERAL,
            title = getString(R.string.settings_title_general),
            groups = listOf(
                SettingsGroupUiModel(
                    items = listOf(
                        SettingsActionItemUiModel(
                            key = KEY_LOGOUT,
                            title = getString(R.string.settings_logout_title),
                            destructive = true
                        ),
                        SettingsActionItemUiModel(
                            key = KEY_SCAN_LIBRARY,
                            title = getString(R.string.settings_scan_title),
                            summary = scanSummary
                        ),
                        SettingsInputItemUiModel(
                            key = KEY_NETWORK_PING_TIMEOUT,
                            title = getString(R.string.settings_ping_timeout_title),
                            summary = getString(R.string.settings_ping_timeout_summary),
                            value = Preferences.getNetworkPingTimeout().toString()
                        )
                    )
                )
            )
        )
    }

    private fun buildUiSection(): SettingsSectionUiModel {
        val items = buildList<SettingsItemUiModel> {
            add(buildLanguageItem())
            add(selectItem(KEY_THEME, R.string.settings_theme, R.array.theme_list_titles, R.array.theme_list_values, getStringPref(KEY_THEME, ThemeHelper.DEFAULT_MODE)))
            if (isDarkThemeVisible(getStringPref(KEY_THEME, ThemeHelper.DEFAULT_MODE))) {
                add(selectItem(KEY_DARK_THEME_STYLE, R.string.settings_dark_theme_style, R.array.dark_theme_style_titles, R.array.dark_theme_style_values, getStringPref(KEY_DARK_THEME_STYLE, "standard")))
            }
            add(toggleItem(KEY_ALWAYS_ON_DISPLAY, R.string.settings_always_on_display, null, getBooleanPref(KEY_ALWAYS_ON_DISPLAY, false)))
            add(selectItem(KEY_TILE_SIZE, R.string.settings_tile_size, R.array.tile_size_titles, R.array.tile_size_divisor, getStringPref(KEY_TILE_SIZE, "2")))
            add(toggleItem(KEY_ENABLE_DRAWER_ON_PORTRAIT, R.string.settings_enable_drawer_on_landscape, R.string.settings_enable_drawer_on_landscape_summary, getBooleanPref(KEY_ENABLE_DRAWER_ON_PORTRAIT, false)))
            add(toggleItem(KEY_HIDE_BOTTOM_NAVBAR_ON_PORTRAIT, R.string.settings_hide_bottom_navbar_on_portrait, R.string.settings_hide_bottom_navbar_on_portrait_summary, getBooleanPref(KEY_HIDE_BOTTOM_NAVBAR_ON_PORTRAIT, false)))
            add(toggleItem(KEY_ROUNDED_CORNER, R.string.settings_rounded_corner, R.string.settings_rounded_corner_summary, getBooleanPref(KEY_ROUNDED_CORNER, true)))
            add(selectItem(KEY_ROUNDED_CORNER_SIZE, R.string.settings_rounded_corner_size, R.array.rounded_corner_size_titles, R.array.rounded_corner_size_values, getStringPref(KEY_ROUNDED_CORNER_SIZE, "6")))
            add(toggleItem(KEY_AUDIO_QUALITY_PER_ITEM, R.string.settings_audio_quality, R.string.settings_audio_quality_summary, getBooleanPref(KEY_AUDIO_QUALITY_PER_ITEM, false)))
            add(toggleItem(KEY_SONG_RATING_PER_ITEM, R.string.settings_song_rating, R.string.settings_song_rating_summary, getBooleanPref(KEY_SONG_RATING_PER_ITEM, false)))
            add(toggleItem(KEY_TRACK_NUMBER_VISIBLE, R.string.settings_track_number, R.string.settings_track_number_summary, getBooleanPref(KEY_TRACK_NUMBER_VISIBLE, false)))
            add(toggleItem(KEY_RATING_PER_ITEM, R.string.settings_item_rating, R.string.settings_item_rating_summary, getBooleanPref(KEY_RATING_PER_ITEM, false)))
            add(toggleItem(KEY_PODCAST_SECTION_VISIBILITY, R.string.settings_podcast, R.string.settings_podcast_summary, getBooleanPref(KEY_PODCAST_SECTION_VISIBILITY, true)))
            add(toggleItem(KEY_RADIO_SECTION_VISIBILITY, R.string.settings_radio, R.string.settings_radio_summary, getBooleanPref(KEY_RADIO_SECTION_VISIBILITY, true)))
            add(toggleItem(KEY_AUTO_DOWNLOAD_LYRICS, R.string.settings_auto_download_lyrics, R.string.settings_auto_download_lyrics_summary, Preferences.isAutoDownloadLyricsEnabled()))
            add(toggleItem(KEY_MUSIC_DIRECTORY_SECTION_VISIBILITY, R.string.settings_music_directory, R.string.settings_music_directory_summary, getBooleanPref(KEY_MUSIC_DIRECTORY_SECTION_VISIBILITY, true)))
            add(toggleItem(KEY_ALBUM_DETAIL, R.string.settings_album_detail, R.string.settings_album_detail_summary, getBooleanPref(KEY_ALBUM_DETAIL, false)))
            add(toggleItem(KEY_ARTIST_SORT_BY_ALBUM_COUNT, R.string.settings_artist_sort_by_album_count, R.string.settings_artist_sort_by_album_count_summary, getBooleanPref(KEY_ARTIST_SORT_BY_ALBUM_COUNT, false)))
            add(toggleItem(KEY_SORT_SEARCH_CHRONOLOGICALLY, R.string.search_sort_title, R.string.search_sort_summary, getBooleanPref(KEY_SORT_SEARCH_CHRONOLOGICALLY, false)))
        }

        return section(
            key = SECTION_UI,
            title = getString(R.string.settings_title_ui),
            groups = listOf(SettingsGroupUiModel(items = items))
        )
    }

    private fun buildSoundSection(): SettingsSectionUiModel {
        val groups = buildList {
            add(
                SettingsGroupUiModel(
                    title = getString(R.string.settings_title_equalizer),
                    items = buildList {
                        add(selectItem(KEY_SELECTED_EQUALIZER, R.string.settings_equalizer_select, R.array.selected_equalizer_entries, R.array.selected_equalizer_values, getStringPref(KEY_SELECTED_EQUALIZER, "0")))
                        if (isSystemEqualizerAvailable()) {
                            add(
                                SettingsActionItemUiModel(
                                    key = KEY_SYSTEM_EQUALIZER,
                                    title = getString(R.string.settings_system_equalizer_title),
                                    summary = getString(R.string.settings_system_equalizer_summary)
                                )
                            )
                        }
                        if (isBuiltinEqualizerVisible()) {
                            add(
                                SettingsActionItemUiModel(
                                    key = KEY_BUILTIN_EQUALIZER,
                                    title = getString(R.string.settings_app_equalizer),
                                    summary = getString(R.string.settings_app_equalizer_summary)
                                )
                            )
                        }
                    }
                )
            )
            add(
                SettingsGroupUiModel(
                    title = getString(R.string.settings_title_replay_gain),
                    items = listOf(
                        SettingsInfoItemUiModel("replay_gain_info", getString(R.string.settings_summary_replay_gain)),
                        selectItem(KEY_REPLAY_GAIN_MODE, R.string.settings_replay_gain, R.array.replay_gain_titles, R.array.replay_gain_values, getStringPref(KEY_REPLAY_GAIN_MODE, "disabled")),
                        toggleItem(KEY_REPLAY_GAIN_PREVENT_CLIPPING, R.string.settings_replay_gain_prevent_clipping_title, R.string.settings_replay_gain_prevent_clipping_summary, getBooleanPref(KEY_REPLAY_GAIN_PREVENT_CLIPPING, true))
                    )
                )
            )
            add(
                SettingsGroupUiModel(
                    title = getString(R.string.settings_title_loudness),
                    items = listOf(
                        SettingsInfoItemUiModel("loudness_info", getString(R.string.settings_summary_loudness)),
                        SettingsSliderItemUiModel(
                            key = KEY_LOUDNESS_PREAMP,
                            title = getString(R.string.settings_loudness_preamp_title),
                            summary = getString(R.string.settings_loudness_preamp_summary),
                            value = Preferences.getLoudnessPreamp().toInt(),
                            valueRange = -15..15,
                            steps = 29,
                            trailingValue = "${Preferences.getLoudnessPreamp().toInt()} dB"
                        )
                    )
                )
            )
        }

        return section(
            key = SECTION_SOUND,
            title = getString(R.string.settings_title_sound),
            groups = groups
        )
    }

    private fun buildPlayersSection(): SettingsSectionUiModel {
        return section(
            key = SECTION_PLAYERS,
            title = getString(R.string.settings_title_players),
            groups = listOf(
                SettingsGroupUiModel(
                    items = listOf(
                        toggleItem(KEY_CONTINUOUS_PLAY, R.string.settings_continuous_play_title, R.string.settings_continuous_play_summary, getBooleanPref(KEY_CONTINUOUS_PLAY, true)),
                        selectItem(KEY_CUSTOM_COMMAND_FIRST_BUTTON, R.string.settings_custom_command_first_button, R.array.custom_commands_titles, R.array.custom_commands_values, getStringPref(KEY_CUSTOM_COMMAND_FIRST_BUTTON, "[heartID]")),
                        selectItem(KEY_CUSTOM_COMMAND_SECOND_BUTTON, R.string.settings_custom_command_second_button, R.array.custom_commands_titles, R.array.custom_commands_values, getStringPref(KEY_CUSTOM_COMMAND_SECOND_BUTTON, "[repeatID]")),
                        SettingsSliderItemUiModel(
                            key = KEY_MIN_STAR_RATING,
                            title = getString(R.string.settings_title_skip_min_star_rating),
                            summary = getString(R.string.settings_summary_skip_min_star_rating),
                            value = getIntPref(KEY_MIN_STAR_RATING, 0),
                            valueRange = 0..4,
                            steps = 3,
                            trailingValue = getIntPref(KEY_MIN_STAR_RATING, 0).toString()
                        )
                    )
                )
            )
        )
    }

    private fun buildPlaylistSection(): SettingsSectionUiModel {
        return section(
            key = SECTION_PLAYLIST,
            title = getString(R.string.settings_title_playlist),
            groups = listOf(
                SettingsGroupUiModel(
                    items = listOf(
                        toggleItem(KEY_ALLOW_PLAYLIST_DUPLICATES, R.string.settings_allow_playlist_duplicates, R.string.settings_allow_playlist_duplicates_summary, getBooleanPref(KEY_ALLOW_PLAYLIST_DUPLICATES, false)),
                        selectItem(KEY_HOME_SORT_PLAYLISTS, R.string.settings_playlist_sort, R.array.playlist_sort_option_titles, R.array.playlist_sort_option_values, getStringPref(KEY_HOME_SORT_PLAYLISTS, "ORDER_BY_NAME"))
                    )
                )
            )
        )
    }

    private fun buildDataSection(): SettingsSectionUiModel {
        val items = buildList<SettingsItemUiModel> {
            add(
                selectItem(
                    key = KEY_STREAMING_CACHE_SIZE,
                    titleRes = R.string.settings_streaming_cache_size,
                    entriesRes = R.array.streaming_cache_size_titles,
                    valuesRes = R.array.streaming_cache_size_values,
                    selectedValue = getStringPref(KEY_STREAMING_CACHE_SIZE, "256"),
                    summary = getString(
                        R.string.settings_summary_streaming_cache_size,
                        resolveOptionLabel(R.array.streaming_cache_size_titles, R.array.streaming_cache_size_values, getStringPref(KEY_STREAMING_CACHE_SIZE, "256")),
                        (DownloadUtil.getStreamingCacheSize(requireActivity()) / (1024 * 1024)).toString()
                    )
                )
            )
            add(selectItem(KEY_IMAGE_CACHE_SIZE, R.string.settings_covers_cache, R.array.pref_cache_size_titles, R.array.pref_cache_size_values, getStringPref(KEY_IMAGE_CACHE_SIZE, "500")))
            add(selectItem(KEY_IMAGE_SIZE, R.string.settings_image_size, R.array.pref_image_size_titles, R.array.pref_image_size_values, getStringPref(KEY_IMAGE_SIZE, "-1")))
            add(toggleItem(KEY_WIFI_ONLY, R.string.settings_wifi_only_title, R.string.settings_wifi_only_summary, getBooleanPref(KEY_WIFI_ONLY, false)))
            add(toggleItem(KEY_DOWNLOAD_WIFI_ONLY, R.string.settings_download_wifi_only_title, R.string.settings_download_wifi_only_summary, getBooleanPref(KEY_DOWNLOAD_WIFI_ONLY, false)))
            add(toggleItem(KEY_DATA_SAVING_MODE, R.string.settings_data_saving_mode_title, R.string.settings_data_saving_mode_summary, getBooleanPref(KEY_DATA_SAVING_MODE, false)))
            add(toggleItem(KEY_SYNC_STARRED_TRACKS, R.string.settings_sync_starred_tracks_for_offline_use_title, R.string.settings_sync_starred_tracks_for_offline_use_summary, getBooleanPref(KEY_SYNC_STARRED_TRACKS, false)))
            add(toggleItem(KEY_SYNC_STARRED_ALBUMS, R.string.settings_sync_starred_albums_for_offline_use_title, R.string.settings_sync_starred_albums_for_offline_use_summary, getBooleanPref(KEY_SYNC_STARRED_ALBUMS, false)))
            add(toggleItem(KEY_SYNC_STARRED_ARTISTS, R.string.settings_sync_starred_artists_for_offline_use_title, R.string.settings_sync_starred_artists_for_offline_use_summary, getBooleanPref(KEY_SYNC_STARRED_ARTISTS, false)))
            val preloadLabel = resolveOptionLabel(R.array.song_preload_buffer_titles, R.array.song_preload_buffer_values, getStringPref(KEY_SONG_PRELOAD_BUFFER, "60"))
            add(
                selectItem(
                    key = KEY_SONG_PRELOAD_BUFFER,
                    titleRes = R.string.settings_song_preload_buffer,
                    entriesRes = R.array.song_preload_buffer_titles,
                    valuesRes = R.array.song_preload_buffer_values,
                    selectedValue = getStringPref(KEY_SONG_PRELOAD_BUFFER, "60"),
                    summary = getString(R.string.settings_song_preload_buffer_summary, preloadLabel)
                )
            )
            if (hasExternalStorage()) {
                add(
                    SettingsActionItemUiModel(
                        key = KEY_STREAMING_CACHE_STORAGE,
                        title = getString(R.string.settings_streaming_cache_storage_title),
                        value = getStreamingCacheStorageSummary()
                    )
                )
                if (Preferences.getDownloadDirectoryUri() == null) {
                    add(
                        SettingsActionItemUiModel(
                            key = KEY_DOWNLOAD_STORAGE,
                            title = getString(R.string.settings_download_storage_title),
                            value = getDownloadStorageSummary()
                        )
                    )
                }
            }
            buildDownloadDirectoryItem()?.let(::add)
            add(
                SettingsActionItemUiModel(
                    key = KEY_DELETE_DOWNLOAD_STORAGE,
                    title = getString(R.string.settings_delete_download_storage_title),
                    summary = getString(R.string.settings_delete_download_storage_summary),
                    destructive = true
                )
            )
        }

        return section(
            key = SECTION_DATA,
            title = getString(R.string.settings_title_data),
            groups = listOf(SettingsGroupUiModel(items = items))
        )
    }

    private fun buildTranscodingSection(): SettingsSectionUiModel {
        return section(
            key = SECTION_TRANSCODING,
            title = getString(R.string.settings_title_transcoding),
            groups = listOf(
                SettingsGroupUiModel(
                    items = listOf(
                        SettingsInfoItemUiModel("transcoding_info", getString(R.string.settings_summary_transcoding)),
                        toggleItem(KEY_AUDIO_TRANSCODE_PRIORITY, R.string.settings_audio_transcode_priority_title, R.string.settings_audio_transcode_priority_summary, getBooleanPref(KEY_AUDIO_TRANSCODE_PRIORITY, false)),
                        selectItem(KEY_AUDIO_TRANSCODE_FORMAT_WIFI, R.string.settings_audio_transcode_format_wifi, R.array.audio_transcode_format_wifi_list_titles, R.array.audio_transcode_format_wifi_list_values, getStringPref(KEY_AUDIO_TRANSCODE_FORMAT_WIFI, "raw")),
                        selectItem(KEY_MAX_BITRATE_WIFI, R.string.settings_max_bitrate_wifi, R.array.max_bitrate_wifi_list_titles, R.array.max_bitrate_wifi_list_values, getStringPref(KEY_MAX_BITRATE_WIFI, "0")),
                        selectItem(KEY_AUDIO_TRANSCODE_FORMAT_MOBILE, R.string.settings_audio_transcode_format_mobile, R.array.audio_transcode_format_mobile_list_titles, R.array.audio_transcode_format_mobile_list_values, getStringPref(KEY_AUDIO_TRANSCODE_FORMAT_MOBILE, "raw")),
                        selectItem(KEY_MAX_BITRATE_MOBILE, R.string.settings_max_bitrate_mobile, R.array.max_bitrate_mobile_list_titles, R.array.max_bitrate_mobile_list_values, getStringPref(KEY_MAX_BITRATE_MOBILE, "0"))
                    )
                ),
                SettingsGroupUiModel(
                    title = getString(R.string.settings_title_transcoding_download),
                    items = listOf(
                        SettingsInfoItemUiModel("transcoding_download_info", getString(R.string.settings_summary_transcoding_download)),
                        toggleItem(KEY_AUDIO_TRANSCODE_DOWNLOAD, R.string.settings_audio_transcode_download_title, R.string.settings_audio_transcode_download_summary, getBooleanPref(KEY_AUDIO_TRANSCODE_DOWNLOAD, false)),
                        toggleItem(KEY_AUDIO_TRANSCODE_DOWNLOAD_PRIORITY, R.string.settings_audio_transcode_download_priority_title, R.string.settings_audio_transcode_download_priority_summary, getBooleanPref(KEY_AUDIO_TRANSCODE_DOWNLOAD_PRIORITY, false)),
                        selectItem(KEY_AUDIO_TRANSCODE_FORMAT_DOWNLOAD, R.string.settings_audio_transcode_format_download, R.array.audio_transcode_format_download_list_titles, R.array.audio_transcode_format_download_list_values, getStringPref(KEY_AUDIO_TRANSCODE_FORMAT_DOWNLOAD, "raw")),
                        selectItem(KEY_MAX_BITRATE_DOWNLOAD, R.string.settings_max_bitrate_download, R.array.max_bitrate_download_list_titles, R.array.max_bitrate_download_list_values, getStringPref(KEY_MAX_BITRATE_DOWNLOAD, "0"))
                    )
                )
            )
        )
    }

    private fun buildMiscSection(): SettingsSectionUiModel {
        return section(
            key = SECTION_MISC,
            title = getString(R.string.settings_title_misc),
            groups = listOf(
                SettingsGroupUiModel(
                    title = getString(R.string.settings_title_scrobble),
                    items = listOf(
                        SettingsInfoItemUiModel("scrobble_info_1", getString(R.string.settings_summary_scrobble)),
                        SettingsInfoItemUiModel("scrobble_info_2", getString(R.string.settings_sub_summary_scrobble)),
                        toggleItem(KEY_SCROBBLING, R.string.settings_scrobble_title, null, getBooleanPref(KEY_SCROBBLING, true))
                    )
                ),
                SettingsGroupUiModel(
                    title = getString(R.string.settings_title_share),
                    items = listOf(
                        SettingsInfoItemUiModel("share_info", getString(R.string.settings_summary_share)),
                        toggleItem(KEY_SHARE, R.string.settings_share_title, null, getBooleanPref(KEY_SHARE, false))
                    )
                ),
                SettingsGroupUiModel(
                    title = getString(R.string.settings_title_syncing),
                    items = listOf(
                        SettingsInfoItemUiModel("queue_sync_info", getString(R.string.settings_summary_syncing)),
                        toggleItem(KEY_QUEUE_SYNCING, R.string.settings_queue_syncing_title, R.string.settings_queue_syncing_summary, getBooleanPref(KEY_QUEUE_SYNCING, false)),
                        selectItem(KEY_QUEUE_SYNCING_COUNTDOWN, R.string.settings_queue_syncing_countdown, R.array.queue_syncing_countdown_titles, R.array.queue_syncing_countdown_values, getStringPref(KEY_QUEUE_SYNCING_COUNTDOWN, "5"))
                    )
                )
            )
        )
    }

    private fun buildAndroidAutoSection(): SettingsSectionUiModel {
        return section(
            key = SECTION_ANDROID_AUTO,
            title = getString(R.string.settings_androidauto),
            groups = listOf(
                SettingsGroupUiModel(
                    items = listOf(
                        SettingsInfoItemUiModel("android_auto_info", getString(R.string.home_rearrangement_dialog_subtitle)),
                        selectItem(KEY_ANDROID_AUTO_FIRST_TAB, R.string.settings_androidauto_first_tab, R.array.aa_tab_titles, R.array.aa_tab_values, getStringPref(KEY_ANDROID_AUTO_FIRST_TAB, "0")),
                        selectItem(KEY_ANDROID_AUTO_SECOND_TAB, R.string.settings_androidauto_second_tab, R.array.aa_tab_titles, R.array.aa_tab_values, getStringPref(KEY_ANDROID_AUTO_SECOND_TAB, "1")),
                        selectItem(KEY_ANDROID_AUTO_THIRD_TAB, R.string.settings_androidauto_third_tab, R.array.aa_tab_titles, R.array.aa_tab_values, getStringPref(KEY_ANDROID_AUTO_THIRD_TAB, "2")),
                        selectItem(KEY_ANDROID_AUTO_FOURTH_TAB, R.string.settings_androidauto_fourth_tab, R.array.aa_tab_titles, R.array.aa_tab_values, getStringPref(KEY_ANDROID_AUTO_FOURTH_TAB, "3")),
                        toggleItem(KEY_ANDROID_AUTO_HOME_VIEW, R.string.settings_androidauto_home_view, null, getBooleanPref(KEY_ANDROID_AUTO_HOME_VIEW, false)),
                        toggleItem(KEY_ANDROID_AUTO_ALBUM_VIEW, R.string.settings_androidauto_album_view, null, getBooleanPref(KEY_ANDROID_AUTO_ALBUM_VIEW, true)),
                        toggleItem(KEY_ANDROID_AUTO_PLAYLIST_VIEW, R.string.settings_androidauto_playlist_view, null, getBooleanPref(KEY_ANDROID_AUTO_PLAYLIST_VIEW, false)),
                        toggleItem(KEY_ANDROID_AUTO_RADIO_VIEW, R.string.settings_androidauto_radio_view, null, getBooleanPref(KEY_ANDROID_AUTO_RADIO_VIEW, false)),
                        toggleItem(KEY_ANDROID_AUTO_PODCAST_VIEW, R.string.settings_androidauto_podcast_view, null, getBooleanPref(KEY_ANDROID_AUTO_PODCAST_VIEW, false)),
                        toggleItem(KEY_ANDROID_AUTO_SHUFFLE_GENRE_SONGS, R.string.settings_androidauto_shuffle_genre_songs, null, getBooleanPref(KEY_ANDROID_AUTO_SHUFFLE_GENRE_SONGS, false)),
                        toggleItem(KEY_ANDROID_AUTO_SHUFFLE_STARRED_TRACKS, R.string.settings_androidauto_shuffle_starred_tracks, null, getBooleanPref(KEY_ANDROID_AUTO_SHUFFLE_STARRED_TRACKS, false)),
                        toggleItem(KEY_ANDROID_AUTO_SHUFFLE_PLAYLISTS, R.string.settings_androidauto_shuffle_playlists, null, getBooleanPref(KEY_ANDROID_AUTO_SHUFFLE_PLAYLISTS, false)),
                        selectItem(KEY_ANDROID_AUTO_STARRED_FOR_MADE_FOR_YOU, R.string.settings_androidauto_starred_for_made_for_you, R.array.aa_starred_for_made_for_you_titles, R.array.aa_starred_for_made_for_you_values, getStringPref(KEY_ANDROID_AUTO_STARRED_FOR_MADE_FOR_YOU, "0"))
                    )
                )
            )
        )
    }

    private fun buildGithubUpdateSection(): SettingsSectionUiModel {
        return section(
            key = SECTION_GITHUB_UPDATE,
            title = getString(R.string.settings_github_update),
            groups = listOf(
                SettingsGroupUiModel(
                    items = listOf(
                        SettingsInfoItemUiModel("github_update_info", getString(R.string.settings_github_update_summary)),
                        toggleItem(KEY_GITHUB_UPDATE_CHECK, R.string.settings_github_update_title, null, getBooleanPref(KEY_GITHUB_UPDATE_CHECK, true))
                    )
                )
            )
        )
    }

    private fun buildAboutSection(): SettingsSectionUiModel {
        return section(
            key = SECTION_ABOUT,
            title = getString(R.string.settings_about_title),
            groups = listOf(
                SettingsGroupUiModel(
                    items = listOf(
                        SettingsInfoItemUiModel("about_info", getString(R.string.settings_about_summary)),
                        SettingsValueItemUiModel(
                            key = KEY_VERSION,
                            title = getString(R.string.settings_version_title),
                            value = BuildConfig.VERSION_NAME
                        ),
                        SettingsActionItemUiModel(
                            key = KEY_ABOUT_GITHUB,
                            title = getString(R.string.settings_github_title),
                            summary = getString(R.string.settings_github_summary)
                        ),
                        SettingsActionItemUiModel(
                            key = KEY_ABOUT_SUPPORT,
                            title = getString(R.string.settings_support_title),
                            summary = getString(R.string.settings_support_summary)
                        ),
                        SettingsActionItemUiModel(
                            key = KEY_ABOUT_UNDRAW,
                            title = getString(R.string.undraw_page),
                            summary = getString(R.string.undraw_thanks)
                        )
                    )
                )
            )
        )
    }

    private fun section(
        key: String,
        title: String,
        groups: List<SettingsGroupUiModel>
    ) = SettingsSectionUiModel(
        key = key,
        title = title,
        expanded = expandedSections.contains(key),
        groups = groups
    )

    private fun toggleItem(
        key: String,
        titleRes: Int,
        summaryRes: Int?,
        checked: Boolean
    ) = SettingsToggleItemUiModel(
        key = key,
        title = getString(titleRes),
        summary = summaryRes?.let(::getString),
        checked = checked
    )

    private fun selectItem(
        key: String,
        titleRes: Int,
        entriesRes: Int,
        valuesRes: Int,
        selectedValue: String,
        summary: String? = null
    ) = SettingsSelectItemUiModel(
        key = key,
        title = getString(titleRes),
        summary = summary,
        selectedLabel = resolveOptionLabel(entriesRes, valuesRes, selectedValue)
    )

    private fun buildLanguageItem(): SettingsSelectItemUiModel {
        val locales = UIUtil.getLangPreferenceDropdownEntries(requireContext())
        val currentValue = getStringPref(KEY_LANGUAGE, "default")
        val label = if (currentValue == "default") {
            getString(R.string.settings_system_language)
        } else {
            locales.entries.firstOrNull { it.value == currentValue }?.key
                ?: Locale.forLanguageTag(currentValue).displayName
        }
        return SettingsSelectItemUiModel(
            key = KEY_LANGUAGE,
            title = getString(R.string.settings_language),
            selectedLabel = label
        )
    }

    private fun resolveOptionLabel(
        entriesRes: Int,
        valuesRes: Int,
        selectedValue: String
    ): String {
        val entries = resources.getStringArray(entriesRes)
        val values = resources.getStringArray(valuesRes)
        val index = values.indexOf(selectedValue)
        return entries.getOrElse(index.coerceAtLeast(0)) { entries.firstOrNull().orEmpty() }
    }

    private fun toggleSection(key: String) {
        if (!expandedSections.add(key)) {
            expandedSections.remove(key)
        }
        refreshUiState()
    }

    private fun handleActionClick(item: SettingsActionItemUiModel) {
        when (item.key) {
            KEY_LOGOUT -> hostActivity.quit()
            KEY_SCAN_LIBRARY -> launchScan()
            KEY_STREAMING_CACHE_STORAGE -> showStreamingCacheStorageDialog()
            KEY_DOWNLOAD_STORAGE -> showDownloadStorageDialog()
            KEY_SET_DOWNLOAD_DIRECTORY -> handleDownloadDirectoryAction()
            KEY_DELETE_DOWNLOAD_STORAGE -> showDeleteDownloadStorageDialog()
            KEY_SYSTEM_EQUALIZER -> openSystemEqualizer()
            KEY_BUILTIN_EQUALIZER -> openBuiltinEqualizer()
            KEY_ABOUT_GITHUB -> openExternalLink(getString(R.string.settings_github_link))
            KEY_ABOUT_SUPPORT -> openExternalLink(getString(R.string.settings_support_discussion_link))
            KEY_ABOUT_UNDRAW -> openExternalLink(getString(R.string.undraw_url))
        }
    }

    private fun handleToggleChange(item: SettingsToggleItemUiModel, checked: Boolean) {
        when (item.key) {
            KEY_ALWAYS_ON_DISPLAY -> {
                putBooleanPref(item.key, checked)
                if (checked) {
                    hostActivity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    hostActivity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
                    requireContext(),
                    DownloaderService::class.java,
                    requirements,
                    false
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

            else -> putBooleanPref(item.key, checked)
        }

        refreshUiState()
    }

    private fun handleSliderChange(item: SettingsSliderItemUiModel, value: Int) {
        when (item.key) {
            KEY_LOUDNESS_PREAMP -> {
                Preferences.setLoudnessPreamp(value.toFloat())
                if (isServiceBound) {
                    mediaServiceBinder?.getPlayer()?.let { player ->
                        ReplayGainUtil.reapplyCurrentTrackGain(player)
                    }
                }
            }

            KEY_MIN_STAR_RATING -> putIntPref(item.key, value)
        }
        refreshUiState()
    }

    private fun showSelectionDialog(item: SettingsSelectItemUiModel) {
        uiState = uiState.copy(
            selectionDialog = SettingsSelectionDialogState(
                key = item.key,
                title = item.title,
                options = selectionOptionsFor(item.key),
                selectedValue = currentSelectedValueFor(item.key)
            )
        )
    }

    private fun showTextInputDialog(item: SettingsInputItemUiModel) {
        uiState = uiState.copy(
            textInputDialog = SettingsTextInputDialogState(
                key = item.key,
                title = item.title,
                value = item.value,
                supportingText = item.summary
            )
        )
    }

    private fun handleSelectionConfirmed(key: String, value: String) {
        when (key) {
            KEY_LANGUAGE -> {
                putStringPref(key, value)
                if (value == "default") {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                } else {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(value))
                }
            }

            KEY_THEME -> {
                putStringPref(key, value)
                ThemeHelper.applyTheme(value)
                requireActivity().recreate()
            }

            KEY_DARK_THEME_STYLE -> {
                putStringPref(key, value)
                requireActivity().recreate()
            }

            KEY_SELECTED_EQUALIZER -> {
                Preferences.setSelectedEqualizer(value)
                val intent = Intent(requireContext().applicationContext, MediaService::class.java).apply {
                    action = BaseMediaService.ACTION_RELOAD_EQUALIZER
                }
                ContextCompat.startForegroundService(requireContext().applicationContext, intent)
            }

            else -> putStringPref(key, value)
        }

        uiState = uiState.copy(selectionDialog = null)
        refreshUiState()
    }

    private fun handleTextInputConfirmed(key: String, value: String) {
        if (key == KEY_NETWORK_PING_TIMEOUT && value.isNotBlank()) {
            Preferences.setNetworkPingTimeout(value)
        }
        uiState = uiState.copy(textInputDialog = null)
        refreshUiState()
    }

    private fun selectionOptionsFor(key: String): List<SettingsOptionUiModel> {
        return when (key) {
            KEY_LANGUAGE -> UIUtil.getLangPreferenceDropdownEntries(requireContext())
                .map { (title, value) -> SettingsOptionUiModel(title = title, value = value) }

            KEY_THEME -> arrayOptions(R.array.theme_list_titles, R.array.theme_list_values)
            KEY_DARK_THEME_STYLE -> arrayOptions(R.array.dark_theme_style_titles, R.array.dark_theme_style_values)
            KEY_TILE_SIZE -> arrayOptions(R.array.tile_size_titles, R.array.tile_size_divisor)
            KEY_ROUNDED_CORNER_SIZE -> arrayOptions(R.array.rounded_corner_size_titles, R.array.rounded_corner_size_values)
            KEY_SELECTED_EQUALIZER -> arrayOptions(R.array.selected_equalizer_entries, R.array.selected_equalizer_values)
            KEY_REPLAY_GAIN_MODE -> arrayOptions(R.array.replay_gain_titles, R.array.replay_gain_values)
            KEY_CUSTOM_COMMAND_FIRST_BUTTON, KEY_CUSTOM_COMMAND_SECOND_BUTTON -> arrayOptions(R.array.custom_commands_titles, R.array.custom_commands_values)
            KEY_HOME_SORT_PLAYLISTS -> arrayOptions(R.array.playlist_sort_option_titles, R.array.playlist_sort_option_values)
            KEY_STREAMING_CACHE_SIZE -> arrayOptions(R.array.streaming_cache_size_titles, R.array.streaming_cache_size_values)
            KEY_IMAGE_CACHE_SIZE -> arrayOptions(R.array.pref_cache_size_titles, R.array.pref_cache_size_values)
            KEY_IMAGE_SIZE -> arrayOptions(R.array.pref_image_size_titles, R.array.pref_image_size_values)
            KEY_SONG_PRELOAD_BUFFER -> arrayOptions(R.array.song_preload_buffer_titles, R.array.song_preload_buffer_values)
            KEY_AUDIO_TRANSCODE_FORMAT_WIFI -> arrayOptions(R.array.audio_transcode_format_wifi_list_titles, R.array.audio_transcode_format_wifi_list_values)
            KEY_MAX_BITRATE_WIFI -> arrayOptions(R.array.max_bitrate_wifi_list_titles, R.array.max_bitrate_wifi_list_values)
            KEY_AUDIO_TRANSCODE_FORMAT_MOBILE -> arrayOptions(R.array.audio_transcode_format_mobile_list_titles, R.array.audio_transcode_format_mobile_list_values)
            KEY_MAX_BITRATE_MOBILE -> arrayOptions(R.array.max_bitrate_mobile_list_titles, R.array.max_bitrate_mobile_list_values)
            KEY_AUDIO_TRANSCODE_FORMAT_DOWNLOAD -> arrayOptions(R.array.audio_transcode_format_download_list_titles, R.array.audio_transcode_format_download_list_values)
            KEY_MAX_BITRATE_DOWNLOAD -> arrayOptions(R.array.max_bitrate_download_list_titles, R.array.max_bitrate_download_list_values)
            KEY_QUEUE_SYNCING_COUNTDOWN -> arrayOptions(R.array.queue_syncing_countdown_titles, R.array.queue_syncing_countdown_values)
            KEY_ANDROID_AUTO_FIRST_TAB, KEY_ANDROID_AUTO_SECOND_TAB, KEY_ANDROID_AUTO_THIRD_TAB, KEY_ANDROID_AUTO_FOURTH_TAB -> arrayOptions(R.array.aa_tab_titles, R.array.aa_tab_values)
            KEY_ANDROID_AUTO_STARRED_FOR_MADE_FOR_YOU -> arrayOptions(R.array.aa_starred_for_made_for_you_titles, R.array.aa_starred_for_made_for_you_values)
            else -> emptyList()
        }
    }

    private fun currentSelectedValueFor(key: String): String {
        val default = when (key) {
            KEY_THEME -> ThemeHelper.DEFAULT_MODE
            KEY_DARK_THEME_STYLE -> "standard"
            KEY_TILE_SIZE -> "2"
            KEY_SELECTED_EQUALIZER -> "0"
            KEY_REPLAY_GAIN_MODE -> "disabled"
            KEY_CUSTOM_COMMAND_FIRST_BUTTON -> "[heartID]"
            KEY_CUSTOM_COMMAND_SECOND_BUTTON -> "[repeatID]"
            KEY_HOME_SORT_PLAYLISTS -> "ORDER_BY_NAME"
            KEY_STREAMING_CACHE_SIZE -> "256"
            KEY_IMAGE_CACHE_SIZE -> "500"
            KEY_IMAGE_SIZE -> "-1"
            KEY_SONG_PRELOAD_BUFFER -> "60"
            KEY_AUDIO_TRANSCODE_FORMAT_WIFI, KEY_AUDIO_TRANSCODE_FORMAT_MOBILE, KEY_AUDIO_TRANSCODE_FORMAT_DOWNLOAD -> "raw"
            KEY_MAX_BITRATE_WIFI, KEY_MAX_BITRATE_MOBILE, KEY_MAX_BITRATE_DOWNLOAD -> "0"
            KEY_QUEUE_SYNCING_COUNTDOWN -> "5"
            KEY_ANDROID_AUTO_FIRST_TAB -> "0"
            KEY_ANDROID_AUTO_SECOND_TAB -> "1"
            KEY_ANDROID_AUTO_THIRD_TAB -> "2"
            KEY_ANDROID_AUTO_FOURTH_TAB -> "3"
            KEY_ANDROID_AUTO_STARRED_FOR_MADE_FOR_YOU -> "0"
            KEY_LANGUAGE -> "default"
            else -> ""
        }
        return getStringPref(key, default)
    }

    private fun arrayOptions(entriesRes: Int, valuesRes: Int): List<SettingsOptionUiModel> {
        val entries = resources.getStringArray(entriesRes)
        val values = resources.getStringArray(valuesRes)
        return entries.zip(values).map { (title, value) ->
            SettingsOptionUiModel(title = title, value = value)
        }
    }

    private fun launchScan() {
        settingViewModel.launchScan(object : ScanCallback {
            override fun onError(exception: Exception?) {
                scanSummary = exception?.message
                refreshUiState()
            }

            override fun onSuccess(isScanning: Boolean, count: Long) {
                scanSummary = getString(R.string.settings_scan_result, count)
                refreshUiState()
                if (isScanning) {
                    getScanStatus()
                }
            }
        })
    }

    private fun getScanStatus() {
        settingViewModel.getScanStatus(object : ScanCallback {
            override fun onError(exception: Exception?) {
                scanSummary = exception?.message
                refreshUiState()
            }

            override fun onSuccess(isScanning: Boolean, count: Long) {
                scanSummary = getString(R.string.settings_scan_result, count)
                refreshUiState()
                if (isScanning) {
                    getScanStatus()
                }
            }
        })
    }

    private fun showStreamingCacheStorageDialog() {
        StreamingCacheStorageDialog(object : DialogClickCallback {
            override fun onPositiveClick() = refreshUiState()
            override fun onNegativeClick() = refreshUiState()
        }).show(parentFragmentManager, null)
    }

    private fun showDownloadStorageDialog() {
        DownloadStorageDialog(object : DialogClickCallback {
            override fun onPositiveClick() = refreshUiState()
            override fun onNegativeClick() = refreshUiState()
            override fun onNeutralClick() = refreshUiState()
        }).show(parentFragmentManager, null)
    }

    private fun showDeleteDownloadStorageDialog() {
        DeleteDownloadStorageDialog(::refreshUiState).show(parentFragmentManager, null)
    }

    private fun showStarredSyncDialog() {
        StarredSyncDialog(::refreshUiState, ::refreshUiState).show(parentFragmentManager, null)
    }

    private fun showStarredAlbumSyncDialog() {
        StarredAlbumSyncDialog(::refreshUiState, ::refreshUiState).show(parentFragmentManager, null)
    }

    private fun showStarredArtistSyncDialog() {
        StarredArtistSyncDialog(::refreshUiState, ::refreshUiState).show(parentFragmentManager, null)
    }

    private fun handleDownloadDirectoryAction() {
        val current = Preferences.getDownloadDirectoryUri()
        if (current != null) {
            Preferences.setDownloadDirectoryUri(null)
            Preferences.setDownloadStoragePreference(0)
            ExternalAudioReader.refreshCache()
            Toast.makeText(requireContext(), R.string.settings_download_folder_cleared, Toast.LENGTH_SHORT).show()
            refreshUiState()
            return
        }

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        directoryPickerLauncher.launch(intent)
    }

    private fun openSystemEqualizer() {
        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
        startActivity(intent)
    }

    private fun openBuiltinEqualizer() {
        val navController: NavController = NavHostFragment.findNavController(this)
        val navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setPopUpTo(R.id.equalizerFragment, true)
            .build()
        hostActivity.setBottomNavigationBarVisibility(true)
        hostActivity.setBottomSheetVisibility(true)
        navController.navigate(R.id.equalizerFragment, null, navOptions)
    }

    private fun openExternalLink(url: String) {
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        )
    }

    private fun bindMediaService() {
        if (isServiceBound) return
        val intent = Intent(requireActivity(), MediaService::class.java).apply {
            action = BaseMediaService.ACTION_BIND_EQUALIZER
        }
        requireActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        isServiceBound = true
    }

    private fun navigateBack() {
        NavHostFragment.findNavController(this).navigateUp()
    }

    private fun isDarkThemeVisible(themeOption: String): Boolean {
        return when (themeOption) {
            ThemeHelper.DARK_MODE, ThemeHelper.AMOLED_MODE -> true
            ThemeHelper.DEFAULT_MODE -> {
                val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                nightMode == Configuration.UI_MODE_NIGHT_YES
            }
            else -> false
        }
    }

    private fun hasExternalStorage(): Boolean {
        return try {
            requireContext().getExternalFilesDirs(null).getOrNull(1) != null
        } catch (_: Exception) {
            false
        }
    }

    private fun isSystemEqualizerAvailable(): Boolean {
        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
        return intent.resolveActivity(requireActivity().packageManager) != null
    }

    private fun isBuiltinEqualizerVisible(): Boolean {
        val equalizerManager: EqualizerManager = mediaServiceBinder?.getEqualizerManager() ?: return false
        return equalizerManager.getNumberOfBands() > 0
    }

    private fun getStreamingCacheStorageSummary(): String {
        return if (Preferences.getStreamingCacheStoragePreference() == 0) {
            getString(R.string.streaming_cache_storage_internal_dialog_negative_button)
        } else {
            getString(R.string.streaming_cache_storage_external_dialog_positive_button)
        }
    }

    private fun getDownloadStorageSummary(): String {
        return when (Preferences.getDownloadStoragePreference()) {
            0 -> getString(R.string.download_storage_internal_dialog_negative_button)
            1 -> getString(R.string.download_storage_external_dialog_positive_button)
            else -> getString(R.string.download_storage_directory_dialog_neutral_button)
        }
    }

    private fun buildDownloadDirectoryItem(): SettingsActionItemUiModel? {
        val current = Preferences.getDownloadDirectoryUri()
        if (current != null) {
            return SettingsActionItemUiModel(
                key = KEY_SET_DOWNLOAD_DIRECTORY,
                title = getString(R.string.settings_clear_download_folder),
                summary = current,
                destructive = true
            )
        }

        if (Preferences.getDownloadStoragePreference() != 2) {
            return null
        }

        return SettingsActionItemUiModel(
            key = KEY_SET_DOWNLOAD_DIRECTORY,
            title = getString(R.string.settings_set_download_folder),
            summary = getString(R.string.settings_choose_download_folder)
        )
    }

    private fun getBooleanPref(key: String, defaultValue: Boolean): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getBoolean(key, defaultValue)
    }

    private fun putBooleanPref(key: String, value: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .edit()
            .putBoolean(key, value)
            .apply()
    }

    private fun getStringPref(key: String, defaultValue: String): String {
        return PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getString(key, defaultValue)
            ?: defaultValue
    }

    private fun putStringPref(key: String, value: String) {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .edit()
            .putString(key, value)
            .apply()
    }

    private fun getIntPref(key: String, defaultValue: Int): Int {
        return PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getInt(key, defaultValue)
    }

    private fun putIntPref(key: String, value: Int) {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .edit()
            .putInt(key, value)
            .apply()
    }

    companion object {
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
        private const val KEY_PODCAST_SECTION_VISIBILITY = "podcast_section_visibility"
        private const val KEY_RADIO_SECTION_VISIBILITY = "radio_section_visibility"
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
        private const val KEY_ANDROID_AUTO_RADIO_VIEW = "androidauto_radio_view"
        private const val KEY_ANDROID_AUTO_PODCAST_VIEW = "androidauto_podcast_view"
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
    }
}
