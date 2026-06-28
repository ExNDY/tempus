package com.cappielloantonio.tempo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.cappielloantonio.tempo.glide.CustomGlideRequest

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
            modifier = modifier.background(Color.LightGray),
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
    
    AndroidView(
        factory = { ctx ->
            android.widget.ImageView(ctx).apply {
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            }
        },
        modifier = modifier,
        update = { view ->
            CustomGlideRequest.Builder
                .from(context, coverArtId, mapToGlideResourceType(imageType))
                .build()
                .into(view)
        }
    )
}

private fun mapToGlideResourceType(type: TempusImageType): CustomGlideRequest.ResourceType {
    return when (type) {
        TempusImageType.Song -> CustomGlideRequest.ResourceType.Song
        TempusImageType.Album -> CustomGlideRequest.ResourceType.Album
        TempusImageType.Artist -> CustomGlideRequest.ResourceType.Artist
        TempusImageType.Playlist -> CustomGlideRequest.ResourceType.Playlist
        TempusImageType.Podcast -> CustomGlideRequest.ResourceType.Podcast
        TempusImageType.Radio -> CustomGlideRequest.ResourceType.Radio
        else -> CustomGlideRequest.ResourceType.Unknown
    }
}
