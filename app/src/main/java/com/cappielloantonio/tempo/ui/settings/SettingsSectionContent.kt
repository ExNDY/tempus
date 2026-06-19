package com.cappielloantonio.tempo.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.ui.theme.TempusTheme

@Composable
fun SettingsSectionCard(
    section: SettingsSectionUiModel,
    onActionClick: (SettingsActionItemUiModel) -> Unit,
    onToggleChange: (SettingsToggleItemUiModel, Boolean) -> Unit,
    onSelectClick: (SettingsSelectItemUiModel) -> Unit,
    onSliderChange: (SettingsSliderItemUiModel, Int) -> Unit,
    onInputClick: (SettingsInputItemUiModel) -> Unit,
) {
    val spacing = TempusTheme.spacing

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = spacing.sm)
        )

        section.groups.forEach { group ->
            SettingsGroup(
                group = group,
                onActionClick = onActionClick,
                onToggleChange = onToggleChange,
                onSelectClick = onSelectClick,
                onSliderChange = onSliderChange,
                onInputClick = onInputClick,
            )
        }
    }
}

@Composable
private fun SettingsGroup(
    group: SettingsGroupUiModel,
    onActionClick: (SettingsActionItemUiModel) -> Unit,
    onToggleChange: (SettingsToggleItemUiModel, Boolean) -> Unit,
    onSelectClick: (SettingsSelectItemUiModel) -> Unit,
    onSliderChange: (SettingsSliderItemUiModel, Int) -> Unit,
    onInputClick: (SettingsInputItemUiModel) -> Unit,
) {
    val spacing = TempusTheme.spacing

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.xxs)
    ) {
        group.title?.let { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = spacing.sm)
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 1.dp,
            shadowElevation = 0.dp,
        ) {
            Column {
                group.items.forEachIndexed { index, item ->
                    when (item) {
                        is SettingsActionItemUiModel -> SettingsActionRow(item = item, onClick = { onActionClick(item) })
                        is SettingsInfoItemUiModel -> SettingsInfoRow(item = item)
                        is SettingsInputItemUiModel -> SettingsInputRow(item = item, onClick = { onInputClick(item) })
                        is SettingsSelectItemUiModel -> SettingsSelectRow(item = item, onClick = { onSelectClick(item) })
                        is SettingsSliderItemUiModel -> SettingsSliderRow(item = item, onValueChange = { onSliderChange(item, it) })
                        is SettingsToggleItemUiModel -> SettingsSwitchRow(item = item, onCheckedChange = { onToggleChange(item, it) })
                        is SettingsValueItemUiModel -> SettingsValueRow(item = item)
                    }

                    if (index != group.items.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = spacing.sm),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        }
    }
}
