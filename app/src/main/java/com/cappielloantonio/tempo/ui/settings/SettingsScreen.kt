package com.cappielloantonio.tempo.ui.settings

import androidx.compose.runtime.Composable

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onNavigateBack: () -> Unit,
    onSectionToggle: (String) -> Unit,
    onActionClick: (SettingsActionItemUiModel) -> Unit,
    onToggleChange: (SettingsToggleItemUiModel, Boolean) -> Unit,
    onSelectClick: (SettingsSelectItemUiModel) -> Unit,
    onSliderChange: (SettingsSliderItemUiModel, Int) -> Unit,
    onInputClick: (SettingsInputItemUiModel) -> Unit,
    onSelectionDismiss: () -> Unit,
    onSelectionConfirm: (String, String) -> Unit,
    onTextInputDismiss: () -> Unit,
    onTextInputConfirm: (String, String) -> Unit,
) {
    SettingsScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onSectionToggle = onSectionToggle,
        onActionClick = onActionClick,
        onToggleChange = onToggleChange,
        onSelectClick = onSelectClick,
        onSliderChange = onSliderChange,
        onInputClick = onInputClick,
    )

    uiState.selectionDialog?.let { dialogState ->
        SettingsSelectionDialog(
            state = dialogState,
            onDismiss = onSelectionDismiss,
            onConfirm = { selectedValue ->
                onSelectionConfirm(dialogState.key, selectedValue)
            }
        )
    }

    uiState.textInputDialog?.let { dialogState ->
        SettingsTextInputDialog(
            state = dialogState,
            onDismiss = onTextInputDismiss,
            onConfirm = { enteredValue ->
                onTextInputConfirm(dialogState.key, enteredValue)
            }
        )
    }
}
