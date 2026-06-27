package com.cappielloantonio.tempo.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    onLineClick: (Int) -> Unit,
    onSyncToggle: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    val lines = uiState.lyricsList?.structuredLyrics?.firstOrNull()?.line ?: emptyList()
    val isSynced = uiState.lyricsList?.structuredLyrics?.firstOrNull()?.synced ?: false

    // Find current line index
    val currentLineIndex = if (isSynced) {
        lines.indexOfLast { (it.start ?: 0) <= currentPosition }.coerceAtLeast(0)
    } else -1

    // Auto-scroll to current line
    LaunchedEffect(currentLineIndex, uiState.isLyricsSynced) {
        if (uiState.isLyricsSynced && currentLineIndex >= 0) {
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
                        onClick = { if (isSynced) onLineClick(line.start ?: 0) }
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
            if (isSynced) {
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
                    contentDescription = null,
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
        text = line.value ?: "",
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
