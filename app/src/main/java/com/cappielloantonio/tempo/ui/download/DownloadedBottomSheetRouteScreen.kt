package com.cappielloantonio.tempo.ui.download

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.cappielloantonio.tempo.navigation.Screen
import com.cappielloantonio.tempo.navigation.encodeNavRouteValue
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.requireActivity
import kotlinx.coroutines.launch

object DownloadedBottomSheetRouteScreen : Screen.BottomSheetScreen {
    private const val GROUP_TYPE = "groupType"
    private const val GROUP_VALUE = "groupValue"

    override val screenName: String =
        "downloadedActions?$GROUP_TYPE={$GROUP_TYPE}&$GROUP_VALUE={$GROUP_VALUE}"

    override val navArgs = listOf(
        navArgument(GROUP_TYPE) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(GROUP_VALUE) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
    )

    fun route(
        groupType: String,
        groupValue: String,
    ): String = buildString {
        append("downloadedActions?")
        append(GROUP_TYPE).append('=').append(encodeNavRouteValue(groupType))
        append('&').append(GROUP_VALUE).append('=').append(encodeNavRouteValue(groupValue))
    }

    @Composable
    override fun Content(navController: NavController, args: Bundle?, onClose: suspend () -> Unit) {
        val groupType = args?.getString(GROUP_TYPE).orEmpty()
        val groupValue = args?.getString(GROUP_VALUE).orEmpty()
        val activity = LocalContext.current.requireActivity<MainActivity>()
        val scope = rememberCoroutineScope()

        TempusTheme {
            DownloadedBottomSheetRoute(
                groupType = groupType,
                groupValue = groupValue,
                onDismiss = { scope.launch { onClose() } },
                onShufflePlay = { groupSongs ->
                    MediaManager.startQueue(activity.mediaBrowserListenableFuture, ArrayList(groupSongs), 0)
                    activity.setBottomSheetInPeek(true)
                },
                onPlayNext = { groupSongs ->
                    MediaManager.enqueue(activity.mediaBrowserListenableFuture, groupSongs, true)
                    activity.setBottomSheetInPeek(true)
                },
                onAddToQueue = { groupSongs ->
                    MediaManager.enqueue(activity.mediaBrowserListenableFuture, groupSongs, false)
                    activity.setBottomSheetInPeek(true)
                },
            )
        }
    }
}
