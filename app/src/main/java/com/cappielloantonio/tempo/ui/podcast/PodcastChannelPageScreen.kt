package com.cappielloantonio.tempo.ui.podcast

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.subsonic.models.PodcastEpisode
import com.cappielloantonio.tempo.subsonic.models.PodcastStatus
import com.cappielloantonio.tempo.ui.components.TempusImage
import com.cappielloantonio.tempo.ui.components.TempusImageType
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.viewmodel.PodcastChannelPageUiState
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastChannelPageScreen(
    uiState: PodcastChannelPageUiState,
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit,
    onEpisodeClick: (PodcastEpisode) -> Unit,
    onEpisodeLongClick: (PodcastEpisode) -> Unit,
    onEpisodeDownloadClick: (PodcastEpisode) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.channel?.title.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(id = R.string.menu_refresh),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.isLoading && uiState.episodes.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        PodcastChannelHeader(uiState = uiState)
                    }

                    item {
                        Text(
                            text = stringResource(id = R.string.podcast_channel_page_title_episode_section),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }

                    if (uiState.episodes.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = stringResource(id = R.string.podcast_channel_page_title_no_episode_available))
                            }
                        }
                    } else {
                        items(uiState.episodes, key = { it.id.orEmpty() }) { episode ->
                            PodcastEpisodeListItem(
                                episode = episode,
                                onClick = { onEpisodeClick(episode) },
                                onLongClick = { onEpisodeLongClick(episode) },
                                onDownloadClick = { onEpisodeDownloadClick(episode) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodcastChannelHeader(uiState: PodcastChannelPageUiState) {
    val channel = uiState.channel ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TempusImage(
                coverArtId = channel.coverArtId,
                imageType = TempusImageType.Podcast,
                modifier = Modifier
                    .size(88.dp)
                    .clip(MaterialTheme.shapes.large),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.title.orEmpty(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!channel.url.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = channel.url.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (!channel.description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(id = R.string.podcast_channel_page_title_description_section),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = MusicUtil.getReadableString(channel.description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PodcastEpisodeListItem(
    episode: PodcastEpisode,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDownloadClick: () -> Unit,
) {
    val isCompleted = episode.status == PodcastStatus.COMPLETED
    val formatter = SimpleDateFormat("MMM d", Locale.getDefault())
    val metadata = if (episode.publishDate != null && episode.duration != null) {
        stringResource(
            id = R.string.podcast_release_date_duration_formatter,
            formatter.format(episode.publishDate),
            MusicUtil.getReadablePodcastDurationString(episode.duration?.toLong() ?: 0L),
        )
    } else {
        episode.artist.orEmpty()
    }

    ListItem(
        modifier = Modifier.combinedClickable(
            onClick = {
                if (isCompleted) {
                    onClick()
                }
            },
            onLongClick = {
                if (isCompleted) {
                    onLongClick()
                }
            },
        ),
        headlineContent = {
            Text(
                text = episode.title.orEmpty(),
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Column {
                if (metadata.isNotBlank()) {
                    Text(
                        text = metadata,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!episode.description.isNullOrBlank()) {
                    Text(
                        text = MusicUtil.getReadableString(episode.description),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        leadingContent = {
            TempusImage(
                coverArtId = episode.coverArtId,
                imageType = TempusImageType.Podcast,
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.large),
            )
        },
        trailingContent = {
            IconButton(onClick = if (isCompleted) onLongClick else onDownloadClick) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.MoreVert else Icons.Default.Download,
                    contentDescription = null,
                    tint = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                )
            }
        },
    )
}
