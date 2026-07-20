package com.cappielloantonio.tempo.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.cappielloantonio.tempo.ui.auth.LandingRouteScreen
import com.cappielloantonio.tempo.ui.home.HomeRouteScreen
import com.cappielloantonio.tempo.ui.player.LocalPlayerSharedTransitionScope
import com.cappielloantonio.tempo.ui.player.PlayerArtistChooserRouteScreen
import com.cappielloantonio.tempo.ui.player.LocalPlayerProgressController
import com.cappielloantonio.tempo.ui.player.PlayerChromeHost
import com.cappielloantonio.tempo.ui.player.PlayerHeader
import com.cappielloantonio.tempo.ui.player.PlayerRouteScreen
import com.cappielloantonio.tempo.ui.player.PlayerSharedContentKeys

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun RootContainer(
    isBottomBarEnabled: Boolean,
    onBottomBarVisibilityChanged: (Boolean) -> Unit,
    onNavControllerReady: (NavHostController) -> Unit,
) {
    val navController = rememberNavController()

    var bottomMenuConfig: BottomMenuConfig by remember {
        mutableStateOf(BottomMenuConfig.Hidden)
    }

    val visibleBottomMenuConfig = bottomMenuConfig as? BottomMenuConfig.Visible
    val shouldShowBottomBar = isBottomBarEnabled && visibleBottomMenuConfig != null
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val isPlayerArtistChooserRoute = currentRoute == PlayerArtistChooserRouteScreen.screenName
    val isAuthRoute = authScreens.any { it.screenName == currentRoute }

    LaunchedEffect(navController) {
        ScreenNameExtension.allScreens = authScreens + mainScreens
        onNavControllerReady(navController)
    }

    LaunchedEffect(shouldShowBottomBar) {
        onBottomBarVisibilityChanged(shouldShowBottomBar)
    }

    PlayerChromeHost(
        onNavigateToRoute = navController::navigate,
        onChooseArtist = { artists ->
            navController.navigate(PlayerArtistChooserRouteScreen.route(artists))
        },
    ) { playerChromeState, playerChromeActions ->
        val progressController = LocalPlayerProgressController.current ?: return@PlayerChromeHost
        val shouldShowMiniPlayer =
            playerChromeState.hasPlayback &&
                !isAuthRoute &&
                currentRoute != PlayerRouteScreen.screenName &&
                !isPlayerArtistChooserRoute
        val sharedTransitionScope = LocalPlayerSharedTransitionScope.current

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                Column {
                    AnimatedVisibility(visible = shouldShowMiniPlayer) {
                        val miniAnimatedVisibilityScope = this
                        val headerModifier = Modifier.height(56.dp)
                        val backgroundModifier = if (sharedTransitionScope != null) {
                            with(sharedTransitionScope) {
                                Modifier.sharedBounds(
                                    sharedContentState = rememberSharedContentState(PlayerSharedContentKeys.Background),
                                    animatedVisibilityScope = miniAnimatedVisibilityScope,
                                )
                            }
                        } else {
                            Modifier
                        }
                        val coverArtModifier = if (sharedTransitionScope != null) {
                            with(sharedTransitionScope) {
                                Modifier.sharedElement(
                                    sharedContentState = rememberSharedContentState(PlayerSharedContentKeys.CoverArt),
                                    animatedVisibilityScope = miniAnimatedVisibilityScope,
                                )
                            }
                        } else {
                            Modifier
                        }

                        PlayerHeader(
                            currentSong = playerChromeState.uiState.currentSong,
                            description = playerChromeState.uiState.description,
                            isPlaying = playerChromeState.playbackSnapshot.isPlaying,
                            playbackState = playerChromeState.transport.playbackState,
                            progressController = progressController,
                            progressRefreshToken = playerChromeState.progressRefreshToken,
                            isNextEnabled = playerChromeState.transport.isNextEnabled,
                            isSeekControlsEnabled = playerChromeState.transport.isSeekControlsEnabled,
                            onHeaderClick = {
                                navController.navigateSingleTop(PlayerRouteScreen.screenName)
                            },
                            onPlayPauseClick = playerChromeActions.onPlayPauseClick,
                            onNextClick = playerChromeActions.onNextClick,
                            onSeekBackClick = playerChromeActions.onSeekBackClick,
                            onSeekForwardClick = playerChromeActions.onSeekForwardClick,
                            backgroundModifier = backgroundModifier,
                            coverArtModifier = coverArtModifier,
                            modifier = headerModifier,
                        )
                    }

                    if (shouldShowMiniPlayer && !shouldShowBottomBar) {
                        Spacer(
                            modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)
                        )
                    }

                    visibleBottomMenuConfig?.takeIf { isBottomBarEnabled }?.let { bottomBarConfig ->
                        BottomBar(
                            navController = navController,
                            selectedItem = bottomBarConfig.bottomItem,
                        )
                    }
                }
            },
        ) { padding ->
            NavHost(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                    ),
                navController = navController,
                startDestination = Hosts.Auth.route,
            ) {
                navigation(
                    route = Hosts.Auth.route,
                    startDestination = LandingRouteScreen.screenName,
                ) {
                    authScreens.forEach { screen ->
                        composableScreen(
                            screen = screen,
                            navController = navController,
                            updateBottomMenuConfig = { bottomMenuConfig = it },
                        )
                    }
                }

                navigation(
                    route = Hosts.Main.route,
                    startDestination = HomeRouteScreen.screenName,
                ) {
                    mainScreens.forEach { screen ->
                        composableScreen(
                            screen = screen,
                            navController = navController,
                            updateBottomMenuConfig = { bottomMenuConfig = it },
                        )
                    }
                }
            }
        }
    }
}
