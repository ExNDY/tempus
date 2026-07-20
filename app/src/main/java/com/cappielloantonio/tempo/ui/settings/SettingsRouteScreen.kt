package com.cappielloantonio.tempo.ui.settings

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.cappielloantonio.tempo.navigation.BottomMenuConfig
import com.cappielloantonio.tempo.navigation.DefaultScreenNameExtension.defaultScreenName
import com.cappielloantonio.tempo.navigation.Screen
object SettingsRouteScreen : Screen.DefaultScreen {

    override val screenName: String = defaultScreenName()

    override fun bottomMenuConfig(): BottomMenuConfig = BottomMenuConfig.Hidden

    @Composable
    override fun Content(navController: NavController, args: Bundle?) {
        SettingsRouteContent(navController = navController)
    }
}
