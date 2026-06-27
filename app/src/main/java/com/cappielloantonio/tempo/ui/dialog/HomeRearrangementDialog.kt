package com.cappielloantonio.tempo.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.model.HomeSector
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.viewmodel.HomeRearrangementViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

@UnstableApi
class HomeRearrangementDialog : BottomSheetDialogFragment() {

    private lateinit var viewModel: HomeRearrangementViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(requireActivity())[HomeRearrangementViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                TempusTheme {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.large
                    ) {
                        HomeRearrangementContent(
                            viewModel = viewModel,
                            onDismiss = { dismiss() }
                        )
                    }
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    @Composable
    fun HomeRearrangementContent(
        viewModel: HomeRearrangementViewModel,
        onDismiss: () -> Unit
    ) {
        var sectors by remember { mutableStateOf(viewModel.getHomeSectorList(mutableListOf())) }
        var carouselEnabled by remember { mutableStateOf(Preferences.isLibraryMaterialCarouselEnabled()) }

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxHeight(0.8f)
        ) {
            Text(
                text = stringResource(id = R.string.home_rearrangement_dialog_title),
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.home_rearrangement_dialog_library_carousel_title),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(id = R.string.home_rearrangement_dialog_library_carousel_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = carouselEnabled,
                    onCheckedChange = { carouselEnabled = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
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
                                    }
                                )
                                Icon(Icons.Default.DragHandle, null)
                            }
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    viewModel.resetHomeSectorList()
                    Preferences.setLibraryMaterialCarouselEnabled(true)
                    onDismiss()
                }) {
                    Text(stringResource(id = R.string.home_rearrangement_dialog_neutral_button))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(id = R.string.home_rearrangement_dialog_negative_button))
                }
                Button(onClick = {
                    viewModel.saveHomeSectorList(ArrayList(sectors))
                    Preferences.setLibraryMaterialCarouselEnabled(carouselEnabled)
                    onDismiss()
                }) {
                    Text(stringResource(id = R.string.home_rearrangement_dialog_positive_button))
                }
            }
        }
    }
}
