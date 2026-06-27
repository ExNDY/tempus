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
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.artist.ArtistBottomSheetRoute
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Constants
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

@UnstableApi
class ArtistBottomSheetDialog : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val artist = arguments?.getSerializable(Constants.ARTIST_OBJECT) as? ArtistID3
        if (artist == null) {
            dismiss()
            return ComposeView(requireContext())
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    ArtistBottomSheetRoute(
                        artist = artist,
                        onDismiss = ::dismiss,
                        onNavigateToArtist = { resolvedArtist ->
                            NavHostFragment.findNavController(this@ArtistBottomSheetDialog).navigate(
                                R.id.artistPageFragment,
                                Bundle().apply { putSerializable(Constants.ARTIST_OBJECT, resolvedArtist) }
                            )
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
                        }
                    )
                }
            }
        }
    }
}
