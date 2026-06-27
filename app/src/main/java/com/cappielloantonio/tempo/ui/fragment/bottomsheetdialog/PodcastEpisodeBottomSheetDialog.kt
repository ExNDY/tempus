package com.cappielloantonio.tempo.ui.fragment.bottomsheetdialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.BundleCompat
import androidx.fragment.app.viewModels
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.KoinViewModelFactory
import com.cappielloantonio.tempo.subsonic.models.PodcastChannel
import com.cappielloantonio.tempo.subsonic.models.PodcastEpisode
import com.cappielloantonio.tempo.subsonic.models.PodcastStatus
import com.cappielloantonio.tempo.ui.components.ActionBottomSheetContent
import com.cappielloantonio.tempo.ui.components.ActionItemUiModel
import com.cappielloantonio.tempo.ui.components.TempusImageType
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.viewmodel.PodcastEpisodeBottomSheetViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Locale

@UnstableApi
class PodcastEpisodeBottomSheetDialog : BottomSheetDialogFragment() {
    private val viewModel: PodcastEpisodeBottomSheetViewModel by viewModels { KoinViewModelFactory() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val bundle = arguments ?: return ComposeView(requireContext())
        val episode = BundleCompat.getSerializable(
            bundle,
            Constants.PODCAST_OBJECT,
            PodcastEpisode::class.java,
        )
        if (episode == null) {
            dismiss()
            return ComposeView(requireContext())
        }
        viewModel.podcastEpisode = episode
        val isCompleted = episode.status == PodcastStatus.COMPLETED
        val dateFormatter = SimpleDateFormat("MMM d", Locale.getDefault())
        val subtitle = if (episode.publishDate != null && episode.duration != null) {
            getString(
                R.string.podcast_release_date_duration_formatter,
                dateFormatter.format(episode.publishDate),
                MusicUtil.getReadablePodcastDurationString(episode.duration?.toLong() ?: 0L),
            )
        } else {
            episode.artist.orEmpty()
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    ActionBottomSheetContent(
                        title = episode.title.orEmpty(),
                        subtitle = subtitle,
                        coverArtId = episode.coverArtId,
                        imageType = TempusImageType.Podcast,
                        actions = listOf(
                            ActionItemUiModel(
                                icon = Icons.AutoMirrored.Filled.ArrowForward,
                                label = getString(R.string.podcast_bottom_sheet_go_to_channel),
                                onClick = {
                                    val targetChannel = PodcastChannel().apply {
                                        id = episode.channelId
                                        title = episode.album ?: episode.artist ?: episode.title
                                        coverArtId = episode.coverArtId
                                    }
                                    dismiss()
                                    findNavController().navigate(
                                        R.id.podcastChannelPageFragment,
                                        Bundle().apply {
                                            putSerializable(Constants.PODCAST_CHANNEL_OBJECT, targetChannel)
                                        },
                                    )
                                },
                                isVisible = !episode.channelId.isNullOrBlank(),
                            ),
                            ActionItemUiModel(
                                icon = if (isCompleted) Icons.Default.Delete else Icons.Default.Download,
                                label = getString(
                                    if (isCompleted) {
                                        R.string.podcast_bottom_sheet_remove
                                    } else {
                                        R.string.podcast_bottom_sheet_download
                                    }
                                ),
                                onClick = {
                                    if (isCompleted) {
                                        viewModel.deletePodcastEpisode()
                                    } else {
                                        viewModel.requestPodcastEpisodeDownload()
                                    }
                                    parentFragmentManager.setFragmentResult(
                                        Constants.REQUEST_REFRESH_PODCASTS,
                                        Bundle.EMPTY,
                                    )
                                    dismiss()
                                },
                                isDestructive = isCompleted,
                            ),
                        ),
                    )
                }
            }
        }
    }
}
