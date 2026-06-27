package com.cappielloantonio.tempo.ui.fragment

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.fragment.findNavController
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.PodcastChannel
import com.cappielloantonio.tempo.subsonic.models.PodcastEpisode
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.PodcastChannelPageViewModel
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class PodcastChannelPageFragment : Fragment() {
    private lateinit var activity: MainActivity
    private lateinit var viewModel: PodcastChannelPageViewModel
    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        activity = requireActivity() as MainActivity
        viewModel = ViewModelProvider(requireActivity())[PodcastChannelPageViewModel::class.java]
        val channel = arguments?.getSerializable(Constants.PODCAST_CHANNEL_OBJECT) as? PodcastChannel ?: return ComposeView(requireContext())
        viewModel.setPodcastChannel(channel)
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    val channelState by viewModel.getPodcastChannel().observeAsState(channel)
                    val episodes by viewModel.getPodcastChannelEpisodes().observeAsState(emptyList())
                    PodcastChannelPageScreen(
                        channel = channelState,
                        episodes = episodes,
                        onNavigateBack = { findNavController().navigateUp() },
                        onEpisodeClick = {
                            MediaManager.startPodcast(mediaBrowserListenableFuture, it)
                            activity.setBottomSheetInPeek(true)
                        },
                        onEpisodeLongClick = {
                            findNavController().navigate(
                                R.id.podcastEpisodeBottomSheetDialog,
                                Bundle().apply { putSerializable(Constants.PODCAST_OBJECT, it) }
                            )
                        },
                        onEpisodeDownloadClick = { viewModel.requestPodcastEpisodeDownload(it) }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mediaBrowserListenableFuture = MediaBrowser.Builder(
            requireContext(),
            SessionToken(requireContext(), ComponentName(requireContext(), MediaService::class.java))
        ).buildAsync()
    }

    override fun onStop() {
        mediaBrowserListenableFuture?.let { MediaBrowser.releaseFuture(it) }
        super.onStop()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PodcastChannelPageScreen(
    channel: PodcastChannel,
    episodes: List<PodcastEpisode>,
    onNavigateBack: () -> Unit,
    onEpisodeClick: (PodcastEpisode) -> Unit,
    onEpisodeLongClick: (PodcastEpisode) -> Unit,
    onEpisodeDownloadClick: (PodcastEpisode) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(channel.title.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        if (episodes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues()) {
                items(episodes) { episode ->
                    ListItem(
                        modifier = Modifier.clickable { onEpisodeClick(episode) },
                        headlineContent = { Text(episode.title.orEmpty()) },
                        supportingContent = { Text(episode.description.orEmpty()) },
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    if (episode.status == "completed") onEpisodeLongClick(episode)
                                    else onEpisodeDownloadClick(episode)
                                }
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                            }
                        }
                    )
                }
            }
        }
    }
}
