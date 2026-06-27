package com.cappielloantonio.tempo.ui.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.ui.activity.CrashActivity
import com.cappielloantonio.tempo.ui.crash.CrashExportScreen
import com.cappielloantonio.tempo.ui.theme.TempusTheme

@UnstableApi
class CrashExportFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as CrashActivity
        val stackTrace = activity.stackTrace ?: ""

        return ComposeView(requireContext()).apply {
            setContent {
                TempusTheme {
                    CrashExportScreen(
                        onCopyClick = {
                            val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(getString(R.string.ca_export_clipboard_label), stackTrace)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(activity, getString(R.string.ca_export_toast_log_copied_to_clipboard), Toast.LENGTH_SHORT).show()
                        },
                        onShareClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, stackTrace)
                            }
                            startActivity(Intent.createChooser(intent, getString(R.string.ca_export_button_share)))
                        }
                    )
                }
            }
        }
    }
}
