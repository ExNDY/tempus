package com.cappielloantonio.tempo.ui.podcast

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.subsonic.models.PodcastChannel
import com.cappielloantonio.tempo.ui.components.TempusImage
import com.cappielloantonio.tempo.ui.components.TempusImageType
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.viewmodel.PodcastChannelCatalogueUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastChannelCatalogueScreen(
    uiState: PodcastChannelCatalogueUiState,
    title: String,
    onChannelClick: (PodcastChannel) -> Unit,
    onChannelLongClick: (PodcastChannel) -> Unit,
    onCreateChannel: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredChannels = remember(uiState.channels, query) {
        uiState.channels.filter { channel ->
            query.isBlank() ||
                channel.title?.contains(query, ignoreCase = true) == true ||
                channel.description?.contains(query, ignoreCase = true) == true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onCreateChannel) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(id = R.string.menu_add_button),
                        )
                    }
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
            uiState.isLoading && uiState.channels.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null)
                            },
                            placeholder = {
                                Text(text = stringResource(id = R.string.search_hint))
                            },
                        )
                    }

                    if (filteredChannels.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (uiState.isUnsupported) {
                                    stringResource(id = R.string.podcast_channel_not_supported_snackbar)
                                } else {
                                    stringResource(id = R.string.podcast_info_empty_title)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(filteredChannels, key = { it.id.orEmpty() }) { channel ->
                                PodcastChannelListItem(
                                    channel = channel,
                                    onClick = { onChannelClick(channel) },
                                    onLongClick = { onChannelLongClick(channel) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PodcastChannelListItem(
    channel: PodcastChannel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        headlineContent = {
            Text(
                text = channel.title.orEmpty(),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            if (!channel.description.isNullOrBlank()) {
                Text(
                    text = MusicUtil.getReadableString(channel.description),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        leadingContent = {
            TempusImage(
                coverArtId = channel.coverArtId,
                imageType = TempusImageType.Podcast,
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.large),
            )
        },
    )
}
