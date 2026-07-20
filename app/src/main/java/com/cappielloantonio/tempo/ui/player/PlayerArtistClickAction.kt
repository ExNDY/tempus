package com.cappielloantonio.tempo.ui.player

import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.resolvedArtists
import com.cappielloantonio.tempo.viewmodel.PlayerUiState

internal sealed interface PlayerArtistClickAction {
    data object None : PlayerArtistClickAction
    data class OpenArtist(val artistId: String) : PlayerArtistClickAction
    data class ChooseArtist(val artists: List<ArtistID3>) : PlayerArtistClickAction
}

internal fun playerArtistClickAction(uiState: PlayerUiState): PlayerArtistClickAction {
    val artists = uiState.currentSong?.resolvedArtists(uiState.currentArtist).orEmpty()

    return when (artists.size) {
        0 -> PlayerArtistClickAction.None
        1 -> PlayerArtistClickAction.OpenArtist(artists.first().id.orEmpty())
        else -> PlayerArtistClickAction.ChooseArtist(artists)
    }
}
