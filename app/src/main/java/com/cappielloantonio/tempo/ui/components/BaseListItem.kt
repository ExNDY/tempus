package com.cappielloantonio.tempo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cappielloantonio.tempo.ui.theme.TempusTheme

@Composable
fun BaseListItem(
    title: String,
    subtitle: String? = null,
    imagePainter: Painter,
    onClick: () -> Unit,
    trailingContent: @Composable (RowScope.() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(MaterialTheme.shapes.small)
        ) {
            androidx.compose.foundation.Image(
                painter = imagePainter,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        if (trailingContent != null) {
            trailingContent()
        }
    }
}

@Composable
fun SongListItem(
    title: String,
    artist: String,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onMoreClick: () -> Unit,
    onClick: () -> Unit,
    imagePainter: Painter = TempusTheme.painters.placeholderSong
) {
    BaseListItem(
        title = title,
        subtitle = artist,
        imagePainter = imagePainter,
        onClick = onClick,
        trailingContent = {
            androidx.compose.material3.Icon(
                painter = if (isFavorite) TempusTheme.painters.favorite else TempusTheme.painters.favoriteOutlined,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onFavoriteClick),
                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            androidx.compose.material3.Icon(
                painter = TempusTheme.painters.moreVert,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onMoreClick),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
fun AlbumListItem(
    title: String,
    artist: String,
    onClick: () -> Unit,
    imagePainter: Painter = TempusTheme.painters.placeholderAlbum
) {
    BaseListItem(
        title = title,
        subtitle = artist,
        imagePainter = imagePainter,
        onClick = onClick
    )
}

@Composable
fun ArtistListItem(
    name: String,
    albumCount: Int? = null,
    onClick: () -> Unit,
    imagePainter: Painter = TempusTheme.painters.placeholderArtist
) {
    BaseListItem(
        title = name,
        subtitle = albumCount?.let { "$it albums" },
        imagePainter = imagePainter,
        onClick = onClick
    )
}
