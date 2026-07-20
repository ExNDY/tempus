package com.cappielloantonio.tempo.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import com.cappielloantonio.tempo.ui.player.LocalPlayerAnimatedVisibilityScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.cappielloantonio.tempo.navigation.Screen.BottomSheetScreen
import com.cappielloantonio.tempo.navigation.Screen.DefaultScreen
import com.cappielloantonio.tempo.navigation.Screen.DialogScreen

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.composableScreen(
    screen: Screen,
    navController: NavHostController,
    updateBottomMenuConfig: (BottomMenuConfig) -> Unit,
) {
    when (screen) {
        is BottomSheetScreen -> {
            dialog(
                route = screen.screenName,
                arguments = screen.navArgs
            ) { backStackEntry ->
                val modalBottomSheetState: SheetState = rememberModalBottomSheetState(
                    skipPartiallyExpanded = true
                )
                val scope: CoroutineScope = rememberCoroutineScope()
                var isClosing by remember { mutableStateOf(false) }

                suspend fun closeSheet() {
                    if (isClosing) return
                    isClosing = true
                    modalBottomSheetState.hide()
                    navController.popBackStack()
                }

                BackHandler(enabled = modalBottomSheetState.isVisible && !isClosing) {
                    scope.launch { closeSheet() }
                }

                ModalBottomSheet(
                    modifier = Modifier.padding(top = 46.dp),
                    dragHandle = null,
                    sheetState = modalBottomSheetState,
                    contentWindowInsets = { WindowInsets(0) },
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    onDismissRequest = { scope.launch { closeSheet() } }
                ) {
                    screen.Content(
                        navController = navController,
                        args = backStackEntry.arguments,
                        onClose = ::closeSheet
                    )
                }
            }
        }

        is DefaultScreen -> {
            composable(
                route = screen.screenName,
                arguments = screen.navArgs
            ) { backStackEntry ->
                updateBottomMenuConfig(screen.bottomMenuConfig())

                androidx.compose.runtime.CompositionLocalProvider(
                    LocalPlayerAnimatedVisibilityScope provides this,
                ) {
                    screen.Content(
                        navController = navController,
                        args = backStackEntry.arguments
                    )
                }
            }
        }

        is DialogScreen -> {
            dialog(
                route = screen.screenName,
                arguments = screen.navArgs,
            ) { backStackEntry ->
                screen.Content(
                    navController = navController,
                    args = backStackEntry.arguments,
                    onClose = { navController.popBackStack() },
                )
            }
        }
    }
}
