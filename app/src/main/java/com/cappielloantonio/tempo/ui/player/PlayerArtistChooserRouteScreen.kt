package com.cappielloantonio.tempo.ui.player

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.cappielloantonio.tempo.navigation.Screen
import com.cappielloantonio.tempo.navigation.encodeNavRouteValue
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.ui.artist.ArtistPageRouteScreen
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import kotlinx.coroutines.launch

object PlayerArtistChooserRouteScreen : Screen.BottomSheetScreen {
    private const val TOKEN = "token"

    override val screenName: String = "playerArtistChooser/{$TOKEN}"

    override val navArgs = listOf(
        navArgument(TOKEN) {
            type = NavType.StringType
            nullable = false
        },
    )

    fun route(artists: List<ArtistID3>): String {
        val token = PlayerArtistChooserRouteArgsStore.put(artists)
        return "playerArtistChooser/${encodeNavRouteValue(token)}"
    }

    @Composable
    override fun Content(navController: NavController, args: Bundle?, onClose: suspend () -> Unit) {
        val token = args?.getString(TOKEN)
        val artists = remember(token) {
            PlayerArtistChooserRouteArgsStore.get(token)
        }
        DisposableEffect(token) {
            onDispose {
                PlayerArtistChooserRouteArgsStore.remove(token)
            }
        }
        val scope = rememberCoroutineScope()

        TempusTheme {
            PlayerArtistChooserScreen(
                artists = artists,
                onArtistClick = { artist ->
                    val artistId = artist.id?.takeIf { it.isNotBlank() } ?: return@PlayerArtistChooserScreen
                    scope.launch {
                        onClose()
                        navController.navigate(ArtistPageRouteScreen.route(artistId))
                    }
                },
            )
        }
    }
}
