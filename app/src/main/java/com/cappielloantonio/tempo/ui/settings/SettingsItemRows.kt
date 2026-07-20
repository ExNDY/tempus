package com.cappielloantonio.tempo.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.ui.theme.TempusTheme

@Composable
internal fun SettingsActionRow(
    item: SettingsActionItemUiModel,
    onClick: () -> Unit,
) {
    SettingsRow(
        enabled = item.enabled,
        onClick = onClick
    ) {
        TextBlock(
            title = item.title,
            summary = item.summary,
            modifier = Modifier.weight(1f),
            titleColor = if (item.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        SettingsTrailingContent(value = item.value, showChevron = item.showChevron)
    }
}

@Composable
internal fun SettingsSelectRow(
    item: SettingsSelectItemUiModel,
    onClick: () -> Unit,
) {
    SettingsRow(
        enabled = item.enabled,
        onClick = onClick
    ) {
        SelectionTextBlock(
            title = item.title,
            summary = item.summary,
            selectedValue = item.selectedLabel,
            modifier = Modifier.weight(1f)
        )
        SettingsTrailingContent(value = null, showChevron = true)
    }
}

@Composable
internal fun SettingsInputRow(
    item: SettingsInputItemUiModel,
    onClick: () -> Unit,
) {
    SettingsRow(
        enabled = item.enabled,
        onClick = onClick
    ) {
        SelectionTextBlock(
            title = item.title,
            summary = item.summary,
            selectedValue = item.value,
            modifier = Modifier.weight(1f)
        )
        SettingsTrailingContent(value = null, showChevron = true)
    }
}

@Composable
internal fun SettingsSwitchRow(
    item: SettingsToggleItemUiModel,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsRow(
        enabled = item.enabled,
        onClick = { onCheckedChange(!item.checked) }
    ) {
        TextBlock(
            title = item.title,
            summary = item.summary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = item.checked,
            onCheckedChange = onCheckedChange,
            enabled = item.enabled
        )
    }
}

@Composable
internal fun SettingsSliderRow(
    item: SettingsSliderItemUiModel,
    onValueChange: (Int) -> Unit,
) {
    val spacing = TempusTheme.spacing

    Column(
        modifier = Modifier.padding(spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextBlock(
                title = item.title,
                summary = item.summary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = item.trailingValue,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = item.value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = item.valueRange.first.toFloat()..item.valueRange.last.toFloat(),
            steps = item.steps,
            enabled = item.enabled
        )
    }
}

@Composable
internal fun SettingsValueRow(item: SettingsValueItemUiModel) {
    SettingsRow(enabled = false, onClick = {}) {
        TextBlock(
            title = item.title,
            summary = item.summary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = item.value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End
        )
    }
}

@Composable
internal fun SettingsInfoRow(item: SettingsInfoItemUiModel) {
    val spacing = TempusTheme.spacing

    Text(
        text = item.text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(spacing.sm)
    )
}

@Composable
private fun SettingsRow(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val spacing = TempusTheme.spacing
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedColor = if (enabled && isPressed) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = spacing.xxs, vertical = 2.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(pressedColor)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                onClick = onClick
            )
            .padding(horizontal = spacing.sm, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        content = content
    )
}

@Composable
private fun TextBlock(
    title: String,
    summary: String?,
    modifier: Modifier = Modifier,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = titleColor
        )
        if (!summary.isNullOrBlank()) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SelectionTextBlock(
    title: String,
    summary: String?,
    selectedValue: String,
    modifier: Modifier = Modifier,
) {
    val spacing = TempusTheme.spacing

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.xxs)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (!summary.isNullOrBlank()) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = selectedValue,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SettingsTrailingContent(
    value: String?,
    showChevron: Boolean,
) {
    val spacing = TempusTheme.spacing

    Row(
        modifier = Modifier.padding(start = spacing.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        value?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(max = 160.dp)
            )
        }
        if (showChevron) {
            Spacer(modifier = Modifier.width(spacing.xs))
            Box(
                modifier = Modifier.width(20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(14.dp)
                )
            }
        }
    }
}
