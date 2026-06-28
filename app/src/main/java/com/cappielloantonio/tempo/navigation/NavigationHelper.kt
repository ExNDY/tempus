package com.cappielloantonio.tempo.navigation

import android.content.res.Configuration
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.util.Preferences
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView

class NavigationHelper(
    val bottomNavigationView: BottomNavigationView,
    val bottomNavigationViewFrame: FrameLayout,
    val drawerLayout: DrawerLayout,
    private val navigationView: NavigationView,
    private val navHostFragment: NavHostFragment,
    val navController: NavController
) {
    fun syncWithBottomSheetBehavior(
        bottomSheetBehavior: BottomSheetBehavior<View>,
        navController: NavController
    ) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // React to the user clicking one of these on bottom-navbar/drawer
            val isTarget = isTargetDestination(destination)
            val currentState = bottomSheetBehavior.state
            if (isTarget && currentState == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
        NavigationUI.setupWithNavController(bottomNavigationView, navController)
        NavigationUI.setupWithNavController(navigationView, navController)
    }

    private fun isTargetDestination(destination: NavDestination): Boolean {
        val destId = destination.id
        return destId == R.id.homeFragment ||
                destId == R.id.libraryFragment ||
                destId == R.id.downloadFragment ||
                destId == R.id.albumCatalogueFragment ||
                destId == R.id.artistCatalogueFragment ||
                destId == R.id.genreCatalogueFragment ||
                destId == R.id.playlistCatalogueFragment
    }

    /*
    Clean public methods
    Removes the need to invoke the activity on the fragment
     */
    fun setBottomNavigationBarVisibility(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        bottomNavigationView.visibility = visibility
        bottomNavigationViewFrame.visibility = visibility
    }

    fun setNavigationDrawerLock(locked: Boolean) {
        val mode = if (locked) DrawerLayout.LOCK_MODE_LOCKED_CLOSED else DrawerLayout.LOCK_MODE_UNLOCKED
        drawerLayout.setDrawerLockMode(mode)
    }

    fun isNavigationDrawerLocked(): Boolean {
        return drawerLayout.getDrawerLockMode(navigationView) != DrawerLayout.LOCK_MODE_UNLOCKED
    }

    @OptIn(UnstableApi::class)
    fun toggleNavigationDrawerLockOnOrientationChange(activity: AppCompatActivity) {
        val orientation = activity.resources.configuration.orientation
        val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE
        if (Preferences.getEnableDrawerOnPortrait()) {
            setNavigationDrawerLock(false)
            return
        }
        setNavigationDrawerLock(!isLandscape)
    }

    fun sethamburgerMenuIconOnToolbar(activity: AppCompatActivity, toolbar: MaterialToolbar) {
        val orientation = activity.resources.configuration.orientation
        val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE
        if (isLandscape) {
            val drawerToggle = ActionBarDrawerToggle(
                activity,
                drawerLayout,
                toolbar,
                R.string.toolbar_navigation_drawer_open,
                R.string.toolbar_navigation_drawer_closed
            )
            drawerLayout.addDrawerListener(drawerToggle)
            drawerToggle.syncState()
        }
    }

    /*
    Auxiliar functions, could be moved somewhere else
     */
    @OptIn(UnstableApi::class)
    fun setSystemBarsVisibility(activity: AppCompatActivity, visibility: Boolean) {
        val window = activity.window
        val decorView = window.decorView
        val insetsController = WindowInsetsControllerCompat(window, decorView)
        if (visibility) {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            insetsController.show(WindowInsetsCompat.Type.navigationBars())
            insetsController.show(WindowInsetsCompat.Type.statusBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            insetsController.hide(WindowInsetsCompat.Type.navigationBars())
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
