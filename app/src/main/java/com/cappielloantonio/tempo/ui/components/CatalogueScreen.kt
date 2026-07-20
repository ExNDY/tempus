package com.cappielloantonio.tempo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Genre
import com.cappielloantonio.tempo.subsonic.models.Playlist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> CatalogueScreen(
    items: List<T>,
    title: String,
    isLoading: Boolean,
    onItemClick: (T) -> Unit,
    onNavigateBack: () -> Unit,
    onSearchClick: () -> Unit,
    onFilterClick: (() -> Unit)? = null,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (onFilterClick != null) {
                        IconButton(onClick = onFilterClick) {
                            Icon(imageVector = Icons.Default.FilterList, contentDescription = null)
                        }
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading && items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items) { item ->
                    CatalogueItem(item = item, onClick = { onItemClick(item) })
                }
            }
        }
    }
}

@Composable
fun <T> CatalogueItem(item: T, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val (title, coverArtId, type) = when (item) {
            is AlbumID3 -> Triple(item.name ?: "", item.coverArtId, TempusImageType.Album)
            is ArtistID3 -> Triple(item.name ?: "", item.coverArtId, TempusImageType.Artist)
            is Genre -> Triple(item.genre ?: "", null, TempusImageType.Unknown)
            is Playlist -> Triple(item.name ?: "", item.coverArtId, TempusImageType.Playlist)
            else -> Triple("", null, TempusImageType.Unknown)
        }

        TempusImage(
            coverArtId = coverArtId,
            imageType = type,
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(if (type == TempusImageType.Artist) MaterialTheme.shapes.extraLarge else MaterialTheme.shapes.medium)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        if (item is AlbumID3) {
            Text(
                text = item.artist ?: "",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
