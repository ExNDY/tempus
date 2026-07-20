package com.cappielloantonio.tempo.navigation

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavController

sealed interface Screen {

    val screenName: String

    val navArgs: List<NamedNavArgument> get() = emptyList()

    interface DefaultScreen : Screen {
        fun bottomMenuConfig(): BottomMenuConfig

        @Composable
        fun Content(navController: NavController, args: Bundle?)
    }

    interface BottomSheetScreen : Screen {
        @Composable
        fun Content(navController: NavController, args: Bundle?, onClose: suspend () -> Unit)
    }

    interface DialogScreen : Screen {
        @Composable
        fun Content(navController: NavController, args: Bundle?, onClose: () -> Unit)
    }
}
