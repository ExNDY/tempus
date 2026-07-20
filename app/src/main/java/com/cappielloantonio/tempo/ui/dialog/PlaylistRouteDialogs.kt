package com.cappielloantonio.tempo.ui.dialog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.repository.PlaylistRepository
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.util.requireActivity
import com.cappielloantonio.tempo.viewmodel.PlaylistChooserViewModel
import com.cappielloantonio.tempo.viewmodel.PlaylistEditorViewModel

@Composable
fun PlaylistChooserRouteDialog(
    tracks: ArrayList<Child>,
    onDismiss: () -> Unit,
    onPlaylistsChanged: () -> Unit,
) {
    val activity = LocalContext.current.requireActivity<MainActivity>()
    val viewModel = ViewModelProvider(activity)[PlaylistChooserViewModel::class.java]
    var showEditor by remember { mutableStateOf(false) }
    var isPublic by remember { mutableStateOf(viewModel.isPlaylistPublic) }
    val playlists by viewModel.getPlaylistList(activity).observeAsState(emptyList())

    LaunchedEffect(tracks) {
        viewModel.setSongsToAdd(tracks)
    }

    if (showEditor) {
        PlaylistEditorRouteDialog(
            tracks = tracks,
            playlist = null,
            onDismiss = onDismiss,
            onPlaylistsChanged = onPlaylistsChanged,
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playlist_chooser_dialog_title)) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isPublic,
                        onCheckedChange = {
                            isPublic = it
                            viewModel.setIsPlaylistPublic(it)
                        },
                    )
                    Text(stringResource(R.string.playlist_chooser_dialog_visibility_switch_label))
                }
                if (playlists.isEmpty()) {
                    Text(stringResource(R.string.playlist_chooser_dialog_empty))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(playlists) { playlist ->
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    if (tracks.isEmpty()) {
                                        Toast.makeText(
                                            activity,
                                            R.string.playlist_chooser_dialog_toast_add_failure,
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        return@TextButton
                                    }
                                    viewModel.addSongsToPlaylist(playlist.id) {
                                        activity.runOnUiThread {
                                            onPlaylistsChanged()
                                            onDismiss()
                                        }
                                    }
                                },
                            ) {
                                Text(playlist.name.orEmpty())
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { showEditor = true }) {
                Text(stringResource(R.string.playlist_chooser_dialog_create_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.playlist_chooser_dialog_cancel_button))
            }
        },
    )
}

@Composable
fun PlaylistEditorRouteDialog(
    tracks: ArrayList<Child>? = null,
    playlist: Playlist? = null,
    onDismiss: () -> Unit,
    onPlaylistsChanged: () -> Unit,
) {
    val activity = LocalContext.current.requireActivity<MainActivity>()
    val viewModel = ViewModelProvider(activity)[PlaylistEditorViewModel::class.java]
    var playlistName by remember(playlist) { mutableStateOf(playlist?.name.orEmpty()) }
    var hasNameError by remember { mutableStateOf(false) }

    LaunchedEffect(tracks, playlist) {
        if (tracks != null) {
            viewModel.setSongsToAdd(tracks)
            viewModel.setPlaylistToEdit(null)
        } else {
            viewModel.setSongsToAdd(null)
            viewModel.setPlaylistToEdit(playlist)
        }
    }

    val songs by viewModel.getPlaylistSongLiveList().observeAsState(emptyList())

    fun handleSuccess(messageRes: Int) {
        activity.runOnUiThread {
            Toast.makeText(activity, messageRes, Toast.LENGTH_SHORT).show()
            onPlaylistsChanged()
            onDismiss()
        }
    }

    fun handleFailure(messageRes: Int) {
        activity.runOnUiThread {
            Toast.makeText(activity, messageRes, Toast.LENGTH_SHORT).show()
        }
    }

    val callback = remember {
        object : PlaylistRepository.PlaylistActionCallback {
            override fun onSuccess() = handleSuccess(R.string.playlist_editor_dialog_action_save_success)
            override fun onFailure() = handleFailure(R.string.playlist_editor_dialog_action_save_failure)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playlist_editor_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = {
                        playlistName = it
                        hasNameError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = hasNameError,
                    label = { Text(stringResource(R.string.playlist_editor_dialog_hint_name)) },
                    supportingText = {
                        if (hasNameError) {
                            Text(stringResource(R.string.error_required))
                        }
                    },
                )
                if (playlist != null && Preferences.isSharingEnabled()) {
                    TextButton(
                        onClick = {
                            viewModel.sharePlaylist().observe(activity) { sharedPlaylist ->
                                sharedPlaylist?.url?.let { url ->
                                    val clipboardManager = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboardManager.setPrimaryClip(
                                        ClipData.newPlainText(activity.getString(R.string.app_name), url),
                                    )
                                }
                            }
                        },
                    ) {
                        Text(stringResource(R.string.album_bottom_sheet_share))
                    }
                }
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(songs.orEmpty()) { song ->
                        Text(
                            text = song.title.orEmpty(),
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val name = playlistName.trim()
                    if (name.isEmpty()) {
                        hasNameError = true
                        return@TextButton
                    }
                    if (tracks != null) {
                        viewModel.createPlaylist(name, callback)
                    } else if (playlist != null) {
                        viewModel.updatePlaylist(name, callback)
                    }
                },
            ) {
                Text(stringResource(R.string.playlist_editor_dialog_positive_button))
            }
        },
        dismissButton = {
            Row {
                if (playlist != null) {
                    TextButton(
                        onClick = {
                            viewModel.deletePlaylist(
                                object : PlaylistRepository.PlaylistActionCallback {
                                    override fun onSuccess() = handleSuccess(R.string.playlist_editor_dialog_action_delete_success)
                                    override fun onFailure() = handleFailure(R.string.playlist_editor_dialog_action_delete_failure)
                                },
                            )
                        },
                    ) {
                        Text(stringResource(R.string.playlist_editor_dialog_neutral_button))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.playlist_editor_dialog_negative_button))
                }
            }
        },
    )
}
