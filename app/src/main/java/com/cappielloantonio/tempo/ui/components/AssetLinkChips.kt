package com.cappielloantonio.tempo.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.util.AssetLinkUtil

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssetLinkChips(
    songId: String?,
    albumId: String?,
    artistId: String?,
    onChipClick: (String, String) -> Unit,
    onChipLongClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!songId.isNullOrEmpty()) {
            AssetLinkChip(
                label = stringResource(R.string.asset_link_label_song),
                id = songId,
                onClick = { onChipClick(AssetLinkUtil.TYPE_SONG, songId) },
                onLongClick = { onChipLongClick(AssetLinkUtil.TYPE_SONG, songId) }
            )
        }
        if (!albumId.isNullOrEmpty()) {
            AssetLinkChip(
                label = stringResource(R.string.asset_link_label_album),
                id = albumId,
                onClick = { onChipClick(AssetLinkUtil.TYPE_ALBUM, albumId) },
                onLongClick = { onChipLongClick(AssetLinkUtil.TYPE_ALBUM, albumId) }
            )
        }
        if (!artistId.isNullOrEmpty()) {
            AssetLinkChip(
                label = stringResource(R.string.asset_link_label_artist),
                id = artistId,
                onClick = { onChipClick(AssetLinkUtil.TYPE_ARTIST, artistId) },
                onLongClick = { onChipLongClick(AssetLinkUtil.TYPE_ARTIST, artistId) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AssetLinkChip(
    label: String,
    id: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(text = stringResource(R.string.asset_link_chip_text, label, id))
        },
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    )
}
