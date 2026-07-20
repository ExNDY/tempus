package com.cappielloantonio.tempo.ui.folder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.subsonic.models.Artist
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.viewmodel.IndexUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndexScreen(
    uiState: IndexUiState,
    title: String,
    onNavigateBack: () -> Unit,
    onArtistClick: (Artist) -> Unit,
    onArtistPlayClick: (Artist) -> Unit,
    onChildClick: (Child) -> Unit,
    onChildLongClick: (Child) -> Unit,
    onChildPlayClick: (Child) -> Unit,
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
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (uiState.children.isNotEmpty()) {
                    items(
                        items = uiState.children,
                        key = { child -> child.id },
                    ) { child ->
                        DirectoryChildRow(
                            child = child,
                            onClick = { onChildClick(child) },
                            onLongClick = if (child.isDir) null else {
                                { onChildLongClick(child) }
                            },
                            onPlayClick = { onChildPlayClick(child) },
                        )
                    }
                }

                uiState.indices.forEach { index ->
                    item {
                        Text(
                            text = index.name.orEmpty(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    items(
                        items = index.artists.orEmpty(),
                        key = { artist -> artist.id.orEmpty() + artist.name.orEmpty() },
                    ) { artist ->
                        ListItem(
                            modifier = Modifier.clickable { onArtistClick(artist) },
                            headlineContent = { Text(artist.name.orEmpty()) },
                            trailingContent = {
                                IconButton(onClick = { onArtistPlayClick(artist) }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
