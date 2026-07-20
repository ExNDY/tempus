package com.cappielloantonio.tempo.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.viewmodel.HomeRearrangementViewModel

@Composable
fun HomeRearrangementRouteSheet(
    onDismiss: () -> Unit,
    onHomeLayoutChanged: () -> Unit,
) {
    val viewModel: HomeRearrangementViewModel = viewModel()
    var sectors by remember { mutableStateOf(viewModel.getHomeSectorList(mutableListOf())) }
    var carouselEnabled by remember { mutableStateOf(Preferences.isLibraryMaterialCarouselEnabled()) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxHeight(0.8f),
    ) {
        Text(
            text = stringResource(id = R.string.home_rearrangement_dialog_title),
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.home_rearrangement_dialog_library_carousel_title),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(id = R.string.home_rearrangement_dialog_library_carousel_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = carouselEnabled,
                onCheckedChange = { carouselEnabled = it },
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(sectors) { sector ->
                ListItem(
                    headlineContent = { Text(sector.sectorTitle.ifEmpty { sector.id }) },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = sector.isVisible,
                                onCheckedChange = { isChecked ->
                                    sectors = sectors.map {
                                        if (it.id == sector.id) it.copy(isVisible = isChecked) else it
                                    }.toMutableList()
                                },
                            )
                            Icon(Icons.Default.DragHandle, null)
                        }
                    },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = {
                    viewModel.resetHomeSectorList()
                    Preferences.setLibraryMaterialCarouselEnabled(true)
                    onHomeLayoutChanged()
                    onDismiss()
                },
            ) {
                Text(stringResource(id = R.string.home_rearrangement_dialog_neutral_button))
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.home_rearrangement_dialog_negative_button))
            }
            Button(
                onClick = {
                    viewModel.saveHomeSectorList(ArrayList(sectors))
                    Preferences.setLibraryMaterialCarouselEnabled(carouselEnabled)
                    onHomeLayoutChanged()
                    onDismiss()
                },
            ) {
                Text(stringResource(id = R.string.home_rearrangement_dialog_positive_button))
            }
        }
    }
}
