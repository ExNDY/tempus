package com.cappielloantonio.tempo.ui.fragment.bottomsheetdialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.NavHostFragment
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.dialog.PlaylistChooserDialog
import com.cappielloantonio.tempo.ui.dialog.RatingDialog
import com.cappielloantonio.tempo.ui.song.SongBottomSheetRoute
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.AssetLinkUtil
import com.cappielloantonio.tempo.util.Constants
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

@UnstableApi
class SongBottomSheetDialog : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val song = arguments?.getSerializable(Constants.TRACK_OBJECT) as? Child
        if (song == null) {
            dismiss()
            return ComposeView(requireContext())
        }

        val playlistId = arguments?.getString(Constants.PLAYLIST_ID)
        val itemPosition = arguments?.getInt(Constants.ITEM_POSITION, -1) ?: -1

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    SongBottomSheetRoute(
                        song = song,
                        playlistId = playlistId,
                        itemPosition = itemPosition,
                        onDismiss = ::dismiss,
                        onOpenRatingDialog = { media ->
                            RatingDialog().apply {
                                arguments = Bundle().apply {
                                    putSerializable(Constants.TRACK_OBJECT, media)
                                }
                            }.show(requireActivity().supportFragmentManager, null)
                        },
                        onOpenPlaylistChooser = { songs ->
                            PlaylistChooserDialog().apply {
                                arguments = Bundle().apply {
                                    putSerializable(Constants.TRACKS_OBJECT, ArrayList(songs))
                                }
                            }.show(requireActivity().supportFragmentManager, null)
                        },
                        onNavigateToAlbum = { album ->
                            NavHostFragment.findNavController(this@SongBottomSheetDialog).navigate(
                                R.id.albumPageFragment,
                                Bundle().apply { putSerializable(Constants.ALBUM_OBJECT, album) }
                            )
                        },
                        onNavigateToArtist = { artist ->
                            NavHostFragment.findNavController(this@SongBottomSheetDialog).navigate(
                                R.id.artistPageFragment,
                                Bundle().apply { putSerializable(Constants.ARTIST_OBJECT, artist) }
                            )
                        },
                        onPlayNext = { media ->
                            val activity = requireActivity() as MainActivity
                            MediaManager.enqueue(activity.mediaBrowserListenableFuture, media, true)
                            activity.setBottomSheetInPeek(true)
                        },
                        onAddToQueue = { media ->
                            val activity = requireActivity() as MainActivity
                            MediaManager.enqueue(activity.mediaBrowserListenableFuture, media, false)
                            activity.setBottomSheetInPeek(true)
                        },
                        onStartInstantMix = { media ->
                            val activity = requireActivity() as MainActivity
                            MediaManager.startQueue(activity.mediaBrowserListenableFuture, media, 0)
                            activity.setBottomSheetInPeek(true)
                        },
                        onOpenAssetLink = { assetLink, collapsePlayer ->
                            (requireActivity() as MainActivity).openAssetLink(assetLink, collapsePlayer)
                        },
                        onCopyAssetLink = { assetLink ->
                            AssetLinkUtil.copyToClipboard(requireContext(), assetLink)
                            android.widget.Toast.makeText(
                                requireContext(),
                                getString(R.string.asset_link_copied_toast, assetLink.id),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        onRefreshShares = {
                            parentFragmentManager.setFragmentResult(
                                Constants.REQUEST_REFRESH_HOME_SHARES,
                                Bundle.EMPTY
                            )
                        }
                    )
                }
            }
        }
    }
}
