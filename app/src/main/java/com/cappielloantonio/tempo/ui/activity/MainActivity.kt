package com.cappielloantonio.tempo.ui.activity

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import androidx.activity.EdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.core.graphics.Insets
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.BuildConfig
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.broadcast.receiver.ConnectivityStatusBroadcastReceiver
import com.cappielloantonio.tempo.databinding.ActivityMainBinding
import com.cappielloantonio.tempo.github.utils.UpdateUtil
import com.cappielloantonio.tempo.navigation.BottomSheetController
import com.cappielloantonio.tempo.navigation.BottomSheetHelper
import com.cappielloantonio.tempo.navigation.NavigationController
import com.cappielloantonio.tempo.navigation.NavigationHelper
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.ui.activity.base.BaseActivity
import com.cappielloantonio.tempo.ui.dialog.ConnectionAlertDialog
import com.cappielloantonio.tempo.ui.dialog.GithubTempoUpdateDialog
import com.cappielloantonio.tempo.ui.dialog.ServerUnreachableDialog
import com.cappielloantonio.tempo.ui.fragment.PlayerBottomSheetFragment
import com.cappielloantonio.tempo.util.AssetLinkNavigator
import com.cappielloantonio.tempo.util.AssetLinkUtil
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.ExecutionException

@UnstableApi
class MainActivity : BaseActivity() {

    lateinit var bind: ActivityMainBinding
    private lateinit var mainViewModel: MainViewModel
    private var bottomSystemBarInset: Int = 0

    lateinit var navController: NavController
    lateinit var navigationController: NavigationController
    lateinit var bottomSheetController: BottomSheetController
    lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private var isLandscape = false
    private var assetLinkNavigator: AssetLinkNavigator? = null
    private var pendingAssetLink: AssetLinkUtil.AssetLink? = null

    private lateinit var connectivityStatusBroadcastReceiver: ConnectivityStatusBroadcastReceiver
    private var pendingDownloadPlaybackIntent: Intent? = null

    override fun isEdgeToEdgeEnabled(): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { false }
        EdgeToEdge.enable(
            this,
            SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        bind = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bind.root)
        applyEdgeToEdgeInsets()

        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        assetLinkNavigator = AssetLinkNavigator(this)

        connectivityStatusBroadcastReceiver = ConnectivityStatusBroadcastReceiver(this)
        connectivityStatusReceiverManager(true)

        isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        init()
        checkConnectionType()
        getOpenSubsonicExtensions()
        checkTempoUpdate()

        maybeSchedulePlaybackIntent(intent)
    }

    private fun applyEdgeToEdgeInsets() {
        val navHostPaddingLeft = bind.navHostFragment.paddingLeft
        val navHostPaddingTop = bind.navHostFragment.paddingTop
        val navHostPaddingRight = bind.navHostFragment.paddingRight
        val navHostPaddingBottom = bind.navHostFragment.paddingBottom

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
            bottomSystemBarInset = systemBars.bottom

            bind.navHostFragment.setPadding(
                navHostPaddingLeft,
                navHostPaddingTop + systemBars.top,
                navHostPaddingRight,
                navHostPaddingBottom
            )

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
            updateBottomSheetPeekHeight()

            windowInsets
        }
        ViewCompat.requestApplyInsets(bind.root)
    }

    private fun updateBottomSheetPeekHeight() {
        if (!::bottomSheetBehavior.isInitialized) return

        val playerHeaderPeekHeight = resources.getDimensionPixelSize(R.dimen.bottom_sheet_peek_height)
        val bottomOverlayHeight = if (bind.bottomNavigation.visibility == View.VISIBLE) {
            bind.bottomNavigation.layoutParams.height
        } else {
            bottomSystemBarInset
        }

        bottomSheetBehavior.peekHeight = playerHeaderPeekHeight + bottomOverlayHeight
    }

    override fun onStart() {
        super.onStart()
        pingServer()
        initService()
        consumePendingPlaybackIntent()
    }

    override fun onResume() {
        super.onResume()
        pingServer()
        toggleNavigationDrawerLockOnOrientationChange()
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityStatusReceiverManager(false)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        maybeSchedulePlaybackIntent(intent)
        consumePendingPlaybackIntent()
    }

    override fun onBackPressed() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            collapseBottomSheetDelayed()
        } else {
            super.onBackPressed()
        }
    }

    fun init() {
        initBottomSheet()
        initNavigation()

        if (Preferences.getPassword() != null || (Preferences.getToken() != null && Preferences.getSalt() != null)) {
            goFromLogin()
        } else {
            goToLogin()
        }

        toggleNavigationDrawerLockOnOrientationChange()
    }

    private fun initNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        navController = navHostFragment?.navController ?: throw IllegalStateException("NavController not found")

        val navigationHelper = NavigationHelper(
            findViewById(R.id.bottom_navigation),
            findViewById(R.id.bottom_navigation_frame),
            findViewById(R.id.drawer_layout),
            findViewById(R.id.nav_view),
            navHostFragment,
            navController
        )

        navigationController = NavigationController(navigationHelper)
        navigationController.syncWithBottomSheetBehavior(bottomSheetBehavior, navController)
    }

    private fun initBottomSheet() {
        val bottomSheetView = findViewById<View>(R.id.player_bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView)

        val bottomSheetHelper = BottomSheetHelper(
            bottomSheetBehavior,
            bottomSheetView,
            supportFragmentManager
        )

        bottomSheetController = BottomSheetController(bottomSheetHelper)
        bottomSheetController.addCallback(bottomSheetCallback)
        bottomSheetController.replaceFragment(R.id.player_bottom_sheet)
        bottomSheetController.checkAfterStateChanged(mainViewModel)
        updateBottomSheetPeekHeight()
    }

    fun setBottomSheetInPeek(isVisible: Boolean) {
        bottomSheetController.setStateInPeek(isVisible)
    }

    fun setBottomSheetVisibility(visibility: Boolean) {
        bottomSheetController.setVisibility(visibility)
    }

    fun collapseBottomSheetDelayed() {
        bottomSheetController.collapseDelayed()
    }

    fun expandBottomSheet() {
        bottomSheetController.expand()
    }

    fun setBottomSheetDraggableState(isDraggable: Boolean) {
        bottomSheetController.setDraggable(isDraggable)
    }

    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        private var navigationHeight = 0

        override fun onStateChanged(view: View, state: Int) {
            val playerBottomSheetFragment = supportFragmentManager.findFragmentByTag("PlayerBottomSheet") as? PlayerBottomSheetFragment

            when (state) {
                BottomSheetBehavior.STATE_HIDDEN -> resetMusicSession()
                BottomSheetBehavior.STATE_COLLAPSED -> playerBottomSheetFragment?.goBackToFirstPage()
                else -> {}
            }
        }

        override fun onSlide(view: View, slideOffset: Float) {
            animateBottomSheet(slideOffset)
            if (!isLandscape) {
                animateBottomNavigation(slideOffset)
            }
        }

        private fun animateBottomNavigation(slideOffset: Float) {
            if (slideOffset < 0) return

            if (navigationHeight == 0) {
                navigationHeight = bind.bottomNavigation.height
            }

            val slideY = navigationHeight - navigationHeight * (1 - slideOffset)
            bind.bottomNavigation.translationY = slideY
        }
    }

    private fun animateBottomSheet(slideOffset: Float) {
        bottomSheetController.animate(slideOffset)
    }

    fun setBottomNavigationBarVisibility(visibility: Boolean) {
        navigationController.setNavbarVisibility(visibility)
    }

    fun toggleBottomNavigationBarVisibilityOnOrientationChange() {
        if (Preferences.getHideBottomNavbarOnPortrait()) {
            navigationController.setNavbarVisibility(false)
            navigationController.setSystemBarsVisibility(this, !isLandscape)
            updateBottomSheetPeekHeight()
            ViewCompat.requestApplyInsets(bind.root)
            return
        }

        if (!isLandscape) {
            navigationController.setNavbarVisibility(true)
            navigationController.setSystemBarsVisibility(this, true)
        } else {
            navigationController.setNavbarVisibility(false)
            navigationController.setSystemBarsVisibility(this, false)
        }
        updateBottomSheetPeekHeight()
        ViewCompat.requestApplyInsets(bind.root)
    }

    fun setNavigationDrawerLock(locked: Boolean) {
        navigationController.setDrawerLock(locked)
    }

    fun isNavigationDrawerLocked(): Boolean = navigationController.isNavigationDrawerLocked

    fun toggleNavigationDrawerLockOnOrientationChange() {
        navigationController.toggleDrawerLockOnOrientation(this)
    }

    fun setSystemBarsVisibility(visibility: Boolean) {
        navigationController.setSystemBarsVisibility(this, visibility)
    }

    private fun initService() {
        MediaManager.check(mediaBrowserListenableFuture)

        mediaBrowserListenableFuture?.addListener({
            try {
                mediaBrowserListenableFuture?.get()?.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying && bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                            setBottomSheetInPeek(true)
                        }
                    }
                })
                if (mediaBrowserListenableFuture?.get()?.currentMediaItem != null
                    && bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN
                ) {
                    setBottomSheetInPeek(true)
                }
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun goToLogin() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        setBottomNavigationBarVisibility(false)
        setBottomSheetVisibility(false)

        val nc = navigationController.navController
        val id = nc.currentDestination?.id

        when (id) {
            R.id.landingFragment -> nc.navigate(R.id.action_landingFragment_to_loginFragment)
            R.id.settingsFragment -> nc.navigate(R.id.action_settingsFragment_to_loginFragment)
            R.id.homeFragment -> nc.navigate(R.id.action_homeFragment_to_loginFragment)
        }
    }

    private fun goToHome() {
        setBottomNavigationBarVisibility(true)

        val nc = navigationController.navController
        val id = nc.currentDestination?.id

        when (id) {
            R.id.landingFragment -> nc.navigate(R.id.action_landingFragment_to_homeFragment)
            R.id.loginFragment -> nc.navigate(R.id.action_loginFragment_to_homeFragment)
        }
    }

    fun goFromLogin() {
        setBottomSheetInPeek(mainViewModel.isQueueLoaded)
        goToHome()
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
        if (collapsePlayer) {
            setBottomSheetInPeek(true)
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

    private fun connectivityStatusReceiverManager(isActive: Boolean) {
        if (isActive) {
            val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            registerReceiver(connectivityStatusBroadcastReceiver, filter)
        } else {
            unregisterReceiver(connectivityStatusBroadcastReceiver)
        }
    }

    private fun pingServer() {
        if (Preferences.getToken() == null && Preferences.getPassword() == null) return

        if (Preferences.isInUseServerAddressLocal) {
            mainViewModel.ping().observe(this) { subsonicResponse ->
                if (subsonicResponse == null) {
                    Preferences.setServerSwitchableTimer()
                    Preferences.switchInUseServerAddress()
                    App.refreshSubsonicClient()
                    pingServer()
                    resetView()
                } else {
                    Preferences.setOpenSubsonic(subsonicResponse.openSubsonic ?: false)
                }
            }
        } else {
            if (Preferences.isServerSwitchable()) {
                Preferences.setServerSwitchableTimer()
                Preferences.switchInUseServerAddress()
                App.refreshSubsonicClient()
                pingServer()
                resetView()
            } else {
                mainViewModel.ping().observe(this) { subsonicResponse ->
                    if (subsonicResponse == null) {
                        if (Preferences.showServerUnreachableDialog()) {
                            val dialog = ServerUnreachableDialog()
                            dialog.show(supportFragmentManager, null)
                        }
                    } else {
                        Preferences.setOpenSubsonic(subsonicResponse.openSubsonic ?: false)
                    }
                }
            }
        }
    }

    private fun resetView() {
        resetViewModel()
        val nc = navigationController.navController
        val id = nc.currentDestination?.id ?: return
        nc.popBackStack(id, true)
        nc.navigate(id)
    }

    private fun getOpenSubsonicExtensions() {
        if (Preferences.getToken() != null || Preferences.getPassword() != null) {
            mainViewModel.openSubsonicExtensions.observe(this) { openSubsonicExtensions ->
                if (openSubsonicExtensions != null) {
                    Preferences.setOpenSubsonicExtensions(openSubsonicExtensions)
                }
            }
        }
    }

    private fun checkTempoUpdate() {
        if (BuildConfig.FLAVOR == "tempus" && Preferences.isGithubUpdateEnabled() && Preferences.showTempusUpdateDialog()) {
            mainViewModel.checkTempoUpdate().observe(this) { latestRelease ->
                if (latestRelease != null && UpdateUtil.showUpdateDialog(latestRelease)) {
                    val dialog = GithubTempoUpdateDialog(latestRelease)
                    dialog.show(supportFragmentManager, null)
                }
            }
        }
    }

    private fun checkConnectionType() {
        if (Preferences.isWifiOnly) {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo

            if (networkInfo != null && networkInfo.type != ConnectivityManager.TYPE_WIFI) {
                val dialog = ConnectionAlertDialog()
                dialog.show(supportFragmentManager, null)
            }
        }
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

    private fun isUserAuthenticated(): Boolean =
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
