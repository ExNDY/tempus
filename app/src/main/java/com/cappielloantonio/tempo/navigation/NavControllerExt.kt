package com.cappielloantonio.tempo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

/**
 * Replace the entire navigation stack with another screen, regardless
 * of the contents of the current backstack.
 *
 * This means after such a transition, if the user presses back, the app will close.
 * The history is reset by this transition.
 */
fun NavController.replace(route: String) {
    navigate(route) {
        // Clear the stack up to the root graph. This is more reliable than magic number 0.
        popUpTo(graph.id) { inclusive = true }

        // Avoid creating screen duplicates if navigation is accidentally called twice
        launchSingleTop = true
    }
}

fun NavController.navigateSingleTop(
    screenName: String,
    saveState: Boolean = true,
) {
    navigate(screenName) {
        // Pop up to the start destination of the graph to
        // avoid building up a large stack of destinations
        // on the back stack as users select items
        popUpTo(graph.findStartDestination().id) {
            this.saveState = saveState
        }
        // Avoid multiple copies of the same destination when
        // reselecting the same item
        launchSingleTop = true
        // Restore state when reselecting a previously selected item
        restoreState = true
    }
}

@Suppress("ComposableNaming")
@Composable
fun <T> NavController.observeForResult(key: String, onResult: (T) -> Unit) {
    val savedStateHandle = currentBackStackEntry?.savedStateHandle ?: return

    LaunchedEffect(key, savedStateHandle) {
        // Subscribe for changes of the specified key
        savedStateHandle.getStateFlow<T?>(key, null).collect { result ->
            if (result != null) {
                onResult(result)
                // Remove result to avoid repeating its handling during recomposition
                savedStateHandle.remove<T>(key)
            }
        }
    }
}

fun <T> NavController.saveResultForPreviousScreen(key: String, value: T) {
    this.previousBackStackEntry
        ?.savedStateHandle
        ?.set(key, value)
}

fun <T> NavController.saveResultToCurrentScreen(key: String, value: T) {
    this.currentBackStackEntry
        ?.savedStateHandle
        ?.set(key, value)
}
