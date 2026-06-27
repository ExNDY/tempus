package com.cappielloantonio.tempo.ui.fragment.bottomsheetdialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.NavHostFragment
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.album.AlbumBottomSheetRoute
import com.cappielloantonio.tempo.ui.dialog.PlaylistChooserDialog
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.HomeViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

@UnstableApi
class AlbumBottomSheetDialog : BottomSheetDialogFragment() {

    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]

        val album = arguments?.getSerializable(Constants.ALBUM_OBJECT) as? AlbumID3
        if (album == null) {
            dismiss()
            return ComposeView(requireContext())
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    AlbumBottomSheetRoute(
                        album = album,
                        onDismiss = ::dismiss,
                        onOpenPlaylistChooser = { songs ->
                            PlaylistChooserDialog().apply {
                                arguments = Bundle().apply {
                                    putSerializable(Constants.TRACKS_OBJECT, ArrayList(songs))
                                }
                            }.show(requireActivity().supportFragmentManager, null)
                        },
                        onNavigateToArtist = { artist ->
                            NavHostFragment.findNavController(this@AlbumBottomSheetDialog).navigate(
                                R.id.artistPageFragment,
                                Bundle().apply { putSerializable(Constants.ARTIST_OBJECT, artist) }
                            )
                        },
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
                        onStartInstantMix = { songs ->
                            val activity = requireActivity() as MainActivity
                            MediaManager.startQueue(activity.mediaBrowserListenableFuture, songs, 0)
                            activity.setBottomSheetInPeek(true)
                        },
                        onRefreshShares = { homeViewModel.refreshShares() }
                    )
                }
            }
        }
    }
}
