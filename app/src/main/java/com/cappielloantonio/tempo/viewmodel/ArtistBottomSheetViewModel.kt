package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.interfaces.StarCallback
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.FavoriteRepository
import com.cappielloantonio.tempo.repository.SongRepository
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.util.Constants

@UnstableApi
class ArtistBottomSheetViewModel(application: Application) : AndroidViewModel(application) {
    private val artistRepository = ArtistRepository()
    private val favoriteRepository = FavoriteRepository()
    private val songRepository = SongRepository()
    var artist: ArtistID3? = null

    fun setFavorite(context: Context) {
        val currentArtist = artist ?: return
        favoriteRepository.star(null, null, currentArtist.id, object : StarCallback {
            override fun onSuccess() {}
            override fun onError() {}
        })
    }

    fun getArtistInstantMix(activity: MainActivity, artist: ArtistID3): LiveData<List<Child>> {
        return songRepository.getInstantMix(artist.id ?: "", Constants.SeedType.ARTIST, 50)
    }
}
