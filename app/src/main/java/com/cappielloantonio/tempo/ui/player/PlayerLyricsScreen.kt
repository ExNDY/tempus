package com.cappielloantonio.tempo.ui.player

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.subsonic.models.Line
import com.cappielloantonio.tempo.viewmodel.PlayerUiState

@Composable
fun PlayerLyricsScreen(
    uiState: PlayerUiState,
    currentPosition: Long,
    onLineClick: (Long) -> Unit,
    onSyncToggle: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    val lines = uiState.lyricsList?.structuredLyrics?.firstOrNull()?.line ?: emptyList()
    val hasTimedLyrics = uiState.lyricsList?.structuredLyrics?.firstOrNull()?.synced == true
    val isSynced = hasTimedLyrics && uiState.isLyricsSynced

    // Find current line index
    val currentLineIndex = if (isSynced) {
        lines.indexOfLast { (it.start ?: 0L) <= currentPosition }.coerceAtLeast(0)
    } else -1

    // Auto-scroll to current line
    LaunchedEffect(currentLineIndex, isSynced) {
        if (isSynced && currentLineIndex >= 0) {
            listState.animateScrollToItem(currentLineIndex, scrollOffset = -200)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (lines.isNotEmpty()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 100.dp, bottom = 200.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(lines) { index, line ->
                    val isCurrent = index == currentLineIndex
                    LyricsLine(
                        line = line,
                        isCurrent = isCurrent,
                        onClick = { if (isSynced) onLineClick(line.start ?: 0L) }
                    )
                }
            }
        } else if (!uiState.lyrics.isNullOrEmpty()) {
            // Unstructured lyrics
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Text(
                        text = uiState.lyrics,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 32.sp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Empty state
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.SyncDisabled,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.description_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Overlay controls
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (hasTimedLyrics) {
                IconButton(onClick = onSyncToggle) {
                    Icon(
                        imageVector = if (uiState.isLyricsSynced) Icons.Default.Sync else Icons.Default.SyncDisabled,
                        contentDescription = null,
                        tint = if (uiState.isLyricsSynced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            IconButton(onClick = onDownloadClick, enabled = !uiState.isLyricsCached) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = stringResource(
                        if (uiState.isLyricsCached) {
                            R.string.player_lyrics_downloaded_content_description
                        } else {
                            R.string.player_lyrics_download_content_description
                        }
                    ),
                    tint = if (uiState.isLyricsCached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun LyricsLine(
    line: Line,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = line.value,
        style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (isCurrent) 24.sp else 20.sp
        ),
        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 24.dp)
    )
}
