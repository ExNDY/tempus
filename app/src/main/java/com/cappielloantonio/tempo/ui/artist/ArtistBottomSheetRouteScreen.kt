package com.cappielloantonio.tempo.ui.artist

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.navigation.Screen
import com.cappielloantonio.tempo.navigation.encodeNavRouteValue
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.AssetLinkUtil
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.requireActivity
import com.cappielloantonio.tempo.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

object ArtistBottomSheetRouteScreen : Screen.BottomSheetScreen {
    private const val ARTIST_ID = "artistId"

    override val screenName: String = "artistActions?$ARTIST_ID={$ARTIST_ID}"

    override val navArgs = listOf(
        navArgument(ARTIST_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
    )

    fun route(artist: ArtistID3): String = route(artist.id.orEmpty())

    internal fun route(artistId: String): String =
        "artistActions?$ARTIST_ID=${encodeNavRouteValue(artistId)}"

    @Composable
    override fun Content(navController: NavController, args: Bundle?, onClose: suspend () -> Unit) {
        val artistId = args?.getString(ARTIST_ID).orEmpty()
        val activity = LocalContext.current.requireActivity<MainActivity>()
        val homeViewModel = ViewModelProvider(activity)[HomeViewModel::class.java]
        val scope = rememberCoroutineScope()

        TempusTheme {
            ArtistBottomSheetRoute(
                artistId = artistId,
                onDismiss = { scope.launch { onClose() } },
                onNavigateToArtist = { resolvedArtist ->
                    scope.launch {
                        onClose()
                        resolvedArtist.id?.takeIf { it.isNotBlank() }?.let { targetArtistId ->
                            navController.navigate(ArtistPageRouteScreen.route(targetArtistId))
                        }
                    }
                },
                onShufflePlay = { songs ->
                    MediaManager.startQueue(activity.mediaBrowserListenableFuture, songs, 0)
                    activity.setBottomSheetInPeek(true)
                },
                onStartInstantMix = { songs ->
                    MediaManager.startQueue(activity.mediaBrowserListenableFuture, songs, 0)
                    activity.setBottomSheetInPeek(true)
                },
                onOpenAssetLink = { assetLink, collapsePlayer ->
                    activity.openAssetLink(assetLink, collapsePlayer)
                },
                onCopyAssetLink = { assetLink ->
                    AssetLinkUtil.copyToClipboard(activity, assetLink)
                    android.widget.Toast.makeText(
                        activity,
                        activity.getString(R.string.asset_link_copied_toast, assetLink.id),
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                },
                onRefreshShares = {
                    homeViewModel.refreshShares()
                },
            )
        }
    }
}
