package com.cappielloantonio.tempo.ui.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.subsonic.models.Genre
import com.cappielloantonio.tempo.viewmodel.FilterUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(
    uiState: FilterUiState,
    onNavigateBack: () -> Unit,
    onGenreToggle: (Genre, Boolean) -> Unit,
    onApplyClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.filter_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = onApplyClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(stringResource(R.string.filter_title))
            }
        },
    ) { padding ->
        if (uiState.isLoading && uiState.genres.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.filter_title_expanded),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                item {
                    Text(
                        text = "${uiState.selectedFilterNames.size}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                items(uiState.genres, key = { genre -> genre.genre.orEmpty() }) { genre ->
                    FilterGenreRow(
                        genre = genre,
                        selected = uiState.selectedFilterIds.contains(genre.genre.orEmpty()),
                        onToggle = { selected -> onGenreToggle(genre, selected) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterGenreRow(
    genre: Genre,
    selected: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    FlowRow {
        FilterChip(
            selected = selected,
            onClick = { onToggle(selected) },
            label = { Text(genre.genre.orEmpty()) },
        )
    }
}
