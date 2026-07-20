package com.cappielloantonio.tempo.ui.settings

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.ui.components.CollapsingToolbarScreen
import com.cappielloantonio.tempo.ui.theme.TempusTheme

@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    onNavigateBack: () -> Unit,
    onSectionToggle: (String) -> Unit,
    onActionClick: (SettingsActionItemUiModel) -> Unit,
    onToggleChange: (SettingsToggleItemUiModel, Boolean) -> Unit,
    onSelectClick: (SettingsSelectItemUiModel) -> Unit,
    onSliderChange: (SettingsSliderItemUiModel, Int) -> Unit,
    onInputClick: (SettingsInputItemUiModel) -> Unit,
) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val spacing = TempusTheme.spacing

    CollapsingToolbarScreen(
        title = androidx.compose.ui.res.stringResource(id = R.string.menu_settings_button),
        onNavigateBack = onNavigateBack,
        containerColor = backgroundColor,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(spacing.sm),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(spacing.s)
    ) {
        items(
            items = uiState.sections,
            key = { it.key }
        ) { section ->
            SettingsSectionCard(
                section = section,
                onSectionToggle = onSectionToggle,
                onActionClick = onActionClick,
                onToggleChange = onToggleChange,
                onSelectClick = onSelectClick,
                onSliderChange = onSliderChange,
                onInputClick = onInputClick,
            )
        }
    }
}
