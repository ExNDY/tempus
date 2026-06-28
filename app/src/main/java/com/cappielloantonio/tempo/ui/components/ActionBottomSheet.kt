package com.cappielloantonio.tempo.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class ActionItemUiModel(
    val icon: ImageVector? = null,
    val iconRes: Int? = null,
    val label: String,
    val onClick: () -> Unit,
    val isVisible: Boolean = true,
    val isDestructive: Boolean = false
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActionBottomSheetContent(
    title: String,
    coverArtId: String?,
    imageType: TempusImageType,
    actions: List<ActionItemUiModel>,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingContent: @Composable (RowScope.() -> Unit)? = null,
    headerExtraContent: @Composable (() -> Unit)? = null,
    onCoverClick: (() -> Unit)? = null,
    onCoverLongClick: (() -> Unit)? = null,
    onTitleClick: (() -> Unit)? = null,
    onTitleLongClick: (() -> Unit)? = null,
    onSubtitleClick: (() -> Unit)? = null,
    onSubtitleLongClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TempusImage(
                coverArtId = coverArtId,
                imageType = imageType,
                modifier = Modifier
                    .size(64.dp)
                    .clip(MaterialTheme.shapes.small)
                    .then(
                        if (onCoverClick != null || onCoverLongClick != null) {
                            Modifier.combinedClickable(
                                onClick = onCoverClick ?: {},
                                onLongClick = onCoverLongClick
                            )
                        } else {
                            Modifier
                        }
                    )
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (onTitleClick != null || onTitleLongClick != null) {
                        Modifier.combinedClickable(
                            onClick = onTitleClick ?: {},
                            onLongClick = onTitleLongClick
                        )
                    } else {
                        Modifier
                    }
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (onSubtitleClick != null || onSubtitleLongClick != null) {
                            Modifier.combinedClickable(
                                onClick = onSubtitleClick ?: {},
                                onLongClick = onSubtitleLongClick
                            )
                        } else {
                            Modifier
                        }
                    )
                }
            }
            if (trailingContent != null) {
                trailingContent()
            }
        }

        if (headerExtraContent != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                headerExtraContent()
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

        // Actions
        LazyColumn {
            items(actions.filter { it.isVisible }) { action ->
                ListItem(
                    modifier = Modifier.clickable(onClick = action.onClick),
                    headlineContent = {
                        Text(
                            text = action.label,
                            color = if (action.isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    leadingContent = {
                        val tint = if (action.isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        if (action.icon != null) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = null,
                                tint = tint
                            )
                        } else if (action.iconRes != null) {
                            Icon(
                                painter = painterResource(id = action.iconRes),
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}
