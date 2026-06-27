package com.cappielloantonio.tempo.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.subsonic.models.PodcastChannel
import com.cappielloantonio.tempo.subsonic.models.PodcastEpisode
import com.cappielloantonio.tempo.ui.components.TempusImage
import com.cappielloantonio.tempo.ui.components.TempusImageType
import com.cappielloantonio.tempo.viewmodel.PodcastUiState

@Composable
fun HomeTabPodcastScreen(
    uiState: PodcastUiState,
    onHideSectionClick: () -> Unit,
    onAddChannelClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onSeeAllChannelsClick: () -> Unit,
    onChannelClick: (PodcastChannel) -> Unit,
    onEpisodeClick: (PodcastEpisode) -> Unit,
    onEpisodeLongClick: (PodcastEpisode) -> Unit,
) {
    if (uiState.channels.isEmpty() && !uiState.isLoading) {
        PodcastEmptyState(
            onHideClick = onHideSectionClick,
            onAddClick = onAddChannelClick
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            SectionHeader(
                title = stringResource(id = R.string.home_title_podcast_channels),
                onSeeAll = onSeeAllChannelsClick,
                onAction = onAddChannelClick,
                actionIcon = { Icon(Icons.Default.Add, contentDescription = null) }
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.channels) { channel ->
                    PodcastChannelItem(channel = channel, onClick = { onChannelClick(channel) })
                }
            }
        }

        item {
            SectionHeader(
                title = stringResource(id = R.string.home_title_newest_podcasts),
                onAction = onRefreshClick,
                actionIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
            )
        }

        items(uiState.newestEpisodes) { episode ->
            ListItem(
                modifier = Modifier
                    .clickable(onClick = { onEpisodeClick(episode) }),
                headlineContent = {
                    Text(
                        text = episode.title ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                supportingContent = {
                    Text(
                        text = episode.description ?: "",
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingContent = {
                    TempusImage(
                        coverArtId = episode.coverArtId,
                        imageType = TempusImageType.Podcast,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(MaterialTheme.shapes.small)
                    )
                }
            )
        }
    }
}

@Composable
fun PodcastChannelItem(channel: PodcastChannel, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TempusImage(
            coverArtId = channel.coverArtId,
            imageType = TempusImageType.Podcast,
            modifier = Modifier
                .size(120.dp)
                .clip(MaterialTheme.shapes.medium)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = channel.title ?: "",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PodcastEmptyState(
    onHideClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.podcast_info_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.podcast_info_empty_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onAddClick) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(id = R.string.menu_add_button))
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onHideClick) {
            Icon(imageVector = Icons.Default.Close, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.podcast_info_empty_button),
                textAlign = TextAlign.Center
            )
        }
    }
}
