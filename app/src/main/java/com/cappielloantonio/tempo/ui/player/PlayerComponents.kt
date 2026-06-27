package com.cappielloantonio.tempo.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.ui.components.AssetLinkChips
import java.util.Locale

@Composable
fun PlayerProgressBar(
    progress: Long,
    duration: Long,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var isUserSeeking by remember { mutableStateOf(false) }

    LaunchedEffect(progress, duration, isUserSeeking) {
        if (!isUserSeeking) {
            val boundedProgress = progress.coerceIn(0L, duration.coerceAtLeast(0L))
            sliderValue = boundedProgress.toFloat()
        }
    }

    val maxDuration = duration.toFloat().coerceAtLeast(1f)
    val displayedProgress = if (isUserSeeking) sliderValue.toLong() else progress

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = sliderValue.coerceIn(0f, maxDuration),
            onValueChange = {
                isUserSeeking = true
                sliderValue = it
            },
            onValueChangeFinished = {
                onValueChange(sliderValue.toLong())
                isUserSeeking = false
            },
            valueRange = 0f..maxDuration,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(displayedProgress),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    val hours = (durationMs / (1000 * 60 * 60))
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onShuffleClick) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = stringResource(R.string.content_description_shuffle_button),
                tint = if (shuffleModeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = onPreviousClick) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
        }
        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        IconButton(onClick = onNextClick) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
        }
        IconButton(onClick = onRepeatClick) {
            val repeatIcon = when (repeatMode) {
                1 -> Icons.Default.RepeatOne
                2 -> Icons.Default.Repeat
                else -> Icons.Default.Repeat
            }
            Icon(
                imageVector = repeatIcon,
                contentDescription = null,
                tint = if (repeatMode != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SimpleRatingBar(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        repeat(5) { index ->
            IconButton(
                onClick = { onRatingChange(index + 1) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (index < rating) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = if (index < rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
