package com.cappielloantonio.tempo.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.ui.theme.TempusTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    onNavigateBack: () -> Unit,
    onActionClick: (SettingsActionItemUiModel) -> Unit,
    onToggleChange: (SettingsToggleItemUiModel, Boolean) -> Unit,
    onSelectClick: (SettingsSelectItemUiModel) -> Unit,
    onSliderChange: (SettingsSliderItemUiModel, Int) -> Unit,
    onInputClick: (SettingsInputItemUiModel) -> Unit,
) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val spacing = TempusTheme.spacing

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = backgroundColor,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                ),
                title = { Text(text = androidx.compose.ui.res.stringResource(id = R.string.menu_settings_button)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.s)
        ) {
            items(
                items = uiState.sections,
                key = { it.key }
            ) { section ->
                SettingsSectionCard(
                    section = section,
                    onActionClick = onActionClick,
                    onToggleChange = onToggleChange,
                    onSelectClick = onSelectClick,
                    onSliderChange = onSliderChange,
                    onInputClick = onInputClick,
                )
            }
        }
    }
}
