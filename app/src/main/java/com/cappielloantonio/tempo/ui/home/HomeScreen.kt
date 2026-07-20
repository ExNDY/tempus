package com.cappielloantonio.tempo.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.R

@Composable
fun HomeScreen(
    onSettingsClick: () -> Unit,
    onEditHomeClick: () -> Unit,
    musicTab: @Composable (topContent: @Composable () -> Unit) -> Unit,
) {
    PassiveCornerGlowBackground(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        musicTab {
            HomeToolbarContent(
                onSettingsClick = onSettingsClick,
                onEditHomeClick = onEditHomeClick,
            )
        }
    }
}

@Composable
private fun HomeToolbarContent(
    onSettingsClick: () -> Unit,
    onEditHomeClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(start = 20.dp, top = 4.dp, end = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = R.string.menu_home_label),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Row {
            IconButton(onClick = onEditHomeClick) {
                Icon(imageVector = Icons.Outlined.Edit, contentDescription = null)
            }
            IconButton(onClick = onSettingsClick) {
                Icon(imageVector = Icons.Outlined.Settings, contentDescription = null)
            }
        }
    }
}
