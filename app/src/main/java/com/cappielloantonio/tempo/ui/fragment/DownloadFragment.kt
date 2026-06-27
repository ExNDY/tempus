package com.cappielloantonio.tempo.ui.fragment

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getDownloadViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.model.DownloadStack
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.download.DownloadScreen
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.ExternalAudioReader
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.viewmodel.DownloadViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

@UnstableApi
class DownloadFragment : Fragment() {

    private val viewModel: DownloadViewModel by viewModel()
    private val directoryPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) return@registerForActivityResult

            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                requireContext().contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: SecurityException) {
            }

            Preferences.setDownloadDirectoryUri(uri.toString())
            ExternalAudioReader.refreshCache()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        observeRefreshResults()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    val routeViewModel = getViewModel { getDownloadViewModel().apply { onStart() } }
                    val uiState by routeViewModel.uiState.collectAsState()

                    DownloadScreen(
                        uiState = uiState,
                        onNavigateBack = {
                            if (routeViewModel.canPopViewStack()) {
                                routeViewModel.popViewStack()
                            } else {
                                findNavController().navigateUp()
                            }
                        },
                        onSearchClick = { findNavController().navigate(R.id.searchFragment) },
                        onGroupTypeSelected = { routeViewModel.setRootView(it) },
                        onRefreshClick = { routeViewModel.refreshExternalDownloads() },
                        onSetDirectoryClick = { directoryPickerLauncher.launch(null) },
                        onShuffleClick = { songs ->
                            if (songs.isNotEmpty()) {
                                MediaManager.startQueue(activity().mediaBrowserListenableFuture, ArrayList(songs), 0)
                                activity().setBottomSheetInPeek(true)
                            }
                        },
                        onTrackClick = { songs, index ->
                            if (songs.isNotEmpty()) {
                                MediaManager.startQueue(activity().mediaBrowserListenableFuture, ArrayList(songs), index)
                                activity().setBottomSheetInPeek(true)
                            }
                        },
                        onTrackLongClick = { song, index ->
                            findNavController().navigate(
                                R.id.songBottomSheetDialog,
                                Bundle().apply {
                                    putSerializable(Constants.TRACK_OBJECT, song)
                                    putInt(Constants.ITEM_POSITION, index)
                                },
                            )
                        },
                        onGroupClick = { groupType, groupValue ->
                            routeViewModel.pushViewStack(
                                DownloadStack(
                                    id = groupType,
                                    view = groupValue,
                                )
                            )
                        },
                        onGroupLongClick = { songs, title, subtitle ->
                            findNavController().navigate(
                                R.id.downloadedBottomSheetDialog,
                                Bundle().apply {
                                    putSerializable(Constants.DOWNLOAD_GROUP, ArrayList(songs))
                                    putString(Constants.DOWNLOAD_GROUP_TITLE, title)
                                    putString(Constants.DOWNLOAD_GROUP_SUBTITLE, subtitle)
                                },
                            )
                        },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        activity().toggleBottomNavigationBarVisibilityOnOrientationChange()
        activity().setBottomSheetVisibility(true)
    }

    private fun observeRefreshResults() {
        viewLifecycleOwnerLiveData.observe(this) { owner ->
            owner ?: return@observe

            owner.lifecycleScope.launch {
                owner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                    viewModel.refreshResults.collect { count ->
                        when {
                            count == -1 -> {
                                android.widget.Toast.makeText(
                                    requireContext(),
                                    getString(R.string.download_refresh_no_directory),
                                    android.widget.Toast.LENGTH_SHORT,
                                ).show()
                            }

                            count == 0 -> {
                                android.widget.Toast.makeText(
                                    requireContext(),
                                    getString(R.string.download_refresh_no_changes),
                                    android.widget.Toast.LENGTH_SHORT,
                                ).show()
                            }

                            else -> {
                                android.widget.Toast.makeText(
                                    requireContext(),
                                    resources.getQuantityString(R.plurals.download_refresh_removed, count, count),
                                    android.widget.Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun activity(): MainActivity = requireActivity() as MainActivity
}
