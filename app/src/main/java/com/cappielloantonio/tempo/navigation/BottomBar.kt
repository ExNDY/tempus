package com.cappielloantonio.tempo.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomBar(
    navController: NavHostController,
    selectedItem: BottomBarItem,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationBar {
        BottomBarItems.all.forEach { item ->
            NavigationBarItem(
                selected = currentDestination
                    ?.hierarchy
                    ?.any { destination -> destination.route == item.screenName }
                    ?: (selectedItem.screenName == item.screenName),
                onClick = {
                    navController.navigateSingleTop(item.screenName)
                },
                icon = {
                    Icon(
                        painter = painterResource(id = item.iconResId),
                        contentDescription = null,
                    )
                },
                label = {
                    Text(text = stringResource(id = item.labelResId))
                },
            )
        }
    }
}
