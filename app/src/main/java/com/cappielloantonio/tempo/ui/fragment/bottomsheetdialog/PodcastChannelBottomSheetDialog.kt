package com.cappielloantonio.tempo.ui.fragment.bottomsheetdialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.BundleCompat
import androidx.fragment.app.viewModels
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.KoinViewModelFactory
import com.cappielloantonio.tempo.subsonic.models.PodcastChannel
import com.cappielloantonio.tempo.ui.components.ActionBottomSheetContent
import com.cappielloantonio.tempo.ui.components.ActionItemUiModel
import com.cappielloantonio.tempo.ui.components.TempusImageType
import com.cappielloantonio.tempo.ui.state.UiEvent
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.viewmodel.PodcastChannelBottomSheetViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete

@UnstableApi
class PodcastChannelBottomSheetDialog : BottomSheetDialogFragment() {
    private val viewModel: PodcastChannelBottomSheetViewModel by viewModels { KoinViewModelFactory() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val bundle = arguments ?: return ComposeView(requireContext())
        val channel = BundleCompat.getSerializable(
            bundle,
            Constants.PODCAST_CHANNEL_OBJECT,
            PodcastChannel::class.java,
        )
        if (channel == null) {
            dismiss()
            return ComposeView(requireContext())
        }
        viewModel.podcastChannel = channel

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    val event by viewModel.events.observeAsState()

                    LaunchedEffect(event) {
                        when (val currentEvent = event) {
                            is UiEvent.ShowMessage -> {
                                Toast.makeText(
                                    requireContext(),
                                    currentEvent.message.resolve(requireContext()),
                                    Toast.LENGTH_LONG,
                                ).show()
                            }

                            UiEvent.CloseDialog -> {
                                parentFragmentManager.setFragmentResult(
                                    Constants.REQUEST_REFRESH_PODCASTS,
                                    Bundle.EMPTY,
                                )
                                dismiss()
                            }

                            null -> Unit
                        }
                    }

                    ActionBottomSheetContent(
                        title = channel.title.orEmpty(),
                        subtitle = MusicUtil.getReadableString(channel.description),
                        coverArtId = channel.coverArtId,
                        imageType = TempusImageType.Podcast,
                        actions = listOf(
                            ActionItemUiModel(
                                icon = Icons.Default.Delete,
                                label = getString(R.string.podcast_bottom_sheet_delete),
                                onClick = { viewModel.deletePodcastChannel() },
                                isDestructive = true,
                            )
                        ),
                    )
                }
            }
        }
    }
}
