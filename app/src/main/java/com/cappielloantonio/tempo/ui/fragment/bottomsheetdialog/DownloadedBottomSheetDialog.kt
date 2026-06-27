package com.cappielloantonio.tempo.ui.fragment.bottomsheetdialog

import android.os.Bundle
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.components.ActionBottomSheetContent
import com.cappielloantonio.tempo.ui.components.ActionItemUiModel
import com.cappielloantonio.tempo.ui.components.TempusImageType
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.ExternalAudioReader
import com.cappielloantonio.tempo.util.MappingUtil
import com.cappielloantonio.tempo.util.Preferences
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

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    ActionBottomSheetContent(
                        title = title,
                        subtitle = subtitle,
                        coverArtId = songs.firstOrNull()?.coverArtId,
                        imageType = TempusImageType.Song,
                        actions = listOf(
                            ActionItemUiModel(
                                icon = Icons.Default.Shuffle,
                                label = getString(R.string.downloaded_bottom_sheet_shuffle),
                                isVisible = songs.size > 1,
                                onClick = {
                                    MediaManager.startQueue(activity().mediaBrowserListenableFuture, ArrayList(songs.shuffled()), 0)
                                    activity().setBottomSheetInPeek(true)
                                    dismiss()
                                },
                            ),
                            ActionItemUiModel(
                                icon = Icons.Default.PlayArrow,
                                label = getString(R.string.downloaded_bottom_sheet_play_next),
                                onClick = {
                                    MediaManager.enqueue(activity().mediaBrowserListenableFuture, songs, true)
                                    activity().setBottomSheetInPeek(true)
                                    dismiss()
                                },
                            ),
                            ActionItemUiModel(
                                icon = Icons.AutoMirrored.Filled.QueueMusic,
                                label = getString(R.string.downloaded_bottom_sheet_add_to_queue),
                                onClick = {
                                    MediaManager.enqueue(activity().mediaBrowserListenableFuture, songs, false)
                                    activity().setBottomSheetInPeek(true)
                                    dismiss()
                                },
                            ),
                            ActionItemUiModel(
                                icon = Icons.Default.Delete,
                                label = getString(
                                    if (songs.size > 1) {
                                        R.string.downloaded_bottom_sheet_remove_all
                                    } else {
                                        R.string.downloaded_bottom_sheet_remove
                                    }
                                ),
                                isDestructive = true,
                                onClick = {
                                    removeDownloads(songs)
                                    dismiss()
                                },
                            ),
                        ),
                    )
                }
            }
        }
    }

    private fun removeDownloads(songs: List<Child>) {
        if (Preferences.getDownloadDirectoryUri() == null) {
            DownloadUtil.getDownloadTracker(requireContext()).remove(
                MappingUtil.mapDownloads(songs),
                songs.map(::Download),
            )
            return
        }

        songs.forEach(ExternalAudioReader::delete)
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
