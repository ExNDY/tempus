package com.cappielloantonio.tempo.ui.artist
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getArtistPageViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.navigation.BottomMenuConfig
import com.cappielloantonio.tempo.navigation.DefaultScreenNameExtension.defaultScreenNameWithParams
import com.cappielloantonio.tempo.navigation.Screen
import com.cappielloantonio.tempo.navigation.ScreenNameExtension.screenNameWithParams
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.util.requireActivity
import com.cappielloantonio.tempo.ui.album.AlbumPageRouteScreen
import com.cappielloantonio.tempo.ui.song.SongBottomSheetRouteScreen
import com.cappielloantonio.tempo.ui.song.SongListPageRouteScreen
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.viewmodel.SongListPageArgs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.ArrayList
object ArtistPageRouteScreen : Screen.DefaultScreen {
    private const val ARTIST_ID = "artistId"
    override val navArgs = listOf(
        navArgument(ARTIST_ID) {
            type = NavType.StringType
            nullable = false
        },
    )
    override val screenName: String = defaultScreenNameWithParams(ARTIST_ID)
    override fun bottomMenuConfig(): BottomMenuConfig = BottomMenuConfig.Hidden
    fun route(artistId: String): String {
        return screenNameWithParams(Uri.encode(artistId))
    }
    @Composable
    override fun Content(navController: NavController, args: Bundle?) {
        val activity = LocalContext.current.requireActivity<MainActivity>()
        val context = LocalContext.current
        val artistId = args?.getString(ARTIST_ID)
            ?.takeIf { it.isNotBlank() }
            ?: error("Missing artistId for ArtistPageRouteScreen")
        val artistPageViewModel = getViewModel { getArtistPageViewModel().apply { onStart(artistId) } }
        val uiState by artistPageViewModel.uiState.collectAsState()
        val coroutineScope = rememberCoroutineScope()
        var isBiographyVisible by remember { mutableStateOf(Preferences.getArtistDisplayBiography()) }
        ArtistPageScreen(
            uiState = uiState,
            currentSongId = uiState.currentSongId,
            isPlaying = uiState.isPlaying,
            isBiographyVisible = isBiographyVisible,
            onFavoriteClick = artistPageViewModel::setFavorite,
            onToggleBiographyVisibility = {
                isBiographyVisible = !isBiographyVisible
                Preferences.setArtistDisplayBiography(isBiographyVisible)
            },
            onBiographyMoreClick = uiState.artistInfo?.lastFmUrl?.let { url ->
                { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            },
            onShuffleClick = {
                val shuffled = artistPageViewModel.getShuffledArtistSongs()
                if (shuffled.isNotEmpty()) {
                    MediaManager.startQueue(activity.mediaBrowserListenableFuture, ArrayList(shuffled), 0)
                    activity.setBottomSheetInPeek(true)
                }
            },
            onInstantMixClick = {
                Toast.makeText(context, R.string.bottom_sheet_generating_instant_mix, Toast.LENGTH_SHORT).show()
                coroutineScope.launch {
                    val media = artistPageViewModel.getArtistInstantMix().first().toMutableList()
                    if (media.isNotEmpty()) {
                        MusicUtil.ratingFilter(media)
                        MediaManager.startQueue(activity.mediaBrowserListenableFuture, media, 0)
                        activity.setBottomSheetInPeek(true)
                    }
                }
            },
            onSeeAllTopSongsClick = {
                val currentArtist = uiState.artist ?: return@ArtistPageScreen
                navController.navigate(
                    SongListPageRouteScreen.route(
                        SongListPageArgs(
                            type = Constants.MEDIA_BY_ARTIST,
                            artist = currentArtist,
                        ),
                    ),
                )
            },
            onAlbumClick = { album ->
                album.id?.takeIf { it.isNotBlank() }?.let { albumId ->
                    navController.navigate(AlbumPageRouteScreen.route(albumId))
                }
            },
            onSongClick = { index ->
                MediaManager.startQueue(activity.mediaBrowserListenableFuture, ArrayList(uiState.topSongs), index)
                activity.setBottomSheetInPeek(true)
            },
            onSongLongClick = { song ->
                navController.navigate(SongBottomSheetRouteScreen.route(song))
            },
            onSimilarArtistClick = { similarArtist ->
                similarArtist.id?.takeIf { it.isNotBlank() }?.let { similarArtistId ->
                    navController.navigate(route(similarArtistId))
                }
            },
            onNavigateBack = { navController.navigateUp() },
        )
    }
}
