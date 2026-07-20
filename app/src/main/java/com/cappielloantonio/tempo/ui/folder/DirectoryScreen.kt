package com.cappielloantonio.tempo.ui.folder

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.components.TempusImage
import com.cappielloantonio.tempo.ui.components.TempusImageType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DirectoryScreen(
    title: String,
    breadcrumb: String,
    children: List<Child>,
    isLoading: Boolean,
    onNavigateBack: () -> Unit,
    onItemClick: (Child) -> Unit,
    onItemLongClick: (Child) -> Unit,
    onItemPlayClick: (Child) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { _ ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(),
            ) {
                item {
                    Text(
                        text = breadcrumb,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                itemsIndexed(
                    items = children,
                    key = { _, child -> child.id },
                ) { _, child ->
                    DirectoryChildRow(
                        child = child,
                        onClick = { onItemClick(child) },
                        onLongClick = if (child.isDir) null else {
                            { onItemLongClick(child) }
                        },
                        onPlayClick = { onItemPlayClick(child) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DirectoryChildRow(
    child: Child,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    onPlayClick: () -> Unit,
) {
    val imageType = if (child.isDir) TempusImageType.Directory else TempusImageType.Song

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        TempusImage(
            coverArtId = child.coverArtId,
            imageType = imageType,
            modifier = Modifier
                .size(52.dp)
                .clip(MaterialTheme.shapes.small),
        )
        if (child.isDir) {
            IconButton(
                onClick = onPlayClick,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(32.dp),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
            }
        } else {
            Spacer(modifier = Modifier.width(44.dp))
        }
        Text(
            text = child.title.orEmpty(),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(start = 12.dp, end = 12.dp)
                .weight(1f),
        )
        if (child.isDir) {
            Icon(
                painter = painterResource(R.drawable.ic_navigate_next),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Spacer(modifier = Modifier.width(46.dp))
        }
    }
}
