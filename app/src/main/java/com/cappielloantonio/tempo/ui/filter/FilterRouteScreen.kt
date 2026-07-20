package com.cappielloantonio.tempo.ui.filter

import android.os.Bundle
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getFilterViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.navigation.BottomMenuConfig
import com.cappielloantonio.tempo.navigation.DefaultScreenNameExtension.defaultScreenName
import com.cappielloantonio.tempo.navigation.Screen
import com.cappielloantonio.tempo.ui.song.SongListPageRouteScreen
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.SongListPageArgs

object FilterRouteScreen : Screen.DefaultScreen {
    override val screenName: String = defaultScreenName()
    override fun bottomMenuConfig(): BottomMenuConfig = BottomMenuConfig.Hidden

    @Composable
    override fun Content(navController: NavController, args: Bundle?) {
        val context = LocalContext.current
        val viewModel = getViewModel { getFilterViewModel().apply { onStart() } }
        val uiState by viewModel.uiState.collectAsState()
        FilterScreen(
            uiState = uiState,
            onNavigateBack = { navController.navigateUp() },
            onGenreToggle = { genre, isSelected ->
                if (isSelected) {
                    viewModel.removeFilter(genre.genre.orEmpty(), genre.genre.orEmpty())
                } else {
                    viewModel.addFilter(genre.genre.orEmpty(), genre.genre.orEmpty())
                }
            },
            onApplyClick = {
                if (uiState.selectedFilterIds.size <= 1) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.filter_info_selection),
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    navController.navigate(
                        SongListPageRouteScreen.route(
                            SongListPageArgs(
                                type = Constants.MEDIA_BY_GENRES,
                                filters = uiState.selectedFilterIds,
                                filterNames = uiState.selectedFilterNames,
                            ),
                        ),
                    )
                }
            },
        )
    }
}
