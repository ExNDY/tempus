package com.cappielloantonio.tempo.ui.genre
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getGenreCatalogueViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.navigation.BottomMenuConfig
import com.cappielloantonio.tempo.navigation.DefaultScreenNameExtension.defaultScreenName
import com.cappielloantonio.tempo.navigation.Screen
import com.cappielloantonio.tempo.ui.filter.FilterRouteScreen
import com.cappielloantonio.tempo.ui.song.SongListPageRouteScreen
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.SongListPageArgs
object GenreCatalogueRouteScreen : Screen.DefaultScreen {
    override val screenName: String = defaultScreenName()
    override fun bottomMenuConfig(): BottomMenuConfig = BottomMenuConfig.Hidden
    @Composable
    override fun Content(navController: NavController, args: Bundle?) {
        val viewModel = getViewModel { getGenreCatalogueViewModel().apply { onStart() } }
        val uiState by viewModel.uiState.collectAsState()
        GenreCatalogueScreen(
            uiState = uiState,
            title = stringResource(R.string.genre_catalogue_title),
            onGenreClick = { genre ->
                navController.navigate(
                    SongListPageRouteScreen.route(
                        SongListPageArgs(
                            type = Constants.MEDIA_BY_GENRE,
                            genre = genre,
                        ),
                    ),
                )
            },
            onRefresh = viewModel::refresh,
            onOpenFilter = {
                navController.navigate(FilterRouteScreen.screenName)
            },
            onNavigateBack = { navController.navigateUp() },
        )
    }
}
