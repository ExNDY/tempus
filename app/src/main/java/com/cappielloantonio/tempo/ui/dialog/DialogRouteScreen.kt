package com.cappielloantonio.tempo.ui.dialog

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.cappielloantonio.tempo.navigation.Screen
import com.cappielloantonio.tempo.navigation.encodeNavRouteValue

object DialogRouteScreen : Screen.DialogScreen {
    private const val TOKEN = "token"

    override val screenName: String = "dialog/{$TOKEN}"

    override val navArgs = listOf(
        navArgument(TOKEN) {
            type = NavType.StringType
            nullable = false
        },
    )

    fun route(content: DialogRouteContent): String {
        val token = DialogRouteArgsStore.putDialog(content)
        return "dialog/${encodeNavRouteValue(token)}"
    }

    @Composable
    override fun Content(navController: NavController, args: Bundle?, onClose: () -> Unit) {
        val token = args?.getString(TOKEN)
        val content = remember(token) { DialogRouteArgsStore.getDialog(token) }
        DisposableEffect(token) {
            onDispose {
                DialogRouteArgsStore.removeDialog(token)
            }
        }
        content?.invoke(navController, onClose)
    }
}

object BottomSheetRouteScreen : Screen.BottomSheetScreen {
    private const val TOKEN = "token"

    override val screenName: String = "bottomSheet/{$TOKEN}"

    override val navArgs = listOf(
        navArgument(TOKEN) {
            type = NavType.StringType
            nullable = false
        },
    )

    fun route(content: BottomSheetRouteContent): String {
        val token = DialogRouteArgsStore.putBottomSheet(content)
        return "bottomSheet/${encodeNavRouteValue(token)}"
    }

    @Composable
    override fun Content(navController: NavController, args: Bundle?, onClose: suspend () -> Unit) {
        val token = args?.getString(TOKEN)
        val content = remember(token) { DialogRouteArgsStore.getBottomSheet(token) }
        DisposableEffect(token) {
            onDispose {
                DialogRouteArgsStore.removeBottomSheet(token)
            }
        }
        content?.invoke(navController, onClose)
    }
}
