package com.cappielloantonio.tempo.ui.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.ui.theme.TempusTheme

@Composable
fun SettingsSectionCard(
    section: SettingsSectionUiModel,
    onSectionToggle: (String) -> Unit,
    onActionClick: (SettingsActionItemUiModel) -> Unit,
    onToggleChange: (SettingsToggleItemUiModel, Boolean) -> Unit,
    onSelectClick: (SettingsSelectItemUiModel) -> Unit,
    onSliderChange: (SettingsSliderItemUiModel, Int) -> Unit,
    onInputClick: (SettingsInputItemUiModel) -> Unit,
) {
    val spacing = TempusTheme.spacing
    val sectionShape = RoundedCornerShape(32.dp)
    val indicatorRotation = animateFloatAsState(
        targetValue = if (section.expanded) 90f else 0f,
        label = "settingsSectionIndicatorRotation"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = sectionShape,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(vertical = spacing.xxs)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(sectionShape)
                    .clickable { onSectionToggle(section.key) }
                    .padding(horizontal = spacing.md, vertical = spacing.sm)
            ) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(end = spacing.lg)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.CenterEnd)
                        .graphicsLayer {
                            rotationZ = indicatorRotation.value
                        }
                )
            }

            if (section.expanded) {
                Column(
                    modifier = Modifier.padding(
                        start = spacing.xs,
                        end = spacing.xs,
                        bottom = spacing.xs
                    ),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
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
