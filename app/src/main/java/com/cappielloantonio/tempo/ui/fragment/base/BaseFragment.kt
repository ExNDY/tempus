package com.cappielloantonio.tempo.ui.fragment.base

import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.cappielloantonio.tempo.navigation.BottomSheetController
import com.cappielloantonio.tempo.navigation.NavigationController
import com.cappielloantonio.tempo.ui.activity.MainActivity

@UnstableApi
abstract class BaseFragment : Fragment() {

    protected val navigationController: NavigationController
        get() = (requireActivity() as MainActivity).navigationController

    protected val navController: NavController
        get() = navigationController.navController

    protected val bottomSheetController: BottomSheetController
        get() = (requireActivity() as MainActivity).bottomSheetController
}
