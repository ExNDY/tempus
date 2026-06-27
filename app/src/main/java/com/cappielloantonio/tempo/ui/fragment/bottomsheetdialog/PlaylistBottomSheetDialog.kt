package com.cappielloantonio.tempo.ui.fragment.bottomsheetdialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.media3.common.util.UnstableApi
import androidx.lifecycle.ViewModelProvider
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.dialog.PlaylistEditorDialog
import com.cappielloantonio.tempo.ui.playlist.PlaylistBottomSheetRoute
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.HomeViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

@UnstableApi
class PlaylistBottomSheetDialog : BottomSheetDialogFragment() {

    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]

        val playlist = arguments?.getSerializable(Constants.PLAYLIST_OBJECT) as? Playlist
        if (playlist == null) {
            dismiss()
            return ComposeView(requireContext())
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    PlaylistBottomSheetRoute(
                        playlist = playlist,
                        onDismiss = ::dismiss,
                        onPlayNext = { songs ->
                            val activity = requireActivity() as MainActivity
                            MediaManager.enqueue(activity.mediaBrowserListenableFuture, songs, true)
                            activity.setBottomSheetInPeek(true)
                        },
                        onAddToQueue = { songs ->
                            val activity = requireActivity() as MainActivity
                            MediaManager.enqueue(activity.mediaBrowserListenableFuture, songs, false)
                            activity.setBottomSheetInPeek(true)
                        },
                        onShufflePlay = { songs ->
                            val activity = requireActivity() as MainActivity
                            MediaManager.startQueue(activity.mediaBrowserListenableFuture, songs, 0)
                            activity.setBottomSheetInPeek(true)
                        },
                        onOpenEditor = { targetPlaylist ->
                            PlaylistEditorDialog(null).apply {
                                arguments = Bundle().apply {
                                    putSerializable(Constants.PLAYLIST_OBJECT, targetPlaylist)
                                }
                            }.show(requireActivity().supportFragmentManager, null)
                        },
                        onDeletePlaylist = { targetPlaylist ->
                            PlaylistEditorDialog(null).apply {
                                arguments = Bundle().apply {
                                    putSerializable(Constants.PLAYLIST_OBJECT, targetPlaylist)
                                }
                            }.show(requireActivity().supportFragmentManager, null)
                        },
                        onRefreshAfterMutation = { homeViewModel.refreshShares() }
                    )
                }
            }
        }
    }
}
