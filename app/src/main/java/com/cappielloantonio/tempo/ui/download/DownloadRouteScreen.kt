package com.cappielloantonio.tempo.ui.download
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getDownloadViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.model.DownloadStack
import com.cappielloantonio.tempo.navigation.BottomBarItems
import com.cappielloantonio.tempo.navigation.BottomMenuConfig
import com.cappielloantonio.tempo.navigation.DefaultScreenNameExtension.defaultScreenName
import com.cappielloantonio.tempo.navigation.Screen
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.util.requireActivity
import com.cappielloantonio.tempo.ui.song.SongBottomSheetRouteScreen
import com.cappielloantonio.tempo.ui.search.SearchRouteScreen
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.ExternalAudioReader
import com.cappielloantonio.tempo.util.Preferences
object DownloadRouteScreen : Screen.DefaultScreen {
    override val screenName: String = defaultScreenName()
    override fun bottomMenuConfig(): BottomMenuConfig {
        return BottomMenuConfig.Visible(BottomBarItems.download)
    }
    @Composable
    override fun Content(navController: NavController, args: Bundle?) {
        val activity = LocalContext.current.requireActivity<MainActivity>()
        val context = LocalContext.current
        val viewModel = getViewModel { getDownloadViewModel().apply { onStart() } }
        val uiState by viewModel.uiState.collectAsState()
        val directoryPickerLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                try {
                    context.contentResolver.takePersistableUriPermission(uri, flags)
                } catch (_: SecurityException) {
                }
                Preferences.setDownloadDirectoryUri(uri.toString())
                ExternalAudioReader.refreshCache()
            }
        LaunchedEffect(viewModel) {
            viewModel.refreshResults.collect { count ->
                when {
                    count == -1 -> {
                        Toast.makeText(
                            context,
                            context.getString(R.string.download_refresh_no_directory),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    count == 0 -> {
                        Toast.makeText(
                            context,
                            context.getString(R.string.download_refresh_no_changes),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    else -> {
                        Toast.makeText(
                            context,
                            context.resources.getQuantityString(
                                R.plurals.download_refresh_removed,
                                count,
                                count,
                            ),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
        }
        DownloadScreen(
            uiState = uiState,
            onSearchClick = {
                navController.navigate(SearchRouteScreen.screenName)
            },
            onViewBack = {
                if (viewModel.canPopViewStack()) {
                    viewModel.popViewStack()
                }
            },
            onGroupTypeSelected = { viewModel.setRootView(it) },
            onRefreshClick = { viewModel.refreshExternalDownloads() },
            onSetDirectoryClick = { directoryPickerLauncher.launch(null) },
            onShuffleClick = { songs ->
                if (songs.isNotEmpty()) {
                    MediaManager.startQueue(activity.mediaBrowserListenableFuture, ArrayList(songs), 0)
                    activity.setBottomSheetInPeek(true)
                }
            },
            onTrackClick = { songs, index ->
                if (songs.isNotEmpty()) {
                    MediaManager.startQueue(activity.mediaBrowserListenableFuture, ArrayList(songs), index)
                    activity.setBottomSheetInPeek(true)
                }
            },
            onTrackLongClick = { song, index ->
                navController.navigate(SongBottomSheetRouteScreen.route(song, itemPosition = index))
            },
            onGroupClick = { groupType, groupValue ->
                viewModel.pushViewStack(
                    DownloadStack(
                        id = groupType,
                        view = groupValue,
                    ),
                )
            },
            onGroupLongClick = { groupType, groupValue ->
                navController.navigate(
                    DownloadedBottomSheetRouteScreen.route(
                        groupType = groupType,
                        groupValue = groupValue,
                    ),
                )
            },
        )
    }
}
