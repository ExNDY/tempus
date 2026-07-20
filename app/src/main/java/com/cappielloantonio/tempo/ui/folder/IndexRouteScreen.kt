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
import com.cappielloantonio.tempo.di.getIndexViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.navigation.BottomMenuConfig
import com.cappielloantonio.tempo.navigation.DefaultScreenNameExtension.defaultScreenName
import com.cappielloantonio.tempo.navigation.DefaultScreenNameExtension.defaultScreenNameWithOptionalParams
import com.cappielloantonio.tempo.navigation.Screen
import com.cappielloantonio.tempo.navigation.ScreenNameExtension.screenNameWithOptionalParams
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.util.requireActivity
import com.cappielloantonio.tempo.ui.song.SongBottomSheetRouteScreen
import kotlinx.coroutines.launch
object IndexRouteScreen : Screen.DefaultScreen {
    private const val FOLDER_ID = "folderId"
    private const val FOLDER_NAME = "folderName"
    override val navArgs = listOf(
        navArgument(FOLDER_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(FOLDER_NAME) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
    )
    override val screenName: String = defaultScreenNameWithOptionalParams(FOLDER_ID, FOLDER_NAME)
    override fun bottomMenuConfig(): BottomMenuConfig = BottomMenuConfig.Hidden
    fun route(
        folderId: String? = null,
        folderName: String? = null,
    ): String {
        return screenNameWithOptionalParams(
            buildList {
                folderId?.takeIf { it.isNotBlank() }?.let { add(FOLDER_ID to Uri.encode(it)) }
                folderName?.takeIf { it.isNotBlank() }?.let { add(FOLDER_NAME to Uri.encode(it)) }
            },
        )
    }
    @Composable
    override fun Content(navController: NavController, args: Bundle?) {
        val activity = LocalContext.current.requireActivity<MainActivity>()
        val folderId = args?.getString(FOLDER_ID)?.takeIf { it.isNotBlank() }
        val folderName = args?.getString(FOLDER_NAME)?.takeIf { it.isNotBlank() }
        val viewModel = getViewModel { getIndexViewModel().apply { onStart(folderId) } }
        val uiState by viewModel.uiState.collectAsState()
        val coroutineScope = rememberCoroutineScope()
        val playableChildren = uiState.children.filter { !it.isDir && !it.isVideo }
        fun playDirectory(directoryId: String) {
            Toast.makeText(
                activity,
                activity.getString(R.string.folder_play_collecting),
                Toast.LENGTH_SHORT,
            ).show()
            coroutineScope.launch {
                val collectedSongs = viewModel.collectDirectorySongs(directoryId)
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
        fun openChild(child: Child) {
            if (child.isDir) {
                child.id.takeIf { it.isNotBlank() }?.let { childDirectoryId ->
                    navController.navigate(
                        DirectoryRouteScreen.route(
                            directoryId = childDirectoryId,
                            directoryName = child.title,
                            breadcrumb = listOf(folderName, child.title)
                                .mapNotNull { it?.takeIf(String::isNotBlank) }
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
        }
        IndexScreen(
            uiState = uiState,
            title = folderName ?: stringResource(R.string.nav_drawer_index),
            onNavigateBack = { navController.navigateUp() },
            onArtistClick = { artist ->
                artist.id?.takeIf { it.isNotBlank() }?.let { directoryId ->
                    navController.navigate(
                        DirectoryRouteScreen.route(
                            directoryId = directoryId,
                            directoryName = artist.name,
                            breadcrumb = artist.name,
                        ),
                    )
                }
            },
            onArtistPlayClick = { artist ->
                val directoryId = artist.id
                if (!directoryId.isNullOrEmpty()) {
                    playDirectory(directoryId)
                }
            },
            onChildClick = { child -> openChild(child) },
            onChildLongClick = { child ->
                if (!child.isDir) {
                    navController.navigate(SongBottomSheetRouteScreen.route(child))
                }
            },
            onChildPlayClick = { child ->
                if (child.isDir) {
                    child.id.takeIf { it.isNotBlank() }?.let(::playDirectory)
                }
            },
        )
    }
}
