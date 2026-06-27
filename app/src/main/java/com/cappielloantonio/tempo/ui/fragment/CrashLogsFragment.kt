package com.cappielloantonio.tempo.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.ui.activity.CrashActivity
import com.cappielloantonio.tempo.ui.crash.CrashLogsScreen
import com.cappielloantonio.tempo.ui.theme.TempusTheme

@UnstableApi
class CrashLogsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as CrashActivity
        val stackTrace = activity.stackTrace ?: ""

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    CrashLogsScreen(stackTrace = stackTrace)
                }
            }
        }
    }
}
