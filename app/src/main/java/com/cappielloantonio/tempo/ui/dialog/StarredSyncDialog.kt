package com.cappielloantonio.tempo.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.MappingUtil
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.viewmodel.StarredSyncViewModel
import java.util.stream.Collectors

@UnstableApi
class StarredSyncDialog @JvmOverloads constructor(
    private val onCancel: Runnable? = null,
    private val onResult: Runnable? = null
) : DialogFragment() {

    private lateinit var viewModel: StarredSyncViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(requireActivity())[StarredSyncViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                TempusTheme {
                    AlertDialog(
                        onDismissRequest = { dismiss() },
                        title = { Text(stringResource(R.string.starred_sync_dialog_title)) },
                        text = { Text(stringResource(R.string.starred_sync_dialog_summary)) },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.getStarredTracks().observe(viewLifecycleOwner) { songs ->
                                    if (songs != null && Preferences.getDownloadDirectoryUri() == null) {
                                        DownloadUtil.getDownloadTracker(requireContext()).download(
                                            MappingUtil.mapDownloads(songs),
                                            songs.map { Download(it) }
                                        )
                                    }
                                    onResult?.run()
                                    dismiss()
                                }
                            }) {
                                Text(stringResource(R.string.starred_sync_dialog_positive_button))
                            }
                        },
                        dismissButton = {
                            Row {
                                TextButton(onClick = {
                                    Preferences.setStarredSyncEnabled(true)
                                    onResult?.run()
                                    dismiss()
                                }) {
                                    Text(stringResource(R.string.starred_sync_dialog_neutral_button))
                                }
                                TextButton(onClick = {
                                    Preferences.setStarredSyncEnabled(false)
                                    onCancel?.run()
                                    onResult?.run()
                                    dismiss()
                                }) {
                                    Text(stringResource(R.string.starred_sync_dialog_negative_button))
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
