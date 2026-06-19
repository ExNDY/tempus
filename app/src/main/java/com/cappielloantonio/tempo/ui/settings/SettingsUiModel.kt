package com.cappielloantonio.tempo.ui.settings

data class SettingsUiState(
    val sections: List<SettingsSectionUiModel> = emptyList(),
    val selectionDialog: SettingsSelectionDialogState? = null,
    val textInputDialog: SettingsTextInputDialogState? = null,
)

data class SettingsSectionUiModel(
    val key: String,
    val title: String,
    val expanded: Boolean,
    val groups: List<SettingsGroupUiModel>,
)

data class SettingsGroupUiModel(
    val title: String? = null,
    val items: List<SettingsItemUiModel>,
)

data class SettingsOptionUiModel(
    val title: String,
    val value: String,
)

data class SettingsSelectionDialogState(
    val key: String,
    val title: String,
    val options: List<SettingsOptionUiModel>,
    val selectedValue: String,
)

data class SettingsTextInputDialogState(
    val key: String,
    val title: String,
    val value: String,
    val supportingText: String? = null,
)

sealed interface SettingsItemUiModel {
    val key: String
}

data class SettingsInfoItemUiModel(
    override val key: String,
    val text: String,
) : SettingsItemUiModel

data class SettingsActionItemUiModel(
    override val key: String,
    val title: String,
    val summary: String? = null,
    val value: String? = null,
    val enabled: Boolean = true,
    val showChevron: Boolean = true,
    val destructive: Boolean = false,
) : SettingsItemUiModel

data class SettingsToggleItemUiModel(
    override val key: String,
    val title: String,
    val summary: String? = null,
    val checked: Boolean,
    val enabled: Boolean = true,
) : SettingsItemUiModel

data class SettingsSelectItemUiModel(
    override val key: String,
    val title: String,
    val summary: String? = null,
    val selectedLabel: String,
    val enabled: Boolean = true,
) : SettingsItemUiModel

data class SettingsSliderItemUiModel(
    override val key: String,
    val title: String,
    val summary: String? = null,
    val value: Int,
    val valueRange: IntRange,
    val steps: Int = 0,
    val trailingValue: String = value.toString(),
    val enabled: Boolean = true,
) : SettingsItemUiModel

data class SettingsValueItemUiModel(
    override val key: String,
    val title: String,
    val summary: String? = null,
    val value: String,
) : SettingsItemUiModel

data class SettingsInputItemUiModel(
    override val key: String,
    val title: String,
    val summary: String? = null,
    val value: String,
    val enabled: Boolean = true,
) : SettingsItemUiModel
