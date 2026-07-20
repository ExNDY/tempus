package com.cappielloantonio.tempo.navigation

import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.ui.download.DownloadRouteScreen
import com.cappielloantonio.tempo.ui.home.HomeRouteScreen
import com.cappielloantonio.tempo.ui.home.LibraryRouteScreen

object BottomBarItems {
    val home = BottomBarItem(
        screenName = HomeRouteScreen.screenName,
        labelResId = R.string.menu_home_label,
        iconResId = R.drawable.ic_home,
    )

    val library = BottomBarItem(
        screenName = LibraryRouteScreen.screenName,
        labelResId = R.string.menu_library_label,
        iconResId = R.drawable.ic_graphic_eq,
    )

    val download = BottomBarItem(
        screenName = DownloadRouteScreen.screenName,
        labelResId = R.string.menu_download_label,
        iconResId = R.drawable.ic_play_for_work,
    )

    val all = listOf(home, library, download)
}
