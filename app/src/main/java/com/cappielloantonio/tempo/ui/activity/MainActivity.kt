package com.cappielloantonio.tempo.ui.activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.navigation.NavController
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.ActivityMainBinding
import com.cappielloantonio.tempo.github.utils.UpdateUtil
import com.cappielloantonio.tempo.navigation.Hosts
import com.cappielloantonio.tempo.navigation.RootContainer
import com.cappielloantonio.tempo.navigation.navigateSingleTop
import com.cappielloantonio.tempo.navigation.replace
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.activity.base.BaseActivity
import com.cappielloantonio.tempo.ui.album.AlbumCatalogueRouteScreen
import com.cappielloantonio.tempo.ui.album.AlbumPageRouteScreen
import com.cappielloantonio.tempo.ui.artist.ArtistCatalogueRouteScreen
import com.cappielloantonio.tempo.ui.artist.ArtistPageRouteScreen
import com.cappielloantonio.tempo.ui.dialog.DialogRouteScreen
import com.cappielloantonio.tempo.ui.dialog.GithubTempoUpdateRouteDialog
import com.cappielloantonio.tempo.ui.download.DownloadRouteScreen
import com.cappielloantonio.tempo.ui.equalizer.EqualizerRouteScreen
import com.cappielloantonio.tempo.ui.auth.LoginRouteScreen
import com.cappielloantonio.tempo.ui.genre.GenreCatalogueRouteScreen
import com.cappielloantonio.tempo.ui.home.HomeRouteScreen
import com.cappielloantonio.tempo.ui.home.LibraryRouteScreen
import com.cappielloantonio.tempo.ui.player.PlayerRouteScreen
import com.cappielloantonio.tempo.ui.playlist.PlaylistCatalogueRouteScreen
import com.cappielloantonio.tempo.ui.playlist.PlaylistPageRouteScreen
import com.cappielloantonio.tempo.ui.search.SearchRouteScreen
import com.cappielloantonio.tempo.ui.settings.SettingsRouteScreen
import com.cappielloantonio.tempo.ui.song.SongBottomSheetRouteScreen
import com.cappielloantonio.tempo.ui.song.SongListPageRouteScreen
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.AssetLinkNavigator
import com.cappielloantonio.tempo.util.AssetLinkUtil
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.viewmodel.MainViewModel
import com.cappielloantonio.tempo.viewmodel.SongListPageArgs
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
class MainActivity : BaseActivity() {
    lateinit var bind: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModel()
    lateinit var navController: NavController
    private var isLandscape = false
    private var assetLinkNavigator: AssetLinkNavigator? = null
    private var pendingAssetLink: AssetLinkUtil.AssetLink? = null
    private var pendingDownloadPlaybackIntent: Intent? = null
    private var pendingUpdateReleaseUrl: String? = null
    private var isComposeBottomBarEnabled by mutableStateOf(true)
    override fun isEdgeToEdgeEnabled(): Boolean = true
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { false }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        window.isNavigationBarContrastEnforced = false
        bind = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bind.root)
        bind.bottomNavigation.visibility = View.GONE
        bind.bottomNavigationFrame.visibility = View.GONE
        applyEdgeToEdgeInsets()
        initComposeRoot()
        initDrawerNavigation()
        assetLinkNavigator = AssetLinkNavigator(this)
        isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        init()
        getOpenSubsonicExtensions()
        checkTempoUpdate()
        observeNetworkStatus()
        maybeSchedulePlaybackIntent(intent)
    }
    private fun initComposeRoot() {
        bind.composeRoot.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        bind.composeRoot.setContent {
            TempusTheme {
                RootContainer(
                    isBottomBarEnabled = isComposeBottomBarEnabled,
                    onBottomBarVisibilityChanged = {},
                    onNavControllerReady = { controller ->
                        navController = controller
                        if (isUserAuthenticated()) {
                            consumePendingAssetLink()
                        }
                        consumePendingUpdateDialog()
                    },
                )
            }
        }
    }
    private fun initDrawerNavigation() {
        bind.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.searchRoute -> openSearchRoute()
                R.id.homeRoute -> navController.navigateSingleTop(HomeRouteScreen.screenName)
                R.id.libraryRoute -> navController.navigateSingleTop(LibraryRouteScreen.screenName)
                R.id.downloadRoute -> navController.navigateSingleTop(DownloadRouteScreen.screenName)
                R.id.settingsRoute -> navController.navigate(SettingsRouteScreen.screenName)
                R.id.albumCatalogueRoute -> navController.navigate(AlbumCatalogueRouteScreen.screenName)
                R.id.artistCatalogueRoute -> navController.navigate(ArtistCatalogueRouteScreen.screenName)
                R.id.genreCatalogueRoute -> navController.navigate(GenreCatalogueRouteScreen.screenName)
                R.id.playlistCatalogueRoute -> {
                    navController.navigate(PlaylistCatalogueRouteScreen.route(Constants.PLAYLIST_ALL))
                }
                else -> return@setNavigationItemSelectedListener false
            }
            item.isChecked = true
            bind.drawerLayout.closeDrawers()
            true
        }
    }
    private fun applyEdgeToEdgeInsets() {
        val offlinePaddingLeft = bind.offlineModeTextView.paddingLeft
        val offlinePaddingTop = bind.offlineModeTextView.paddingTop
        val offlinePaddingRight = bind.offlineModeTextView.paddingRight
        val offlinePaddingBottom = bind.offlineModeTextView.paddingBottom
        val navigationViewPaddingLeft = bind.navView.paddingLeft
        val navigationViewPaddingTop = bind.navView.paddingTop
        val navigationViewPaddingRight = bind.navView.paddingRight
        val navigationViewPaddingBottom = bind.navView.paddingBottom
        val bottomNavigationPaddingLeft = bind.bottomNavigation.paddingLeft
        val bottomNavigationPaddingTop = bind.bottomNavigation.paddingTop
        val bottomNavigationPaddingRight = bind.bottomNavigation.paddingRight
        val bottomNavigationPaddingBottom = bind.bottomNavigation.paddingBottom
        val bottomNavigationHeight = bind.bottomNavigation.layoutParams.height
        ViewCompat.setOnApplyWindowInsetsListener(bind.root) { _, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            bind.offlineModeTextView.setPadding(
                offlinePaddingLeft,
                offlinePaddingTop + systemBars.top,
                offlinePaddingRight,
                offlinePaddingBottom
            )
            bind.navView.setPadding(
                navigationViewPaddingLeft,
                navigationViewPaddingTop + systemBars.top,
                navigationViewPaddingRight,
                navigationViewPaddingBottom
            )
            bind.bottomNavigation.layoutParams.height = bottomNavigationHeight + systemBars.bottom
            bind.bottomNavigation.setPadding(
                bottomNavigationPaddingLeft,
                bottomNavigationPaddingTop,
                bottomNavigationPaddingRight,
                bottomNavigationPaddingBottom + systemBars.bottom
            )
            windowInsets
        }
        ViewCompat.requestApplyInsets(bind.root)
    }
    override fun onStart() {
        super.onStart()
        refreshConnectionState()
        consumePendingPlaybackIntent()
    }
    override fun onResume() {
        super.onResume()
        toggleNavigationDrawerLockOnOrientationChange()
    }
    override fun onDestroy() {
        super.onDestroy()
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        maybeSchedulePlaybackIntent(intent)
        consumePendingPlaybackIntent()
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }
    fun init() {
        toggleNavigationDrawerLockOnOrientationChange()
    }
    fun setBottomSheetInPeek(isVisible: Boolean) {
    }
    fun setBottomSheetVisibility(visibility: Boolean) {
    }
    fun collapseBottomSheetDelayed() {
        if (::navController.isInitialized &&
            navController.currentDestination?.route == PlayerRouteScreen.screenName
        ) {
            navController.navigateUp()
        }
    }
    fun setBottomNavigationBarVisibility(visibility: Boolean) {
        isComposeBottomBarEnabled = visibility
        ViewCompat.requestApplyInsets(bind.root)
    }
    fun toggleBottomNavigationBarVisibilityOnOrientationChange() {
        if (Preferences.getHideBottomNavbarOnPortrait()) {
            isComposeBottomBarEnabled = false
            setSystemBarsVisibility(!isLandscape)
            ViewCompat.requestApplyInsets(bind.root)
            return
        }
        if (!isLandscape) {
            isComposeBottomBarEnabled = true
            setSystemBarsVisibility(true)
        } else {
            isComposeBottomBarEnabled = false
            setSystemBarsVisibility(false)
        }
        ViewCompat.requestApplyInsets(bind.root)
    }
    fun setNavigationDrawerLock(locked: Boolean) {
        bind.drawerLayout.setDrawerLockMode(
            if (locked) {
                androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED
            } else {
                androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED
            }
        )
    }
    fun toggleNavigationDrawerLockOnOrientationChange() {
        val shouldLock = if (Preferences.getEnableDrawerOnPortrait()) {
            false
        } else {
            resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
        }
        setNavigationDrawerLock(shouldLock)
    }
    fun setSystemBarsVisibility(visibility: Boolean) {
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        if (visibility) {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            insetsController.show(WindowInsetsCompat.Type.navigationBars())
            insetsController.show(WindowInsetsCompat.Type.statusBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            insetsController.hide(WindowInsetsCompat.Type.navigationBars())
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    fun goToLogin() {
        setBottomNavigationBarVisibility(false)
        if (::navController.isInitialized) {
            navController.replace(LoginRouteScreen.screenName)
        }
    }
    private fun goToHome() {
        setBottomNavigationBarVisibility(true)
        if (::navController.isInitialized) {
            navController.replace(Hosts.Main.route)
        }
    }
    fun goFromLogin() {
        goToHome()
        refreshConnectionState()
        consumePendingAssetLink()
    }
    fun openAssetLink(assetLink: AssetLinkUtil.AssetLink) {
        openAssetLink(assetLink, true)
    }
    fun openAssetLink(assetLink: AssetLinkUtil.AssetLink, collapsePlayer: Boolean) {
        if (!isUserAuthenticated()) {
            pendingAssetLink = assetLink
            return
        }
        assetLinkNavigator?.open(assetLink)
    }
    fun quit() {
        resetUserSession()
        resetMusicSession()
        App.refreshSubsonicClient()
        resetViewModel()
        goToLogin()
    }
    fun openEqualizerRoute() {
        if (::navController.isInitialized) {
            navController.navigate(EqualizerRouteScreen.screenName)
        }
    }
    fun openAlbumRoute(albumId: String) {
        if (::navController.isInitialized && albumId.isNotBlank()) {
            navController.navigate(AlbumPageRouteScreen.route(albumId))
        }
    }
    fun openArtistRoute(artistId: String) {
        if (::navController.isInitialized && artistId.isNotBlank()) {
            navController.navigate(ArtistPageRouteScreen.route(artistId))
        }
    }
    fun openPlaylistRoute(playlistId: String) {
        if (::navController.isInitialized && playlistId.isNotBlank()) {
            navController.navigate(PlaylistPageRouteScreen.route(playlistId))
        }
    }
    fun openSongListRoute(args: SongListPageArgs) {
        if (::navController.isInitialized) {
            navController.navigate(SongListPageRouteScreen.route(args))
        }
    }
    fun openSearchRoute() {
        if (::navController.isInitialized) {
            navController.navigate(SearchRouteScreen.screenName)
        }
    }
    fun openSongBottomSheetRoute(
        song: Child,
        playlistId: String? = null,
        itemPosition: Int = -1,
    ) {
        if (::navController.isInitialized) {
            navController.navigate(SongBottomSheetRouteScreen.route(song, playlistId, itemPosition))
        }
    }
    private fun resetUserSession() {
        Preferences.setServerId(null)
        Preferences.setSalt(null)
        Preferences.setToken(null)
        Preferences.setPassword(null)
        Preferences.setServer(null)
        Preferences.setLocalAddress(null)
        Preferences.setUser(null)
        Preferences.setClientCert(null)
        Preferences.setOpenSubsonic(false)
        Preferences.setPlaybackSpeed(1.0f)
        Preferences.setSkipSilenceMode(false)
        Preferences.setDataSavingMode(false)
        Preferences.setStarredSyncEnabled(false)
        Preferences.setStarredAlbumsSyncEnabled(false)
    }
    private fun resetMusicSession() {
        MediaManager.reset(mediaBrowserListenableFuture)
    }
    private fun resetViewModel() {
        viewModelStore.clear()
    }
    private fun observeNetworkStatus() {
        lifecycleScope.launch {
            mainViewModel.connectionState.collect { state ->
                bind.offlineModeTextView.isVisible = !state.isNetworkAvailable || !state.isServerAvailable
                if (!state.isNetworkAvailable) {
                    bind.offlineModeTextView.setText(R.string.no_internet)
                } else if (!state.isServerAvailable) {
                    bind.offlineModeTextView.setText(R.string.server_unreachable)
                }
            }
        }
    }
    private fun refreshConnectionState() {
        mainViewModel.syncSelectedServerFromPreferences()
        mainViewModel.refreshConnectionState()
    }
    private fun getOpenSubsonicExtensions() {
        if (Preferences.getToken() != null || Preferences.getPassword() != null) {
            mainViewModel.getOpenSubsonicExtensions().observe(this) { openSubsonicExtensions ->
                if (openSubsonicExtensions != null) {
                    Preferences.setOpenSubsonicExtensions(openSubsonicExtensions)
                }
            }
        }
    }
    private fun checkTempoUpdate() {
        if (Preferences.isGithubUpdateEnabled() && Preferences.showTempusUpdateDialog()) {
            mainViewModel.checkTempoUpdate().observe(this) { latestRelease ->
                if (latestRelease != null && UpdateUtil.showUpdateDialog(latestRelease)) {
                    showUpdateDialog(latestRelease.htmlUrl.orEmpty())
                }
            }
        }
    }
    private fun showUpdateDialog(releaseUrl: String) {
        if (!::navController.isInitialized) {
            pendingUpdateReleaseUrl = releaseUrl
            return
        }
        navController.navigate(
            DialogRouteScreen.route { _, onClose ->
                GithubTempoUpdateRouteDialog(
                    releaseUrl = releaseUrl,
                    onDismiss = onClose,
                )
            },
        )
    }
    private fun consumePendingUpdateDialog() {
        val releaseUrl = pendingUpdateReleaseUrl ?: return
        pendingUpdateReleaseUrl = null
        showUpdateDialog(releaseUrl)
    }
    private fun maybeSchedulePlaybackIntent(intent: Intent?) {
        if (intent == null) return
        if (Constants.ACTION_PLAY_EXTERNAL_DOWNLOAD == intent.action
            || intent.hasExtra(Constants.EXTRA_DOWNLOAD_URI)
        ) {
            pendingDownloadPlaybackIntent = Intent(intent)
        }
        handleAssetLinkIntent(intent)
    }
    private fun consumePendingPlaybackIntent() {
        val intent = pendingDownloadPlaybackIntent ?: return
        pendingDownloadPlaybackIntent = null
        playDownloadedMedia(intent)
    }
    private fun handleAssetLinkIntent(intent: Intent) {
        val assetLink = AssetLinkUtil.parse(intent) ?: return
        if (!isUserAuthenticated()) {
            pendingAssetLink = assetLink
            intent.data = null
            return
        }
        assetLinkNavigator?.open(assetLink)
        intent.data = null
    }
    fun isUserAuthenticated(): Boolean =
        Preferences.getPassword() != null || (Preferences.getToken() != null && Preferences.getSalt() != null)
    private fun consumePendingAssetLink() {
        val assetLink = pendingAssetLink ?: return
        assetLinkNavigator?.open(assetLink)
        pendingAssetLink = null
    }
    private fun playDownloadedMedia(intent: Intent) {
        val uriString = intent.getStringExtra(Constants.EXTRA_DOWNLOAD_URI)
        if (TextUtils.isEmpty(uriString)) return
        val uri = Uri.parse(uriString)
        var mediaId = intent.getStringExtra(Constants.EXTRA_DOWNLOAD_MEDIA_ID)
        if (TextUtils.isEmpty(mediaId)) {
            mediaId = uri.toString()
        }
        val title = intent.getStringExtra(Constants.EXTRA_DOWNLOAD_TITLE)
        val artist = intent.getStringExtra(Constants.EXTRA_DOWNLOAD_ARTIST)
        val album = intent.getStringExtra(Constants.EXTRA_DOWNLOAD_ALBUM)
        val duration = intent.getIntExtra(Constants.EXTRA_DOWNLOAD_DURATION, 0)
        val extras = Bundle().apply {
            putString("id", mediaId)
            putString("title", title)
            putString("artist", artist)
            putString("album", album)
            putString("uri", uri.toString())
            putString("type", Constants.MEDIA_TYPE_MUSIC)
            putInt("duration", duration)
        }
        val metadataBuilder = MediaMetadata.Builder()
            .setExtras(extras)
            .setIsBrowsable(false)
            .setIsPlayable(true)
        if (!TextUtils.isEmpty(title)) metadataBuilder.setTitle(title)
        if (!TextUtils.isEmpty(artist)) metadataBuilder.setArtist(artist)
        if (!TextUtils.isEmpty(album)) metadataBuilder.setAlbumTitle(album)
        val mediaItem = MediaItem.Builder()
            .setMediaId(mediaId!!)
            .setMediaMetadata(metadataBuilder.build())
            .setUri(uri)
            .setMimeType(MimeTypes.BASE_TYPE_AUDIO)
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(uri)
                    .setExtras(extras)
                    .build()
            )
            .build()
        MediaManager.playDownloadedMediaItem(mediaBrowserListenableFuture, mediaItem)
    }
}
