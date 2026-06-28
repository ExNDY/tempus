package com.cappielloantonio.tempo.ui.activity

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import cat.ereza.customactivityoncrash.CustomActivityOnCrash
import cat.ereza.customactivityoncrash.config.CaocConfig
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.ActivityCrashBinding
import com.cappielloantonio.tempo.ui.fragment.CrashExportFragment
import com.cappielloantonio.tempo.ui.fragment.CrashInfoFragment
import com.cappielloantonio.tempo.ui.fragment.CrashLogsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.color.DynamicColors
import com.google.android.material.navigation.NavigationView

@UnstableApi
class CrashActivity : AppCompatActivity() {

    private var _bind: ActivityCrashBinding? = null
    private val bind get() = _bind!!

    var stackTrace: String? = null
        private set

    var configFromIntent: CaocConfig? = null
        private set

    private var toolbar: Toolbar? = null
    private var bottomNav: BottomNavigationView? = null
    private var drawerLayout: DrawerLayout? = null
    private var navView: NavigationView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        DynamicColors.applyToActivityIfAvailable(this)

        super.onCreate(savedInstanceState)

        window.isNavigationBarContrastEnforced = false

        _bind = ActivityCrashBinding.inflate(layoutInflater)
        setContentView(bind.root)
        applyEdgeToEdgeInsets()

        stackTrace = CustomActivityOnCrash.getStackTraceFromIntent(intent)
        configFromIntent = CustomActivityOnCrash.getConfigFromIntent(intent)

        init()
        initAppBar()
    }

    override fun onDestroy() {
        super.onDestroy()
        _bind = null
    }

    private fun init() {
        toolbar = findViewById(R.id.crash_toolbar)
        setSupportActionBar(toolbar)

        // These will be null if the view is not present in the current layout
        bottomNav = findViewById(R.id.crash_bottom_nav)
        drawerLayout = findViewById(R.id.crash_drawer_layout)
        navView = findViewById(R.id.crash_nav_view)

        bottomNav?.let { setupBottomNav(it) }
        if (drawerLayout != null && navView != null) {
            setupDrawer(drawerLayout!!, navView!!)
        }
    }

    private fun applyEdgeToEdgeInsets() {
        val toolbarPaddingLeft = bind.crashToolbar.paddingLeft
        val toolbarPaddingTop = bind.crashToolbar.paddingTop
        val toolbarPaddingRight = bind.crashToolbar.paddingRight
        val toolbarPaddingBottom = bind.crashToolbar.paddingBottom

        val contentPaddingLeft = bind.crashContentFrame.paddingLeft
        val contentPaddingTop = bind.crashContentFrame.paddingTop
        val contentPaddingRight = bind.crashContentFrame.paddingRight
        val contentPaddingBottom = bind.crashContentFrame.paddingBottom

        val drawerPaddingLeft = bind.crashDrawerLayout.paddingLeft
        val drawerPaddingTop = bind.crashDrawerLayout.paddingTop
        val drawerPaddingRight = bind.crashDrawerLayout.paddingRight
        val drawerPaddingBottom = bind.crashDrawerLayout.paddingBottom

        val bottomNavPaddingLeft = bind.crashBottomNav?.paddingLeft ?: 0
        val bottomNavPaddingTop = bind.crashBottomNav?.paddingTop ?: 0
        val bottomNavPaddingRight = bind.crashBottomNav?.paddingRight ?: 0
        val bottomNavPaddingBottom = bind.crashBottomNav?.paddingBottom ?: 0
        val bottomNavHeight = bind.crashBottomNav?.layoutParams?.height ?: ViewGroup.LayoutParams.WRAP_CONTENT

        val navViewPaddingLeft = bind.crashNavView?.paddingLeft ?: 0
        val navViewPaddingTop = bind.crashNavView?.paddingTop ?: 0
        val navViewPaddingRight = bind.crashNavView?.paddingRight ?: 0
        val navViewPaddingBottom = bind.crashNavView?.paddingBottom ?: 0

        ViewCompat.setOnApplyWindowInsetsListener(bind.root) { _, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            bind.crashDrawerLayout.setPadding(
                drawerPaddingLeft,
                drawerPaddingTop,
                drawerPaddingRight,
                drawerPaddingBottom
            )

            bind.crashToolbar.setPadding(
                toolbarPaddingLeft,
                toolbarPaddingTop + systemBars.top,
                toolbarPaddingRight,
                toolbarPaddingBottom
            )

            bind.crashContentFrame.setPadding(
                contentPaddingLeft,
                contentPaddingTop,
                contentPaddingRight,
                contentPaddingBottom
            )

            bind.crashBottomNav?.let { nav ->
                val layoutParams = nav.layoutParams
                if (bottomNavHeight > 0) {
                    layoutParams.height = bottomNavHeight + systemBars.bottom
                }
                nav.layoutParams = layoutParams
                nav.setPadding(
                    bottomNavPaddingLeft,
                    bottomNavPaddingTop,
                    bottomNavPaddingRight,
                    bottomNavPaddingBottom + systemBars.bottom
                )
            } ?: run {
                bind.crashContentFrame.setPadding(
                    contentPaddingLeft,
                    contentPaddingTop,
                    contentPaddingRight,
                    contentPaddingBottom + systemBars.bottom
                )
            }

            bind.crashNavView?.setPadding(
                navViewPaddingLeft,
                navViewPaddingTop + systemBars.top,
                navViewPaddingRight,
                navViewPaddingBottom + systemBars.bottom
            )

            windowInsets
        }
        ViewCompat.requestApplyInsets(bind.root)
    }

    private fun initAppBar() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape && drawerLayout != null && toolbar != null) {
            val drawerToggle = ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.toolbar_navigation_drawer_open,
                R.string.toolbar_navigation_drawer_closed
            )
            drawerLayout!!.addDrawerListener(drawerToggle)
            drawerToggle.syncState()
        }
    }

    private fun setupBottomNav(nav: BottomNavigationView) {
        val toolbarTitle = "Crash Landing"
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.crash_nav_info -> {
                    loadFragment(CrashInfoFragment(), toolbarTitle)
                    true
                }
                R.id.crash_nav_logs -> {
                    loadFragment(CrashLogsFragment(), toolbarTitle)
                    true
                }
                R.id.crash_nav_export -> {
                    loadFragment(CrashExportFragment(), toolbarTitle)
                    true
                }
                else -> false
            }
        }

        loadFragment(CrashInfoFragment(), toolbarTitle)
        nav.menu.findItem(R.id.crash_nav_info).isChecked = true
    }

    private fun setupDrawer(drawer: DrawerLayout, navigationView: NavigationView) {
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.crash_nav_info -> loadFragment(CrashInfoFragment(), getString(R.string.ca_info_title))
                R.id.crash_nav_logs -> loadFragment(CrashLogsFragment(), getString(R.string.ca_logs_title))
                R.id.crash_nav_export -> loadFragment(CrashExportFragment(), getString(R.string.ca_export_title))
            }
            drawer.closeDrawers()
            true
        }
        loadFragment(CrashInfoFragment(), getString(R.string.ca_info_title))
        navigationView.setCheckedItem(R.id.crash_nav_info)
    }

    private fun loadFragment(fragment: Fragment, toolbarTitle: String) {
        supportActionBar?.title = toolbarTitle
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.crash_content_frame, fragment)
            .setReorderingAllowed(true)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        private const val TAG = "MainActivityLogs"
    }
}
