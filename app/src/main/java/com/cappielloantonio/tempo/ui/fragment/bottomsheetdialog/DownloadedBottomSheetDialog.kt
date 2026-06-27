package com.cappielloantonio.tempo.ui.fragment.bottomsheetdialog

import android.os.Bundle
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.download.DownloadedBottomSheetRoute
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.DownloadedBottomSheetArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

@UnstableApi
class DownloadedBottomSheetDialog : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val songs = getSongsArg()
        val title = arguments?.getString(Constants.DOWNLOAD_GROUP_TITLE).orEmpty()
        val subtitle = arguments?.getString(Constants.DOWNLOAD_GROUP_SUBTITLE).orEmpty()

        if (songs.isNullOrEmpty()) {
            dismiss()
            return ComposeView(requireContext())
        }

        val bottomSheetArgs = DownloadedBottomSheetArgs(
            songs = songs,
            title = title,
            subtitle = subtitle,
        )

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    DownloadedBottomSheetRoute(
                        args = bottomSheetArgs,
                        onDismiss = ::dismiss,
                        onShufflePlay = { groupSongs ->
                            MediaManager.startQueue(activity().mediaBrowserListenableFuture, ArrayList(groupSongs), 0)
                            activity().setBottomSheetInPeek(true)
                        },
                        onPlayNext = { groupSongs ->
                            MediaManager.enqueue(activity().mediaBrowserListenableFuture, groupSongs, true)
                            activity().setBottomSheetInPeek(true)
                        },
                        onAddToQueue = { groupSongs ->
                            MediaManager.enqueue(activity().mediaBrowserListenableFuture, groupSongs, false)
                            activity().setBottomSheetInPeek(true)
                        },
                    )
                }
            }
        }
    }

    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    private fun getSongsArg(): ArrayList<Child>? {
        val args = arguments ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            args.getSerializable(Constants.DOWNLOAD_GROUP, ArrayList::class.java) as? ArrayList<Child>
        } else {
            args.getSerializable(Constants.DOWNLOAD_GROUP) as? ArrayList<Child>
        }
    }

    private fun activity(): MainActivity = requireActivity() as MainActivity
}
