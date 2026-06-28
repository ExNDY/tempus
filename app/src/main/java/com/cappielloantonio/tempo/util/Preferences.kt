package com.cappielloantonio.tempo.util

import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.preference.PreferenceManager
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.model.HomeSector
import com.cappielloantonio.tempo.subsonic.models.OpenSubsonicExtension
import com.google.gson.Gson
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.boolean
import com.russhwolf.settings.float
import com.russhwolf.settings.int
import com.russhwolf.settings.long
import com.russhwolf.settings.nullableString
import com.russhwolf.settings.string

@UnstableApi
object Preferences {
    private val settings: Settings by lazy {
        SharedPreferencesSettings(PreferenceManager.getDefaultSharedPreferences(App.getContext()))
    }
    private val gson = Gson()

    // ==========================================
    // Server & Authentication
    // ==========================================



    @JvmStatic
    fun getServer(): String? = settings.getStringOrNull(SERVER)
    @JvmStatic
    fun setServer(server: String?) = settings.putString(SERVER, server ?: "")

    @JvmStatic
    fun getNetworkPingTimeout(): Int {
        val timeoutString = settings.getString(NETWORK_PING_TIMEOUT, "2")
        return (timeoutString.toIntOrNull() ?: 2).coerceAtLeast(1)
    }

    @JvmStatic
    fun setNetworkPingTimeout(pingTimeout: String?) = settings.putString(NETWORK_PING_TIMEOUT, pingTimeout ?: "2")

    @JvmStatic
    fun getUser(): String? = settings.getStringOrNull(USER)
    @JvmStatic
    fun setUser(user: String?) = settings.putString(USER, user ?: "")

    @JvmStatic
    fun getPassword(): String? = settings.getStringOrNull(PASSWORD)
    @JvmStatic
    fun setPassword(password: String?) = settings.putString(PASSWORD, password ?: "")

    @JvmStatic
    fun getToken(): String? = settings.getStringOrNull(TOKEN)
    @JvmStatic
    fun setToken(token: String?) = settings.putString(TOKEN, token ?: "")

    @JvmStatic
    fun getSalt(): String? = settings.getStringOrNull(SALT)
    @JvmStatic
    fun setSalt(salt: String?) = settings.putString(SALT, salt ?: "")

    @JvmStatic
    fun isLowSecurity(): Boolean = settings.getBoolean(LOW_SECURITY, false)
    @JvmStatic
    fun setLowSecurity(isLowSecurity: Boolean) = settings.putBoolean(LOW_SECURITY, isLowSecurity)

    @JvmStatic
    fun getClientCert(): String? = settings.getStringOrNull(CLIENT_CERT)
    @JvmStatic
    fun setClientCert(clientCert: String?) = settings.putString(CLIENT_CERT, clientCert ?: "")

    @JvmStatic
    fun getServerId(): String? = settings.getStringOrNull(SERVER_ID)
    @JvmStatic
    fun setServerId(serverId: String?) = settings.putString(SERVER_ID, serverId ?: "")

    @JvmStatic
    fun isOpenSubsonic(): Boolean = settings.getBoolean(OPEN_SUBSONIC, false)
    @JvmStatic
    fun setOpenSubsonic(isOpenSubsonic: Boolean) = settings.putBoolean(OPEN_SUBSONIC, isOpenSubsonic)

    @JvmStatic
    fun getOpenSubsonicExtensions(): String? = settings.getStringOrNull(OPEN_SUBSONIC_EXTENSIONS)
    @JvmStatic
    fun setOpenSubsonicExtensions(extension: List<OpenSubsonicExtension>) {
        settings.putString(OPEN_SUBSONIC_EXTENSIONS, gson.toJson(extension))
    }

    @JvmStatic
    fun isAutoDownloadLyricsEnabled(): Boolean = settings.getBoolean(AUTO_DOWNLOAD_LYRICS, false)
    @JvmStatic
    fun setAutoDownloadLyricsEnabled(isEnabled: Boolean) = settings.putBoolean(AUTO_DOWNLOAD_LYRICS, isEnabled)

    @JvmStatic
    fun getLocalAddress(): String? = settings.getStringOrNull(LOCAL_ADDRESS)
    @JvmStatic
    fun setLocalAddress(address: String?) = settings.putString(LOCAL_ADDRESS, address ?: "")

    @JvmStatic
    fun getInUseServerAddress(): String? = settings.getStringOrNull(IN_USE_SERVER_ADDRESS)?.takeIf { it.isNotBlank() } ?: getServer()
    @JvmStatic
    fun isInUseServerAddressLocal(): Boolean = getInUseServerAddress() == getLocalAddress()

    @JvmStatic
    fun switchInUseServerAddress() {
        val current = getInUseServerAddress()
        val server = getServer()
        val local = getLocalAddress()

        val targetAddress = if (current == server) {
            local?.takeIf { it.isNotBlank() } ?: server
        } else {
            server?.takeIf { it.isNotBlank() } ?: local
        }

        if (!targetAddress.isNullOrBlank()) {
            settings.putString(IN_USE_SERVER_ADDRESS, targetAddress)
        }
    }

    @JvmStatic
    fun isServerSwitchable(): Boolean {
        return settings.getLong(NEXT_SERVER_SWITCH, 0) + 15000 < System.currentTimeMillis() && !getServer().isNullOrEmpty() && !getLocalAddress().isNullOrEmpty()
    }

    @JvmStatic
    fun setServerSwitchableTimer() = settings.putLong(NEXT_SERVER_SWITCH, System.currentTimeMillis())

    @JvmStatic
    fun askForOptimization(): Boolean = settings.getBoolean(BATTERY_OPTIMIZATION, true)
    @JvmStatic
    fun dontAskForOptimization() = settings.putBoolean(BATTERY_OPTIMIZATION, false)

    @JvmStatic
    fun getPlaybackSpeed(): Float = settings.getFloat(PLAYBACK_SPEED, 1f)
    @JvmStatic
    fun setPlaybackSpeed(playbackSpeed: Float) = settings.putFloat(PLAYBACK_SPEED, playbackSpeed)

    @JvmStatic
    fun getBitrateVisible(): Boolean = settings.getBoolean(BITRATE_VISIBLE, true)
    @JvmStatic
    fun setBitrateVisible(bitrateVisible: Boolean) = settings.putBoolean(BITRATE_VISIBLE, bitrateVisible)

    @JvmStatic
    fun getQuickActionVisible(): Boolean = settings.getBoolean(QUICK_ACTION_VISIBLE, true)
    @JvmStatic
    fun setQuickActionVisible(quickActionVisible: Boolean) = settings.putBoolean(QUICK_ACTION_VISIBLE, quickActionVisible)

    @JvmStatic
    fun getTrackNumberVisible(): Boolean = settings.getBoolean(TRACK_NUMBER_VISIBLE, false)
    @JvmStatic
    fun setTrackNumberVisible(trackNumberVisible: Boolean) = settings.putBoolean(TRACK_NUMBER_VISIBLE, trackNumberVisible)

    @JvmStatic
    fun isSkipSilenceMode(): Boolean = settings.getBoolean(SKIP_SILENCE, false)
    @JvmStatic
    fun setSkipSilenceMode(isSkipSilenceMode: Boolean) = settings.putBoolean(SKIP_SILENCE, isSkipSilenceMode)

    @JvmStatic
    fun isShuffleModeEnabled(): Boolean = settings.getBoolean(SHUFFLE_MODE, false)
    @JvmStatic
    fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) = settings.putBoolean(SHUFFLE_MODE, shuffleModeEnabled)

    @JvmStatic
    fun getRepeatMode(): Int = settings.getInt(REPEAT_MODE, Player.REPEAT_MODE_OFF)
    @JvmStatic
    fun setRepeatMode(repeatMode: Int) = settings.putInt(REPEAT_MODE, repeatMode)

    @JvmStatic
    fun getImageCacheSize(): Int = settings.getString(IMAGE_CACHE_SIZE, "500").toInt()
    @JvmStatic
    fun getLandscapeItemsPerRow(): Int = settings.getString(LANDSCAPE_ITEMS_PER_ROW, "4").toInt()
    @JvmStatic
    fun getEnableDrawerOnPortrait(): Boolean = settings.getBoolean(ENABLE_DRAWER_ON_PORTRAIT, false)
    @JvmStatic
    fun getHideBottomNavbarOnPortrait(): Boolean = settings.getBoolean(HIDE_BOTTOM_NAVBAR_ON_PORTRAIT, false)
    @JvmStatic
    fun getImageSize(): Int = settings.getString(IMAGE_SIZE, "-1").toInt()
    @JvmStatic
    fun getStreamingCacheSize(): Long = settings.getString(STREAMING_CACHE_SIZE, "256").toLong()

    @JvmStatic
    fun getMaxBitrateWifi(): String = settings.getString(MAX_BITRATE_WIFI, "0")
    @JvmStatic
    fun getMaxBitrateMobile(): String = settings.getString(MAX_BITRATE_MOBILE, "0")

    @JvmStatic
    fun getAudioTranscodeFormatWifi(): String = settings.getString(AUDIO_TRANSCODE_FORMAT_WIFI, "raw")
    @JvmStatic
    fun getAudioTranscodeFormatMobile(): String = settings.getString(AUDIO_TRANSCODE_FORMAT_MOBILE, "raw")

    @JvmStatic
    fun isWifiOnly(): Boolean = settings.getBoolean(WIFI_ONLY, false)
    @JvmStatic
    fun isDownloadWifiOnly(): Boolean = settings.getBoolean(DOWNLOAD_WIFI_ONLY, false)
    @JvmStatic
    fun setDownloadWifiOnly(enabled: Boolean) = settings.putBoolean(DOWNLOAD_WIFI_ONLY, enabled)

    @JvmStatic
    fun isDataSavingMode(): Boolean = settings.getBoolean(DATA_SAVING_MODE, false)
    @JvmStatic
    fun setDataSavingMode(isDataSavingModeEnabled: Boolean) = settings.putBoolean(DATA_SAVING_MODE, isDataSavingModeEnabled)

    @JvmStatic
    fun isStarredArtistsSyncEnabled(): Boolean = settings.getBoolean(SYNC_STARRED_ARTISTS_FOR_OFFLINE_USE, false)
    @JvmStatic
    fun setStarredArtistsSyncEnabled(enabled: Boolean) = settings.putBoolean(SYNC_STARRED_ARTISTS_FOR_OFFLINE_USE, enabled)

    @JvmStatic
    fun isStarredAlbumsSyncEnabled(): Boolean = settings.getBoolean(SYNC_STARRED_ALBUMS_FOR_OFFLINE_USE, false)
    @JvmStatic
    fun setStarredAlbumsSyncEnabled(enabled: Boolean) = settings.putBoolean(SYNC_STARRED_ALBUMS_FOR_OFFLINE_USE, enabled)

    @JvmStatic
    fun isStarredSyncEnabled(): Boolean = settings.getBoolean(SYNC_STARRED_TRACKS_FOR_OFFLINE_USE, false)
    @JvmStatic
    fun setStarredSyncEnabled(enabled: Boolean) = settings.putBoolean(SYNC_STARRED_TRACKS_FOR_OFFLINE_USE, enabled)

    @JvmStatic
    fun showShuffleInsteadOfHeart(): Boolean = settings.getBoolean(MINI_SHUFFLE_BUTTON_VISIBILITY, false)
    @JvmStatic
    fun setShuffleInsteadOfHeart(enabled: Boolean) = settings.putBoolean(MINI_SHUFFLE_BUTTON_VISIBILITY, enabled)

    @JvmStatic
    fun getCustomCommandFirstButton(): String = settings.getString(CUSTOM_COMMAND_FIRST_BUTTON, "[heartID]")
    @JvmStatic
    fun getCustomCommandSecondButton(): String = settings.getString(CUSTOM_COMMAND_SECOND_BUTTON, "[repeatID]")

    @JvmStatic
    fun showServerUnreachableDialog(): Boolean = settings.getLong(SERVER_UNREACHABLE, 0) + 86400000 < System.currentTimeMillis()
    @JvmStatic
    fun setServerUnreachableDatetime() = settings.putLong(SERVER_UNREACHABLE, System.currentTimeMillis())

    @JvmStatic
    fun isSyncronizationEnabled(): Boolean = settings.getBoolean(QUEUE_SYNCING, false)
    @JvmStatic
    fun getSyncCountdownTimer(): Int = settings.getString(QUEUE_SYNCING_COUNTDOWN, "5").toInt()

    @JvmStatic
    fun isCornerRoundingEnabled(): Boolean = settings.getBoolean(ROUNDED_CORNER, false)
    @JvmStatic
    fun getRoundedCornerSize(): Int = settings.getString(ROUNDED_CORNER_SIZE, "12").toInt()

    @JvmStatic
    fun isPodcastSectionVisible(): Boolean = settings.getBoolean(PODCAST_SECTION_VISIBILITY, true)
    @JvmStatic
    fun setPodcastSectionHidden() = settings.putBoolean(PODCAST_SECTION_VISIBILITY, false)

    @JvmStatic
    fun isRadioSectionVisible(): Boolean = settings.getBoolean(RADIO_SECTION_VISIBILITY, true)
    @JvmStatic
    fun setRadioSectionHidden() = settings.putBoolean(RADIO_SECTION_VISIBILITY, false)

    @JvmStatic
    fun isMusicDirectorySectionVisible(): Boolean = settings.getBoolean(MUSIC_DIRECTORY_SECTION_VISIBILITY, true)

    @JvmStatic
    fun getReplayGainMode(): String = settings.getString(REPLAY_GAIN_MODE, "disabled")
    @JvmStatic
    fun isReplayGainPreventClipping(): Boolean = settings.getBoolean(REPLAY_GAIN_PREVENT_CLIPPING, true)
    @JvmStatic
    fun getLoudnessPreamp(): Float = settings.getInt(LOUDNESS_PREAMP, 0).toFloat()
    @JvmStatic
    fun setLoudnessPreamp(value: Float) = settings.putInt(LOUDNESS_PREAMP, value.toInt())

    @JvmStatic
    fun isServerPrioritized(): Boolean = settings.getBoolean(AUDIO_TRANSCODE_PRIORITY, false)

    @JvmStatic
    fun getStreamingCacheStoragePreference(): Int = settings.getString(STREAMING_CACHE_STORAGE, "0").toInt()
    @JvmStatic
    fun setStreamingCacheStoragePreference(preference: Int) = settings.putString(STREAMING_CACHE_STORAGE, preference.toString())

    @JvmStatic
    fun getDownloadStoragePreference(): Int = settings.getString(DOWNLOAD_STORAGE, "0").toInt()
    @JvmStatic
    fun setDownloadStoragePreference(preference: Int) = settings.putString(DOWNLOAD_STORAGE, preference.toString())

    @JvmStatic
    fun getDownloadDirectoryUri(): String? = settings.getStringOrNull(DOWNLOAD_DIRECTORY_URI)
    @JvmStatic
    fun setDownloadDirectoryUri(uri: String?) = settings.putString(DOWNLOAD_DIRECTORY_URI, uri ?: "")

    @JvmStatic
    fun getDefaultDownloadViewType(): String = settings.getString(DEFAULT_DOWNLOAD_VIEW_TYPE, Constants.DOWNLOAD_TYPE_TRACK)
    @JvmStatic
    fun setDefaultDownloadViewType(viewType: String) = settings.putString(DEFAULT_DOWNLOAD_VIEW_TYPE, viewType)

    @JvmStatic
    fun preferTranscodedDownload(): Boolean = settings.getBoolean(AUDIO_TRANSCODE_DOWNLOAD, false)
    @JvmStatic
    fun isServerPrioritizedInTranscodedDownload(): Boolean = settings.getBoolean(AUDIO_TRANSCODE_DOWNLOAD_PRIORITY, false)
    @JvmStatic
    fun getBitrateTranscodedDownload(): String = settings.getString(MAX_BITRATE_DOWNLOAD, "0")
    @JvmStatic
    fun getAudioTranscodeFormatTranscodedDownload(): String = settings.getString(AUDIO_TRANSCODE_FORMAT_DOWNLOAD, "raw")

    @JvmStatic
    fun isSharingEnabled(): Boolean = settings.getBoolean(SHARE, false)
    @JvmStatic
    fun isScrobblingEnabled(): Boolean = settings.getBoolean(SCROBBLING, true)

    @JvmStatic
    fun getSongPreloadBuffer(): Int = settings.getString(SONG_PRELOAD_BUFFER, "60").toInt()
    @JvmStatic
    fun getMinStarRatingAccepted(): Int = settings.getInt(MIN_STAR_RATING, 0)
    @JvmStatic
    fun isDisplayAlwaysOn(): Boolean = settings.getBoolean(ALWAYS_ON_DISPLAY, false)
    @JvmStatic
    fun showAudioQuality(): Boolean = settings.getBoolean(AUDIO_QUALITY_PER_ITEM, false)

    @JvmStatic
    fun getHomeSectorList(): String? = settings.getStringOrNull(HOME_SECTOR_LIST)
    @JvmStatic
    fun setHomeSectorList(extension: List<HomeSector>?) = settings.putString(HOME_SECTOR_LIST, gson.toJson(extension))

    @JvmStatic
    fun isLibraryMaterialCarouselEnabled(): Boolean = settings.getBoolean(HOME_LIBRARY_MATERIAL_CAROUSEL, true)
    @JvmStatic
    fun setLibraryMaterialCarouselEnabled(enabled: Boolean) = settings.putBoolean(HOME_LIBRARY_MATERIAL_CAROUSEL, enabled)

    @JvmStatic
    fun showItemStarRating(): Boolean = settings.getBoolean(SONG_RATING_PER_ITEM, false)
    @JvmStatic
    fun showItemRating(): Boolean = settings.getBoolean(RATING_PER_ITEM, false)

    @JvmStatic
    fun isGithubUpdateEnabled(): Boolean = settings.getBoolean(GITHUB_UPDATE_CHECK, true)
    @JvmStatic
    fun showTempusUpdateDialog(): Boolean = settings.getLong(NEXT_UPDATE_CHECK, 0) + 86400000 < System.currentTimeMillis()
    @JvmStatic
    fun setTempusUpdateReminder() = settings.putLong(NEXT_UPDATE_CHECK, System.currentTimeMillis())

    @JvmStatic
    fun isContinuousPlayEnabled(): Boolean = settings.getBoolean(CONTINUOUS_PLAY, true)
    @JvmStatic
    fun setLastInstantMix() = settings.putLong(LAST_INSTANT_MIX, System.currentTimeMillis())
    @JvmStatic
    fun isInstantMixUsable(): Boolean = settings.getLong(LAST_INSTANT_MIX, 0) + 10000 < System.currentTimeMillis()

    @JvmStatic
    fun setAllowPlaylistDuplicates(allowDuplicates: Boolean) = settings.putBoolean(ALLOW_PLAYLIST_DUPLICATES, allowDuplicates)
    @JvmStatic
    fun allowPlaylistDuplicates(): Boolean = settings.getBoolean(ALLOW_PLAYLIST_DUPLICATES, false)

    @JvmStatic
    fun getHomeSortPlaylists(): String = settings.getString(HOME_SORT_PLAYLISTS, DEFAULT_HOME_SORT_PLAYLISTS_SORT_ORDER)
    @JvmStatic
    fun setHomeSortPlaylists(sortOrder: String) = settings.putString(HOME_SORT_PLAYLISTS, sortOrder)

    @JvmStatic
    fun setEqualizerEnabled(enabled: Boolean) = settings.putBoolean(EQUALIZER_ENABLED, enabled)
    @JvmStatic
    fun isEqualizerEnabled(): Boolean = settings.getBoolean(EQUALIZER_ENABLED, false)

    @JvmStatic
    fun getSelectedEqualizer(): Int = settings.getString(SELECTED_EQUALIZER, "0").toInt()
    @JvmStatic
    fun setSelectedEqualizer(selectedEqualizer: String) = settings.putString(SELECTED_EQUALIZER, selectedEqualizer)

    @JvmStatic
    fun setEqualizerBandLevels(bandLevels: ShortArray) {
        val asString = bandLevels.joinToString(",")
        settings.putString(EQUALIZER_BAND_LEVELS, asString)
    }

    @JvmStatic
    fun getEqualizerBandLevels(bandCount: Short): ShortArray {
        val str = settings.getStringOrNull(EQUALIZER_BAND_LEVELS)
        if (str.isNullOrBlank()) return ShortArray(bandCount.toInt())
        val parts = str.split(",")
        if (parts.size < bandCount) return ShortArray(bandCount.toInt())
        return ShortArray(bandCount.toInt()) { i -> parts[i].toShortOrNull() ?: 0 }
    }

    @JvmStatic
    fun showAlbumDetail(): Boolean = settings.getBoolean(ALBUM_DETAIL, false)
    @JvmStatic
    fun getAlbumSortOrder(): String = settings.getString(ALBUM_SORT_ORDER, DEFAULT_ALBUM_SORT_ORDER)
    @JvmStatic
    fun setAlbumSortOrder(sortOrder: String) = settings.putString(ALBUM_SORT_ORDER, sortOrder)

    @JvmStatic
    fun getArtistSortOrder(): String {
        val sortByAlbumCount = settings.getBoolean(ARTIST_SORT_BY_ALBUM_COUNT, false)
        return if (sortByAlbumCount) Constants.ARTIST_ORDER_BY_ALBUM_COUNT else Constants.ARTIST_ORDER_BY_NAME
    }

    @JvmStatic
    fun setArtistSortOrder(sortOrder: String) {
        val sortByAlbumCount = sortOrder == Constants.ARTIST_ORDER_BY_ALBUM_COUNT
        settings.putBoolean(ARTIST_SORT_BY_ALBUM_COUNT, sortByAlbumCount)
    }

    @JvmStatic
    fun isSearchSortingChronologicallyEnabled(): Boolean = settings.getBoolean(SORT_SEARCH_CHRONOLOGICALLY, false)
    @JvmStatic
    fun getArtistDisplayBiography(): Boolean = settings.getBoolean(ARTIST_DISPLAY_BIOGRAPHY, true)
    @JvmStatic
    fun setArtistDisplayBiography(enabled: Boolean) = settings.putBoolean(ARTIST_DISPLAY_BIOGRAPHY, enabled)

    @JvmStatic
    fun getTileSize(): Int {
        val parsed = settings.getStringOrNull(TILE_SIZE)?.toIntOrNull()
        return parsed?.takeIf { it in 2..6 } ?: 2
    }

    @JvmStatic
    fun isAndroidAutoAlbumViewEnabled(): Boolean = settings.getBoolean(AA_ALBUM_VIEW, true)
    @JvmStatic
    fun isAndroidAutoHomeViewEnabled(): Boolean = settings.getBoolean(AA_HOME_VIEW, false)
    @JvmStatic
    fun isAndroidAutoPlaylistViewEnabled(): Boolean = settings.getBoolean(AA_PLAYLIST_VIEW, false)
    @JvmStatic
    fun isAndroidAutoPodcastViewEnabled(): Boolean = settings.getBoolean(AA_PODCAST_VIEW, false)
    @JvmStatic
    fun isAndroidAutoRadioViewEnabled(): Boolean = settings.getBoolean(AA_RADIO_VIEW, false)

    @JvmStatic
    fun getAndroidAutoFirstTab(): Int = settings.getString(AA_FIRST_TAB, "0").toInt()
    @JvmStatic
    fun getAndroidAutoSecondTab(): Int = settings.getString(AA_SECOND_TAB, "1").toInt()
    @JvmStatic
    fun getAndroidAutoThirdTab(): Int = settings.getString(AA_THIRD_TAB, "2").toInt()
    @JvmStatic
    fun getAndroidAutoFourthTab(): Int = settings.getString(AA_FOURTH_TAB, "3").toInt()

    @JvmStatic
    fun resetAndroidAutoFirstTab() = settings.putString(AA_FIRST_TAB, "-1")
    @JvmStatic
    fun resetAndroidAutoSecondTab() = settings.putString(AA_SECOND_TAB, "-1")
    @JvmStatic
    fun resetAndroidAutoThirdTab() = settings.putString(AA_THIRD_TAB, "-1")
    @JvmStatic
    fun resetAndroidAutoFourthTab() = settings.putString(AA_FOURTH_TAB, "-1")

    @JvmStatic
    fun isAndroidAutoShuffleGenreSongsEnabled(): Boolean = settings.getBoolean(AA_SHUFFLE_GENRE_SONGS, false)
    @JvmStatic
    fun isAndroidAutoShuffleStarredTracksEnabled(): Boolean = settings.getBoolean(AA_SHUFFLE_STARRED_TRACKS, false)
    @JvmStatic
    fun isAndroidAutoShufflePlaylistsEnabled(): Boolean = settings.getBoolean(AA_SHUFFLE_PLAYLISTS, false)
    @JvmStatic
    fun setAndroidAutoShuffleGenreSongsEnabled(enabled: Boolean) = settings.putBoolean(AA_SHUFFLE_GENRE_SONGS, enabled)

    @JvmStatic
    fun getAndroidAutoStarredForMadeForYou(): Int = settings.getString(AA_STARRED_FOR_MADE_FOR_YOU, "0").toInt()

    @JvmStatic
    fun getTheme(): String = settings.getString(THEME, "default")
    @JvmStatic
    fun setTheme(theme: String) = settings.putString(THEME, theme)

    @JvmStatic
    fun getDarkThemeStyle(): String = settings.getString(DARK_THEME_STYLE, "standard")
    @JvmStatic
    fun setDarkThemeStyle(style: String) = settings.putString(DARK_THEME_STYLE, style)

    const val THEME = "theme"
    private const val SERVER = "server"
    private const val USER = "user"
    private const val PASSWORD = "password"
    private const val TOKEN = "token"
    private const val SALT = "salt"
    private const val LOW_SECURITY = "low_security"
    private const val CLIENT_CERT = "client_cert"
    private const val BATTERY_OPTIMIZATION = "battery_optimization"
    private const val SERVER_ID = "server_id"
    private const val OPEN_SUBSONIC = "open_subsonic"
    private const val OPEN_SUBSONIC_EXTENSIONS = "open_subsonic_extensions"
    private const val LOCAL_ADDRESS = "local_address"
    private const val IN_USE_SERVER_ADDRESS = "in_use_server_address"
    private const val NEXT_SERVER_SWITCH = "next_server_switch"
    private const val PLAYBACK_SPEED = "playback_speed"
    private const val BITRATE_VISIBLE = "bitrate_visible"
    private const val QUICK_ACTION_VISIBLE = "quick_action_visible"
    private const val TRACK_NUMBER_VISIBLE = "track_number_visible"
    private const val SKIP_SILENCE = "skip_silence"
    private const val SHUFFLE_MODE = "shuffle_mode"
    private const val REPEAT_MODE = "repeat_mode"
    private const val IMAGE_CACHE_SIZE = "image_cache_size"
    private const val STREAMING_CACHE_SIZE = "streaming_cache_size"
    private const val LANDSCAPE_ITEMS_PER_ROW = "landscape_items_per_row"
    private const val ENABLE_DRAWER_ON_PORTRAIT = "enable_drawer_on_portrait"
    private const val HIDE_BOTTOM_NAVBAR_ON_PORTRAIT = "hide_bottom_navbar_on_portrait"
    private const val IMAGE_SIZE = "image_size"
    private const val MAX_BITRATE_WIFI = "max_bitrate_wifi"
    private const val MAX_BITRATE_MOBILE = "max_bitrate_mobile"
    private const val AUDIO_TRANSCODE_FORMAT_WIFI = "audio_transcode_format_wifi"
    private const val AUDIO_TRANSCODE_FORMAT_MOBILE = "audio_transcode_format_mobile"
    private const val WIFI_ONLY = "wifi_only"
    private const val DOWNLOAD_WIFI_ONLY = "download_wifi_only"
    private const val DATA_SAVING_MODE = "data_saving_mode"
    private const val SERVER_UNREACHABLE = "server_unreachable"
    private const val SYNC_STARRED_ARTISTS_FOR_OFFLINE_USE = "sync_starred_artists_for_offline_use"
    private const val SYNC_STARRED_ALBUMS_FOR_OFFLINE_USE = "sync_starred_albums_for_offline_use"
    private const val SYNC_STARRED_TRACKS_FOR_OFFLINE_USE = "sync_starred_tracks_for_offline_use"
    private const val QUEUE_SYNCING = "queue_syncing"
    private const val QUEUE_SYNCING_COUNTDOWN = "queue_syncing_countdown"
    private const val ROUNDED_CORNER = "rounded_corner"
    private const val ROUNDED_CORNER_SIZE = "rounded_corner_size"
    private const val PODCAST_SECTION_VISIBILITY = "podcast_section_visibility"
    private const val RADIO_SECTION_VISIBILITY = "radio_section_visibility"
    private const val AUTO_DOWNLOAD_LYRICS = "auto_download_lyrics"
    private const val MUSIC_DIRECTORY_SECTION_VISIBILITY = "music_directory_section_visibility"
    private const val REPLAY_GAIN_MODE = "replay_gain_mode"
    private const val REPLAY_GAIN_PREVENT_CLIPPING = "replay_gain_prevent_clipping"
    private const val LOUDNESS_PREAMP = "loudness_preamp"
    private const val AUDIO_TRANSCODE_PRIORITY = "audio_transcode_priority"
    private const val STREAMING_CACHE_STORAGE = "streaming_cache_storage"
    private const val DOWNLOAD_STORAGE = "download_storage"
    private const val DOWNLOAD_DIRECTORY_URI = "download_directory_uri"
    private const val DEFAULT_DOWNLOAD_VIEW_TYPE = "default_download_view_type"
    private const val AUDIO_TRANSCODE_DOWNLOAD = "audio_transcode_download"
    private const val AUDIO_TRANSCODE_DOWNLOAD_PRIORITY = "audio_transcode_download_priority"
    private const val MAX_BITRATE_DOWNLOAD = "max_bitrate_download"
    private const val AUDIO_TRANSCODE_FORMAT_DOWNLOAD = "audio_transcode_format_download"
    private const val SHARE = "share"
    private const val SCROBBLING = "scrobbling"
    private const val SONG_PRELOAD_BUFFER = "song_preload_buffer"
    private const val MIN_STAR_RATING = "min_star_rating"
    private const val ALWAYS_ON_DISPLAY = "always_on_display"
    private const val AUDIO_QUALITY_PER_ITEM = "audio_quality_per_item"
    private const val HOME_SECTOR_LIST = "home_sector_list"
    private const val HOME_LIBRARY_MATERIAL_CAROUSEL = "home_library_material_carousel"
    private const val SONG_RATING_PER_ITEM = "song_rating_per_item"
    private const val RATING_PER_ITEM = "rating_per_item"
    private const val NEXT_UPDATE_CHECK = "next_update_check"
    private const val GITHUB_UPDATE_CHECK = "github_update_check"
    private const val CONTINUOUS_PLAY = "continuous_play"
    private const val LAST_INSTANT_MIX = "last_instant_mix"
    private const val ALLOW_PLAYLIST_DUPLICATES = "allow_playlist_duplicates"
    private const val HOME_SORT_PLAYLISTS = "home_sort_playlists"
    private const val DEFAULT_HOME_SORT_PLAYLISTS_SORT_ORDER = Constants.PLAYLIST_ORDER_BY_RANDOM
    private const val SELECTED_EQUALIZER = "selected_equalizer"
    private const val EQUALIZER_ENABLED = "equalizer_enabled"
    private const val EQUALIZER_BAND_LEVELS = "equalizer_band_levels"
    private const val MINI_SHUFFLE_BUTTON_VISIBILITY = "mini_shuffle_button_visibility"
    private const val CUSTOM_COMMAND_FIRST_BUTTON = "custom_command_first_button"
    private const val CUSTOM_COMMAND_SECOND_BUTTON = "custom_command_second_button"
    private const val ALBUM_DETAIL = "album_detail"
    private const val ALBUM_SORT_ORDER = "album_sort_order"
    private const val DEFAULT_ALBUM_SORT_ORDER = Constants.ALBUM_ORDER_BY_NAME
    private const val ARTIST_SORT_BY_ALBUM_COUNT= "artist_sort_by_album_count"
    private const val SORT_SEARCH_CHRONOLOGICALLY= "sort_search_chronologically"
    private const val ARTIST_DISPLAY_BIOGRAPHY= "artist_display_biography"
    private const val NETWORK_PING_TIMEOUT = "network_ping_timeout_base"
    private const val TILE_SIZE = "tile_size"
    private const val AA_ALBUM_VIEW = "androidauto_album_view"
    private const val AA_HOME_VIEW = "androidauto_home_view"
    private const val AA_PLAYLIST_VIEW = "androidauto_playlist_view"
    private const val AA_PODCAST_VIEW = "androidauto_podcast_view"
    private const val AA_RADIO_VIEW = "androidauto_radio_view"
    private const val AA_FIRST_TAB = "androidauto_first_tab"
    private const val AA_SECOND_TAB = "androidauto_second_tab"
    private const val AA_THIRD_TAB = "androidauto_third_tab"
    private const val AA_FOURTH_TAB = "androidauto_fourth_tab"
    private const val AA_SHUFFLE_GENRE_SONGS = "androidauto_shuffle_genre_songs"
    private const val AA_STARRED_FOR_MADE_FOR_YOU ="androidauto_starred_for_made_for_you"
    private const val DARK_THEME_STYLE = "dark_theme_style"
    private const val AA_SHUFFLE_STARRED_TRACKS = "androidauto_shuffle_starred_tracks"
    private const val AA_SHUFFLE_PLAYLISTS = "androidauto_shuffle_playlists"
}
