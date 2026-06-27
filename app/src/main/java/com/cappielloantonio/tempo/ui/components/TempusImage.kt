package com.cappielloantonio.tempo.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
