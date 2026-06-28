package com.cappielloantonio.tempo.navigation

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior

class NavigationController(private val helper: NavigationHelper) {
    fun syncWithBottomSheetBehavior(
        bottomSheetBehavior: BottomSheetBehavior<View>,
        navController: NavController
    ) {
        helper.syncWithBottomSheetBehavior(bottomSheetBehavior, navController)
    }

    fun setNavbarVisibility(visibility: Boolean) {
        helper.setBottomNavigationBarVisibility(visibility)
    }

    fun setDrawerLock(visibility: Boolean) {
        helper.setNavigationDrawerLock(visibility)
    }

    fun isNavigationDrawerLocked(): Boolean {
        return helper.isNavigationDrawerLocked()
    }

    fun toggleDrawerLockOnOrientation(activity: AppCompatActivity) {
        helper.toggleNavigationDrawerLockOnOrientationChange(activity)
    }

    fun setSystemBarsVisibility(activity: AppCompatActivity, visibility: Boolean) {
        helper.setSystemBarsVisibility(activity, visibility)
    }

    fun setHamburgerMenuForLandscape(activity: AppCompatActivity, toolbar: MaterialToolbar) {
        helper.sethamburgerMenuIconOnToolbar(activity, toolbar)
    }

    val navController: NavController
        get() = helper.navController
}
