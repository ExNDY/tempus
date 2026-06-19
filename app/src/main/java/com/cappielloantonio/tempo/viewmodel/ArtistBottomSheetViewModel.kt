package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LiveData
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.interfaces.StarCallback
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.FavoriteRepository
import com.cappielloantonio.tempo.repository.SongRepository
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.Constants

@UnstableApi
class ArtistBottomSheetViewModel(
    private val artistRepository: ArtistRepository,
    private val favoriteRepository: FavoriteRepository,
    private val songRepository: SongRepository,
) : androidx.lifecycle.ViewModel() {
    var artist: ArtistID3? = null

    fun setFavorite() {
        val currentArtist = artist ?: return
        favoriteRepository.star(null, null, currentArtist.id, object : StarCallback {
            override fun onSuccess() {}
            override fun onError() {}
        })
    }

    fun getArtistInstantMix(artist: ArtistID3): LiveData<List<Child>> {
        return songRepository.getInstantMix(artist.id ?: "", Constants.SeedType.ARTIST, 50)
    }
}
