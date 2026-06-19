package com.cappielloantonio.tempo.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.cappielloantonio.tempo.R

@Immutable
data class TempusIcons(
    @DrawableRes val favorite: Int = R.drawable.ic_favorite,
    @DrawableRes val favoriteOutlined: Int = R.drawable.ic_favorites_outlined,
    @DrawableRes val moreVert: Int = R.drawable.ic_more_vert,
    @DrawableRes val play: Int = R.drawable.ic_play,
    @DrawableRes val pause: Int = R.drawable.ic_pause,
    @DrawableRes val skipNext: Int = R.drawable.ic_skip_next,
    @DrawableRes val skipPrevious: Int = R.drawable.ic_skip_previous,
    @DrawableRes val shuffle: Int = R.drawable.ic_shuffle,
    @DrawableRes val repeat: Int = R.drawable.ic_repeat,
    @DrawableRes val download: Int = R.drawable.ic_download,
    @DrawableRes val search: Int = R.drawable.ic_search,
    @DrawableRes val settings: Int = R.drawable.ic_settings,
    @DrawableRes val placeholderSong: Int = R.drawable.ic_placeholder_song,
    @DrawableRes val placeholderAlbum: Int = R.drawable.ic_placeholder_album,
    @DrawableRes val placeholderArtist: Int = R.drawable.ic_placeholder_artist,
    @DrawableRes val placeholderPlaylist: Int = R.drawable.ic_placeholder_playlist,
    @DrawableRes val placeholderRadio: Int = R.drawable.ic_placeholder_radio,
    @DrawableRes val placeholderPodcast: Int = R.drawable.ic_placeholder_podcast
)

/**
 * Optimized painters that are shared across the application.
 */
@Immutable
data class TempusPainters(
    val favorite: Painter,
    val favoriteOutlined: Painter,
    val moreVert: Painter,
    val placeholderSong: Painter,
    val placeholderAlbum: Painter,
    val placeholderArtist: Painter
)

val LocalTempusIcons = staticCompositionLocalOf { TempusIcons() }
val LocalTempusPainters = staticCompositionLocalOf<TempusPainters> { error("No painters provided") }
