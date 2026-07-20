package com.cappielloantonio.tempo.ui.folder
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getDirectoryViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.navigation.BottomMenuConfig
import com.cappielloantonio.tempo.navigation.DefaultScreenNameExtension.defaultScreenName
import com.cappielloantonio.tempo.navigation.DefaultScreenNameExtension.defaultScreenNameWithOptionalParams
import com.cappielloantonio.tempo.navigation.Screen
import com.cappielloantonio.tempo.navigation.ScreenNameExtension.screenNameWithOptionalParams
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.util.requireActivity
import com.cappielloantonio.tempo.ui.song.SongBottomSheetRouteScreen
import kotlinx.coroutines.launch
object DirectoryRouteScreen : Screen.DefaultScreen {
    private const val DIRECTORY_ID = "directoryId"
    private const val DIRECTORY_NAME = "directoryName"
    private const val BREADCRUMB = "breadcrumb"
    override val navArgs = listOf(
        navArgument(DIRECTORY_ID) {
            type = NavType.StringType
            nullable = false
        },
        navArgument(DIRECTORY_NAME) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(BREADCRUMB) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
    )
    override val screenName: String = defaultScreenNameWithOptionalParams(
        params = listOf(DIRECTORY_ID),
        optionalParams = listOf(DIRECTORY_NAME, BREADCRUMB),
    )
    override fun bottomMenuConfig(): BottomMenuConfig = BottomMenuConfig.Hidden
    fun route(
        directoryId: String,
        directoryName: String? = null,
        breadcrumb: String? = null,
    ): String {
        return screenNameWithOptionalParams(
            params = listOf(Uri.encode(directoryId)),
            optionalParams = listOf(
                DIRECTORY_NAME to directoryName?.takeIf { it.isNotBlank() }?.let(Uri::encode),
                BREADCRUMB to breadcrumb?.takeIf { it.isNotBlank() }?.let(Uri::encode),
            ),
        )
    }
    @Composable
    override fun Content(navController: NavController, args: Bundle?) {
        val activity = LocalContext.current.requireActivity<MainActivity>()
        val routeArgs = requireNotNull(args) { "Missing arguments for ${defaultScreenName()}" }
        val directoryId = routeArgs.getString(DIRECTORY_ID)
            ?.takeIf { it.isNotBlank() }
            ?: error("Missing directoryId for ${defaultScreenName()}")
        val directoryName = routeArgs.getString(DIRECTORY_NAME)?.takeIf { it.isNotBlank() }
        val breadcrumb = routeArgs.getString(BREADCRUMB)?.takeIf { it.isNotBlank() }
        val viewModel = getViewModel { getDirectoryViewModel().apply { onStart(directoryId) } }
        val uiState by viewModel.uiState.collectAsState()
        val coroutineScope = rememberCoroutineScope()
        val currentTitle = directoryName ?: uiState.directory?.name ?: stringResource(R.string.settings_music_directory)
        val currentBreadcrumb = breadcrumb ?: currentTitle
        val playableChildren = uiState.children.filter { !it.isDir && !it.isVideo }
        fun playDirectory(childDirectoryId: String) {
            Toast.makeText(
                activity,
                activity.getString(R.string.folder_play_collecting),
                Toast.LENGTH_SHORT,
            ).show()
            coroutineScope.launch {
                val collectedSongs = viewModel.collectDirectorySongs(childDirectoryId)
                if (collectedSongs.isEmpty()) {
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.folder_play_no_songs),
                        Toast.LENGTH_SHORT,
                    ).show()
                    return@launch
                }
                MediaManager.startQueue(
                    activity.mediaBrowserListenableFuture,
                    ArrayList(collectedSongs),
                    0,
                )
                activity.setBottomSheetInPeek(true)
                Toast.makeText(
                    activity,
                    activity.getString(R.string.folder_play_playing, collectedSongs.size),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
        DirectoryScreen(
            title = currentTitle,
            breadcrumb = currentBreadcrumb,
            children = uiState.children,
            isLoading = uiState.isLoading,
            onNavigateBack = { navController.navigateUp() },
            onItemClick = { child ->
                if (child.isDir) {
                    child.id.takeIf { it.isNotBlank() }?.let { childDirectoryId ->
                        navController.navigate(
                            route(
                                directoryId = childDirectoryId,
                                directoryName = child.title,
                                breadcrumb = listOf(currentBreadcrumb, child.title.orEmpty())
                                    .filter { it.isNotBlank() }
                                    .joinToString(" / "),
                            ),
                        )
                    }
                } else {
                    MediaManager.startQueue(
                        activity.mediaBrowserListenableFuture,
                        ArrayList(playableChildren),
                        playableChildren.indexOfFirst { it.id == child.id }.coerceAtLeast(0),
                    )
                    activity.setBottomSheetInPeek(true)
                }
            },
            onItemLongClick = { child ->
                if (!child.isDir) {
                    navController.navigate(SongBottomSheetRouteScreen.route(child))
                }
            },
            onItemPlayClick = { child ->
                if (child.isDir) {
                    child.id.takeIf { it.isNotBlank() }?.let(::playDirectory)
                }
            },
        )
    }
}
