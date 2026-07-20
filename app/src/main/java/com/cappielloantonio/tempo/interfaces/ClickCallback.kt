package com.cappielloantonio.tempo.interfaces

import android.os.Bundle
import androidx.annotation.Keep

@Keep
interface ClickCallback {
    fun onAlbumClick(bundle: Bundle) {}
    fun onArtistClick(bundle: Bundle) {}
    fun onPlaylistClick(bundle: Bundle) {}
}
