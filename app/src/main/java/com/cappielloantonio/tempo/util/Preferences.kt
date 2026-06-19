package com.cappielloantonio.tempo.util

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.preference.PreferenceManager
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.model.HomeSector
import com.cappielloantonio.tempo.subsonic.models.OpenSubsonicExtension
import com.google.gson.Gson

@UnstableApi
object Preferences {
    private val preferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(App.getContext())
    private val gson = Gson()

    @JvmStatic
    fun getServer(): String? = preferences.getString(SERVER, null)
    @JvmStatic
    fun setServer(server: String?) = preferences.edit().putString(SERVER, server).apply()

    @JvmStatic
    fun getNetworkPingTimeout(): Int {
        val timeoutString = preferences.getString(NETWORK_PING_TIMEOUT, "2") ?: "2"
        return (timeoutString.toIntOrNull() ?: 2).coerceAtLeast(1)
    }

    @JvmStatic
    fun setNetworkPingTimeout(pingTimeout: String?) = preferences.edit().putString(NETWORK_PING_TIMEOUT, pingTimeout).apply()

    @JvmStatic
    fun getUser(): String? = preferences.getString(USER, null)
    @JvmStatic
    fun setUser(user: String?) = preferences.edit().putString(USER, user).apply()

    @JvmStatic
    fun getPassword(): String? = preferences.getString(PASSWORD, null)
    @JvmStatic
    fun setPassword(password: String?) = preferences.edit().putString(PASSWORD, password).apply()

    @JvmStatic
    fun getToken(): String? = preferences.getString(TOKEN, null)
    @JvmStatic
    fun setToken(token: String?) = preferences.edit().putString(TOKEN, token).apply()

    @JvmStatic
    fun getSalt(): String? = preferences.getString(SALT, null)
    @JvmStatic
    fun setSalt(salt: String?) = preferences.edit().putString(SALT, salt).apply()

    @JvmStatic
    fun isLowSecurity(): Boolean = preferences.getBoolean(LOW_SECURITY, false)
    @JvmStatic
    fun setLowSecurity(isLowSecurity: Boolean) = preferences.edit().putBoolean(LOW_SECURITY, isLowSecurity).apply()

    @JvmStatic
    fun getClientCert(): String? = preferences.getString(CLIENT_CERT, null)
    @JvmStatic
    fun setClientCert(clientCert: String?) = preferences.edit().putString(CLIENT_CERT, clientCert).apply()

    @JvmStatic
    fun getServerId(): String? = preferences.getString(SERVER_ID, null)
    @JvmStatic
    fun setServerId(serverId: String?) = preferences.edit().putString(SERVER_ID, serverId).apply()

    @JvmStatic
    fun isOpenSubsonic(): Boolean = preferences.getBoolean(OPEN_SUBSONIC, false)
    @JvmStatic
    fun setOpenSubsonic(isOpenSubsonic: Boolean) = preferences.edit().putBoolean(OPEN_SUBSONIC, isOpenSubsonic).apply()

    @JvmStatic
    fun getOpenSubsonicExtensions(): String? = preferences.getString(OPEN_SUBSONIC_EXTENSIONS, null)
    @JvmStatic
    fun setOpenSubsonicExtensions(extension: List<OpenSubsonicExtension>) {
        preferences.edit().putString(OPEN_SUBSONIC_EXTENSIONS, gson.toJson(extension)).apply()
    }

    @JvmStatic
    fun isAutoDownloadLyricsEnabled(): Boolean = preferences.getBoolean(AUTO_DOWNLOAD_LYRICS, false)
    @JvmStatic
    fun setAutoDownloadLyricsEnabled(isEnabled: Boolean) = preferences.edit().putBoolean(AUTO_DOWNLOAD_LYRICS, isEnabled).apply()

    @JvmStatic
    fun getLocalAddress(): String? = preferences.getString(LOCAL_ADDRESS, null)
    @JvmStatic
    fun setLocalAddress(address: String?) = preferences.edit().putString(LOCAL_ADDRESS, address).apply()

    @JvmStatic
    fun getInUseServerAddress(): String? = preferences.getString(IN_USE_SERVER_ADDRESS, null)?.takeIf { it.isNotBlank() } ?: getServer()
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
            preferences.edit().putString(IN_USE_SERVER_ADDRESS, targetAddress).apply()
        }
    }

    @JvmStatic
    fun isServerSwitchable(): Boolean {
        return preferences.getLong(NEXT_SERVER_SWITCH, 0) + 15000 < System.currentTimeMillis() && !getServer().isNullOrEmpty() && !getLocalAddress().isNullOrEmpty()
    }

    @JvmStatic
    fun setServerSwitchableTimer() = preferences.edit().putLong(NEXT_SERVER_SWITCH, System.currentTimeMillis()).apply()

    @JvmStatic
    fun askForOptimization(): Boolean = preferences.getBoolean(BATTERY_OPTIMIZATION, true)
    @JvmStatic
    fun dontAskForOptimization() = preferences.edit().putBoolean(BATTERY_OPTIMIZATION, false).apply()

    @JvmStatic
    fun getPlaybackSpeed(): Float = preferences.getFloat(PLAYBACK_SPEED, 1f)
    @JvmStatic
    fun setPlaybackSpeed(playbackSpeed: Float) = preferences.edit().putFloat(PLAYBACK_SPEED, playbackSpeed).apply()

    @JvmStatic
    fun getBitrateVisible(): Boolean = preferences.getBoolean(BITRATE_VISIBLE, true)
    @JvmStatic
    fun setBitrateVisible(bitrateVisible: Boolean) = preferences.edit().putBoolean(BITRATE_VISIBLE, bitrateVisible).apply()

    @JvmStatic
    fun getQuickActionVisible(): Boolean = preferences.getBoolean(QUICK_ACTION_VISIBLE, true)
    @JvmStatic
    fun setQuickActionVisible(quickActionVisible: Boolean) = preferences.edit().putBoolean(QUICK_ACTION_VISIBLE, quickActionVisible).apply()

    @JvmStatic
    fun getTrackNumberVisible(): Boolean = preferences.getBoolean(TRACK_NUMBER_VISIBLE, false)
    @JvmStatic
    fun setTrackNumberVisible(trackNumberVisible: Boolean) = preferences.edit().putBoolean(TRACK_NUMBER_VISIBLE, trackNumberVisible).apply()

    @JvmStatic
    fun isSkipSilenceMode(): Boolean = preferences.getBoolean(SKIP_SILENCE, false)
    @JvmStatic
    fun setSkipSilenceMode(isSkipSilenceMode: Boolean) = preferences.edit().putBoolean(SKIP_SILENCE, isSkipSilenceMode).apply()

    @JvmStatic
    fun isShuffleModeEnabled(): Boolean = preferences.getBoolean(SHUFFLE_MODE, false)
    @JvmStatic
    fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) = preferences.edit().putBoolean(SHUFFLE_MODE, shuffleModeEnabled).apply()

    @JvmStatic
    fun getRepeatMode(): Int = preferences.getInt(REPEAT_MODE, Player.REPEAT_MODE_OFF)
    @JvmStatic
    fun setRepeatMode(repeatMode: Int) = preferences.edit().putInt(REPEAT_MODE, repeatMode).apply()

    @JvmStatic
    fun getImageCacheSize(): Int = preferences.getString(IMAGE_CACHE_SIZE, "500")!!.toInt()
    @JvmStatic
    fun getLandscapeItemsPerRow(): Int = preferences.getString(LANDSCAPE_ITEMS_PER_ROW, "4")!!.toInt()
    @JvmStatic
    fun getEnableDrawerOnPortrait(): Boolean = preferences.getBoolean(ENABLE_DRAWER_ON_PORTRAIT, false)
    @JvmStatic
    fun getHideBottomNavbarOnPortrait(): Boolean = preferences.getBoolean(HIDE_BOTTOM_NAVBAR_ON_PORTRAIT, false)
    @JvmStatic
    fun getImageSize(): Int = preferences.getString(IMAGE_SIZE, "-1")!!.toInt()
    @JvmStatic
    fun getStreamingCacheSize(): Long = preferences.getString(STREAMING_CACHE_SIZE, "256")!!.toLong()

    @JvmStatic
    fun getMaxBitrateWifi(): String = preferences.getString(MAX_BITRATE_WIFI, "0")!!
    @JvmStatic
    fun getMaxBitrateMobile(): String = preferences.getString(MAX_BITRATE_MOBILE, "0")!!

    @JvmStatic
    fun getAudioTranscodeFormatWifi(): String = preferences.getString(AUDIO_TRANSCODE_FORMAT_WIFI, "raw")!!
    @JvmStatic
    fun getAudioTranscodeFormatMobile(): String = preferences.getString(AUDIO_TRANSCODE_FORMAT_MOBILE, "raw")!!

    @JvmStatic
    fun isWifiOnly(): Boolean = preferences.getBoolean(WIFI_ONLY, false)
    @JvmStatic
    fun isDownloadWifiOnly(): Boolean = preferences.getBoolean(DOWNLOAD_WIFI_ONLY, false)
    @JvmStatic
    fun setDownloadWifiOnly(enabled: Boolean) = preferences.edit().putBoolean(DOWNLOAD_WIFI_ONLY, enabled).apply()

    @JvmStatic
    fun isDataSavingMode(): Boolean = preferences.getBoolean(DATA_SAVING_MODE, false)
    @JvmStatic
    fun setDataSavingMode(isDataSavingModeEnabled: Boolean) = preferences.edit().putBoolean(DATA_SAVING_MODE, isDataSavingModeEnabled).apply()

    @JvmStatic
    fun isStarredArtistsSyncEnabled(): Boolean = preferences.getBoolean(SYNC_STARRED_ARTISTS_FOR_OFFLINE_USE, false)
    @JvmStatic
    fun setStarredArtistsSyncEnabled(enabled: Boolean) = preferences.edit().putBoolean(SYNC_STARRED_ARTISTS_FOR_OFFLINE_USE, enabled).apply()

    @JvmStatic
    fun isStarredAlbumsSyncEnabled(): Boolean = preferences.getBoolean(SYNC_STARRED_ALBUMS_FOR_OFFLINE_USE, false)
    @JvmStatic
    fun setStarredAlbumsSyncEnabled(enabled: Boolean) = preferences.edit().putBoolean(SYNC_STARRED_ALBUMS_FOR_OFFLINE_USE, enabled).apply()

    @JvmStatic
    fun isStarredSyncEnabled(): Boolean = preferences.getBoolean(SYNC_STARRED_TRACKS_FOR_OFFLINE_USE, false)
    @JvmStatic
    fun setStarredSyncEnabled(enabled: Boolean) = preferences.edit().putBoolean(SYNC_STARRED_TRACKS_FOR_OFFLINE_USE, enabled).apply()

    @JvmStatic
    fun showShuffleInsteadOfHeart(): Boolean = preferences.getBoolean(MINI_SHUFFLE_BUTTON_VISIBILITY, false)
    @JvmStatic
    fun setShuffleInsteadOfHeart(enabled: Boolean) = preferences.edit().putBoolean(MINI_SHUFFLE_BUTTON_VISIBILITY, enabled).apply()

    @JvmStatic
    fun getCustomCommandFirstButton(): String? = preferences.getString(CUSTOM_COMMAND_FIRST_BUTTON, "[heartID]")
    @JvmStatic
    fun getCustomCommandSecondButton(): String? = preferences.getString(CUSTOM_COMMAND_SECOND_BUTTON, "[repeatID]")

    @JvmStatic
    fun showServerUnreachableDialog(): Boolean = preferences.getLong(SERVER_UNREACHABLE, 0) + 86400000 < System.currentTimeMillis()
    @JvmStatic
    fun setServerUnreachableDatetime() = preferences.edit().putLong(SERVER_UNREACHABLE, System.currentTimeMillis()).apply()

    @JvmStatic
    fun isSyncronizationEnabled(): Boolean = preferences.getBoolean(QUEUE_SYNCING, false)
    @JvmStatic
    fun getSyncCountdownTimer(): Int = preferences.getString(QUEUE_SYNCING_COUNTDOWN, "5")!!.toInt()

    @JvmStatic
    fun isCornerRoundingEnabled(): Boolean = preferences.getBoolean(ROUNDED_CORNER, false)
    @JvmStatic
    fun getRoundedCornerSize(): Int = preferences.getString(ROUNDED_CORNER_SIZE, "12")!!.toInt()

    @JvmStatic
    fun isPodcastSectionVisible(): Boolean = preferences.getBoolean(PODCAST_SECTION_VISIBILITY, true)
    @JvmStatic
    fun setPodcastSectionHidden() = preferences.edit().putBoolean(PODCAST_SECTION_VISIBILITY, false).apply()

    @JvmStatic
    fun isRadioSectionVisible(): Boolean = preferences.getBoolean(RADIO_SECTION_VISIBILITY, true)
    @JvmStatic
    fun setRadioSectionHidden() = preferences.edit().putBoolean(RADIO_SECTION_VISIBILITY, false).apply()

    @JvmStatic
    fun isMusicDirectorySectionVisible(): Boolean = preferences.getBoolean(MUSIC_DIRECTORY_SECTION_VISIBILITY, true)

    @JvmStatic
    fun getReplayGainMode(): String? = preferences.getString(REPLAY_GAIN_MODE, "disabled")
    @JvmStatic
    fun isReplayGainPreventClipping(): Boolean = preferences.getBoolean(REPLAY_GAIN_PREVENT_CLIPPING, true)
    @JvmStatic
    fun getLoudnessPreamp(): Float = preferences.getInt(LOUDNESS_PREAMP, 0).toFloat()
    @JvmStatic
    fun setLoudnessPreamp(value: Float) = preferences.edit().putInt(LOUDNESS_PREAMP, value.toInt()).apply()

    @JvmStatic
    fun isServerPrioritized(): Boolean = preferences.getBoolean(AUDIO_TRANSCODE_PRIORITY, false)

    @JvmStatic
    fun getStreamingCacheStoragePreference(): Int = preferences.getString(STREAMING_CACHE_STORAGE, "0")!!.toInt()
    @JvmStatic
    fun setStreamingCacheStoragePreference(preference: Int) = preferences.edit().putString(STREAMING_CACHE_STORAGE, preference.toString()).apply()

    @JvmStatic
    fun getDownloadStoragePreference(): Int = preferences.getString(DOWNLOAD_STORAGE, "0")!!.toInt()
    @JvmStatic
    fun setDownloadStoragePreference(preference: Int) = preferences.edit().putString(DOWNLOAD_STORAGE, preference.toString()).apply()

    @JvmStatic
    fun getDownloadDirectoryUri(): String? = preferences.getString(DOWNLOAD_DIRECTORY_URI, null)
    @JvmStatic
    fun setDownloadDirectoryUri(uri: String?) = preferences.edit().putString(DOWNLOAD_DIRECTORY_URI, uri).apply()

    @JvmStatic
    fun getDefaultDownloadViewType(): String = preferences.getString(DEFAULT_DOWNLOAD_VIEW_TYPE, Constants.DOWNLOAD_TYPE_TRACK)!!
    @JvmStatic
    fun setDefaultDownloadViewType(viewType: String) = preferences.edit().putString(DEFAULT_DOWNLOAD_VIEW_TYPE, viewType).apply()

    @JvmStatic
    fun preferTranscodedDownload(): Boolean = preferences.getBoolean(AUDIO_TRANSCODE_DOWNLOAD, false)
    @JvmStatic
    fun isServerPrioritizedInTranscodedDownload(): Boolean = preferences.getBoolean(AUDIO_TRANSCODE_DOWNLOAD_PRIORITY, false)
    @JvmStatic
    fun getBitrateTranscodedDownload(): String = preferences.getString(MAX_BITRATE_DOWNLOAD, "0")!!
    @JvmStatic
    fun getAudioTranscodeFormatTranscodedDownload(): String = preferences.getString(AUDIO_TRANSCODE_FORMAT_DOWNLOAD, "raw")!!

    @JvmStatic
    fun isSharingEnabled(): Boolean = preferences.getBoolean(SHARE, false)
    @JvmStatic
    fun isScrobblingEnabled(): Boolean = preferences.getBoolean(SCROBBLING, true)

    @JvmStatic
    fun getSongPreloadBuffer(): Int = preferences.getString(SONG_PRELOAD_BUFFER, "60")!!.toInt()
    @JvmStatic
    fun getMinStarRatingAccepted(): Int = preferences.getInt(MIN_STAR_RATING, 0)
    @JvmStatic
    fun isDisplayAlwaysOn(): Boolean = preferences.getBoolean(ALWAYS_ON_DISPLAY, false)
    @JvmStatic
    fun showAudioQuality(): Boolean = preferences.getBoolean(AUDIO_QUALITY_PER_ITEM, false)

    @JvmStatic
    fun getHomeSectorList(): String? = preferences.getString(HOME_SECTOR_LIST, null)
    @JvmStatic
    fun setHomeSectorList(extension: List<HomeSector>?) = preferences.edit().putString(HOME_SECTOR_LIST, gson.toJson(extension)).apply()

    @JvmStatic
    fun showItemStarRating(): Boolean = preferences.getBoolean(SONG_RATING_PER_ITEM, false)
    @JvmStatic
    fun showItemRating(): Boolean = preferences.getBoolean(RATING_PER_ITEM, false)

    @JvmStatic
    fun isGithubUpdateEnabled(): Boolean = preferences.getBoolean(GITHUB_UPDATE_CHECK, true)
    @JvmStatic
    fun showTempusUpdateDialog(): Boolean = preferences.getLong(NEXT_UPDATE_CHECK, 0) + 86400000 < System.currentTimeMillis()
    @JvmStatic
    fun setTempusUpdateReminder() = preferences.edit().putLong(NEXT_UPDATE_CHECK, System.currentTimeMillis()).apply()

    @JvmStatic
    fun isContinuousPlayEnabled(): Boolean = preferences.getBoolean(CONTINUOUS_PLAY, true)
    @JvmStatic
    fun setLastInstantMix() = preferences.edit().putLong(LAST_INSTANT_MIX, System.currentTimeMillis()).apply()
    @JvmStatic
    fun isInstantMixUsable(): Boolean = preferences.getLong(LAST_INSTANT_MIX, 0) + 10000 < System.currentTimeMillis()

    @JvmStatic
    fun setAllowPlaylistDuplicates(allowDuplicates: Boolean) = preferences.edit().putBoolean(ALLOW_PLAYLIST_DUPLICATES, allowDuplicates).apply()
    @JvmStatic
    fun allowPlaylistDuplicates(): Boolean = preferences.getBoolean(ALLOW_PLAYLIST_DUPLICATES, false)

    @JvmStatic
    fun getHomeSortPlaylists(): String = preferences.getString(HOME_SORT_PLAYLISTS, DEFAULT_HOME_SORT_PLAYLISTS_SORT_ORDER) ?: DEFAULT_HOME_SORT_PLAYLISTS_SORT_ORDER
    @JvmStatic
    fun setHomeSortPlaylists(sortOrder: String) = preferences.edit().putString(HOME_SORT_PLAYLISTS, sortOrder).apply()

    @JvmStatic
    fun setEqualizerEnabled(enabled: Boolean) = preferences.edit().putBoolean(EQUALIZER_ENABLED, enabled).apply()
    @JvmStatic
    fun isEqualizerEnabled(): Boolean = preferences.getBoolean(EQUALIZER_ENABLED, false)

    @JvmStatic
    fun getSelectedEqualizer(): Int = preferences.getString(SELECTED_EQUALIZER, "0")!!.toInt()
    @JvmStatic
    fun setSelectedEqualizer(selectedEqualizer: String) = preferences.edit().putString(SELECTED_EQUALIZER, selectedEqualizer).apply()

    @JvmStatic
    fun setEqualizerBandLevels(bandLevels: ShortArray) {
        val asString = bandLevels.joinToString(",")
        preferences.edit().putString(EQUALIZER_BAND_LEVELS, asString).apply()
    }

    @JvmStatic
    fun getEqualizerBandLevels(bandCount: Short): ShortArray {
        val str = preferences.getString(EQUALIZER_BAND_LEVELS, null)
        if (str.isNullOrBlank()) return ShortArray(bandCount.toInt())
        val parts = str.split(",")
        if (parts.size < bandCount) return ShortArray(bandCount.toInt())
        return ShortArray(bandCount.toInt()) { i -> parts[i].toShortOrNull() ?: 0 }
    }

    @JvmStatic
    fun showAlbumDetail(): Boolean = preferences.getBoolean(ALBUM_DETAIL, false)
    @JvmStatic
    fun getAlbumSortOrder(): String = preferences.getString(ALBUM_SORT_ORDER, DEFAULT_ALBUM_SORT_ORDER) ?: DEFAULT_ALBUM_SORT_ORDER
    @JvmStatic
    fun setAlbumSortOrder(sortOrder: String) = preferences.edit().putString(ALBUM_SORT_ORDER, sortOrder).apply()

    @JvmStatic
    fun getArtistSortOrder(): String {
        val sortByAlbumCount = preferences.getBoolean(ARTIST_SORT_BY_ALBUM_COUNT, false)
        return if (sortByAlbumCount) Constants.ARTIST_ORDER_BY_ALBUM_COUNT else Constants.ARTIST_ORDER_BY_NAME
    }

    @JvmStatic
    fun isSearchSortingChronologicallyEnabled(): Boolean = preferences.getBoolean(SORT_SEARCH_CHRONOLOGICALLY, false)
    @JvmStatic
    fun getArtistDisplayBiography(): Boolean = preferences.getBoolean(ARTIST_DISPLAY_BIOGRAPHY, true)
    @JvmStatic
    fun setArtistDisplayBiography(enabled: Boolean) = preferences.edit().putBoolean(ARTIST_DISPLAY_BIOGRAPHY, enabled).apply()

    @JvmStatic
    fun getTileSize(): Int {
        val parsed = preferences.getString(TILE_SIZE, "2")?.toIntOrNull()
        return parsed?.takeIf { it in 2..6 } ?: 2
    }

    @JvmStatic
    fun isAndroidAutoAlbumViewEnabled(): Boolean = preferences.getBoolean(AA_ALBUM_VIEW, true)
    @JvmStatic
    fun isAndroidAutoHomeViewEnabled(): Boolean = preferences.getBoolean(AA_HOME_VIEW, false)
    @JvmStatic
    fun isAndroidAutoPlaylistViewEnabled(): Boolean = preferences.getBoolean(AA_PLAYLIST_VIEW, false)
    @JvmStatic
    fun isAndroidAutoPodcastViewEnabled(): Boolean = preferences.getBoolean(AA_PODCAST_VIEW, false)
    @JvmStatic
    fun isAndroidAutoRadioViewEnabled(): Boolean = preferences.getBoolean(AA_RADIO_VIEW, false)

    @JvmStatic
    fun getAndroidAutoFirstTab(): Int = preferences.getString(AA_FIRST_TAB, "0")!!.toInt()
    @JvmStatic
    fun getAndroidAutoSecondTab(): Int = preferences.getString(AA_SECOND_TAB, "1")!!.toInt()
    @JvmStatic
    fun getAndroidAutoThirdTab(): Int = preferences.getString(AA_THIRD_TAB, "2")!!.toInt()
    @JvmStatic
    fun getAndroidAutoFourthTab(): Int = preferences.getString(AA_FOURTH_TAB, "3")!!.toInt()

    @JvmStatic
    fun resetAndroidAutoFirstTab() = preferences.edit().putString(AA_FIRST_TAB, "-1").apply()
    @JvmStatic
    fun resetAndroidAutoSecondTab() = preferences.edit().putString(AA_SECOND_TAB, "-1").apply()
    @JvmStatic
    fun resetAndroidAutoThirdTab() = preferences.edit().putString(AA_THIRD_TAB, "-1").apply()
    @JvmStatic
    fun resetAndroidAutoFourthTab() = preferences.edit().putString(AA_FOURTH_TAB, "-1").apply()

    @JvmStatic
    fun isAndroidAutoShuffleGenreSongsEnabled(): Boolean = preferences.getBoolean(AA_SHUFFLE_GENRE_SONGS, false)
    @JvmStatic
    fun isAndroidAutoShuffleStarredTracksEnabled(): Boolean = preferences.getBoolean(AA_SHUFFLE_STARRED_TRACKS, false)
    @JvmStatic
    fun isAndroidAutoShufflePlaylistsEnabled(): Boolean = preferences.getBoolean(AA_SHUFFLE_PLAYLISTS, false)
    @JvmStatic
    fun setAndroidAutoShuffleGenreSongsEnabled(enabled: Boolean) = preferences.edit().putBoolean(AA_SHUFFLE_GENRE_SONGS, enabled).apply()

    @JvmStatic
    fun getAndroidAutoStarredForMadeForYou(): Int = preferences.getString(AA_STARRED_FOR_MADE_FOR_YOU, "0")!!.toInt()

    @JvmStatic
    fun getTheme(): String = preferences.getString(THEME, "default") ?: "default"
    @JvmStatic
    fun setTheme(theme: String) = preferences.edit().putString(THEME, theme).apply()

    @JvmStatic
    fun getDarkThemeStyle(): String = preferences.getString(DARK_THEME_STYLE, "standard") ?: "standard"
    @JvmStatic
    fun setDarkThemeStyle(style: String) = preferences.edit().putString(DARK_THEME_STYLE, style).apply()

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
