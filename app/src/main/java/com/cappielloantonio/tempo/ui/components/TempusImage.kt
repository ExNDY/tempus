package com.cappielloantonio.tempo.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import com.cappielloantonio.tempo.image.CoilImageRequest

@Composable
fun TempusImage(
    coverArtId: String?,
    imageType: TempusImageType,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null
) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.DarkGray
            )
        }
        return
    }

    val context = LocalContext.current
    val resourceType = mapToCoilResourceType(imageType)
    val model = remember(context, coverArtId, imageType) {
        CoilImageRequest.buildImageRequest(
            context = context,
            item = coverArtId,
            type = resourceType
        )
    }
    val placeholder = painterResource(id = imageType.placeholderRes)

    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        placeholder = placeholder,
        error = placeholder,
        fallback = placeholder,
        contentScale = contentScale,
        modifier = modifier,
    )
}

private fun mapToCoilResourceType(type: TempusImageType): CoilImageRequest.ResourceType {
    return when (type) {
        TempusImageType.Song -> CoilImageRequest.ResourceType.Song
        TempusImageType.Album -> CoilImageRequest.ResourceType.Album
        TempusImageType.Artist -> CoilImageRequest.ResourceType.Artist
        TempusImageType.Folder -> CoilImageRequest.ResourceType.Folder
        TempusImageType.Directory -> CoilImageRequest.ResourceType.Directory
        TempusImageType.Playlist -> CoilImageRequest.ResourceType.Playlist
        else -> CoilImageRequest.ResourceType.Unknown
    }
}
