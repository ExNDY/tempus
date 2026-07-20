package com.cappielloantonio.tempo.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.cappielloantonio.tempo.R

@Immutable
data class TempusIcons(
    @param:DrawableRes val favorite: Int = R.drawable.ic_favorite,
    @param:DrawableRes val favoriteOutlined: Int = R.drawable.ic_favorites_outlined,
    @param:DrawableRes val moreVert: Int = R.drawable.ic_more_vert,
    @param:DrawableRes val play: Int = R.drawable.ic_play,
    @param:DrawableRes val pause: Int = R.drawable.ic_pause,
    @param:DrawableRes val skipNext: Int = R.drawable.ic_skip_next,
    @param:DrawableRes val skipPrevious: Int = R.drawable.ic_skip_previous,
    @param:DrawableRes val shuffle: Int = R.drawable.ic_shuffle,
    @param:DrawableRes val repeat: Int = R.drawable.ic_repeat,
    @param:DrawableRes val download: Int = R.drawable.ic_download,
    @param:DrawableRes val search: Int = R.drawable.ic_search,
    @param:DrawableRes val settings: Int = R.drawable.ic_settings,
    @param:DrawableRes val placeholderSong: Int = R.drawable.ic_placeholder_song,
    @param:DrawableRes val placeholderAlbum: Int = R.drawable.ic_placeholder_album,
    @param:DrawableRes val placeholderArtist: Int = R.drawable.ic_placeholder_artist,
    @param:DrawableRes val placeholderPlaylist: Int = R.drawable.ic_placeholder_playlist
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
