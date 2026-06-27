package com.cappielloantonio.tempo.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.cappielloantonio.tempo.subsonic.models.InternetRadioStation
import com.cappielloantonio.tempo.ui.components.TempusImage
import com.cappielloantonio.tempo.ui.components.TempusImageType
import com.cappielloantonio.tempo.viewmodel.RadioUiState

@Composable
fun HomeTabRadioScreen(
    uiState: RadioUiState,
    onHideSectionClick: () -> Unit,
    onAddStationClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onStationClick: (InternetRadioStation) -> Unit,
    onStationLongClick: (InternetRadioStation) -> Unit,
) {
    if (uiState.stations.isEmpty() && !uiState.isLoading) {
        RadioEmptyState(
            onHideClick = onHideSectionClick,
            onAddClick = onAddStationClick
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            SectionHeader(
                title = stringResource(id = R.string.home_title_radio_station),
                onAction = onAddStationClick,
                actionIcon = { Icon(Icons.Default.Add, contentDescription = null) }
            )
        }

        items(uiState.stations) { station ->
            ListItem(
                modifier = Modifier
                    .clickable(onClick = { onStationClick(station) }),
                headlineContent = {
                    Text(
                        text = station.name ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                supportingContent = {
                    Text(
                        text = station.streamUrl ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingContent = {
                    TempusImage(
                        coverArtId = null,
                        imageType = TempusImageType.Radio,
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
fun RadioEmptyState(
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
            text = stringResource(id = R.string.radio_station_info_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.radio_station_info_empty_subtitle),
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
                text = stringResource(id = R.string.radio_station_info_empty_button),
                textAlign = TextAlign.Center
            )
        }
    }
}
