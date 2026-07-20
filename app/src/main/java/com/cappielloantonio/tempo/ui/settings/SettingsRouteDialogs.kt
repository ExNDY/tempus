package com.cappielloantonio.tempo.ui.settings

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.repository.DownloadRepository
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.ExternalAudioReader
import com.cappielloantonio.tempo.util.ExternalDownloadMetadataStore
import com.cappielloantonio.tempo.util.MappingUtil
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.util.requireActivity
import com.cappielloantonio.tempo.viewmodel.StarredAlbumsSyncViewModel
import com.cappielloantonio.tempo.viewmodel.StarredArtistsSyncViewModel
import com.cappielloantonio.tempo.viewmodel.StarredSyncViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun StreamingCacheStorageRouteDialog(
    onDismiss: () -> Unit,
    onSettingsChanged: () -> Unit,
) {
    fun chooseStorage(value: Int) {
        if (Preferences.getStreamingCacheStoragePreference() != value) {
            Preferences.setStreamingCacheStoragePreference(value)
            onSettingsChanged()
        }
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.streaming_cache_storage_dialog_title)) },
        text = {
            Column {
                TextButton(onClick = { chooseStorage(1) }) {
                    Text(stringResource(R.string.streaming_cache_storage_external_dialog_positive_button))
                }
                TextButton(onClick = { chooseStorage(0) }) {
                    Text(stringResource(R.string.streaming_cache_storage_internal_dialog_negative_button))
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
fun DownloadStorageRouteDialog(
    onDismiss: () -> Unit,
    onSettingsChanged: () -> Unit,
) {
    val context = LocalContext.current

    fun chooseStorage(value: Int) {
        if (Preferences.getDownloadStoragePreference() != value) {
            Preferences.setDownloadStoragePreference(value)
            DownloadUtil.getDownloadTracker(context).removeAll()
            onSettingsChanged()
        }
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.download_storage_dialog_title)) },
        text = {
            Column {
                TextButton(onClick = { chooseStorage(1) }) {
                    Text(stringResource(R.string.download_storage_external_dialog_positive_button))
                }
                TextButton(onClick = { chooseStorage(0) }) {
                    Text(stringResource(R.string.download_storage_internal_dialog_negative_button))
                }
                TextButton(onClick = { chooseStorage(2) }) {
                    Text(stringResource(R.string.download_storage_directory_dialog_neutral_button))
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
fun DeleteDownloadStorageRouteDialog(
    onDismiss: () -> Unit,
    onSettingsChanged: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_download_storage_dialog_title)) },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        deleteDownloadStorage(context)
                        onSettingsChanged()
                    }
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.delete_download_storage_dialog_positive_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.delete_download_storage_dialog_negative_button))
            }
        },
    )
}

@Composable
fun StarredTracksSyncRouteDialog(
    onDismiss: () -> Unit,
    onSettingsChanged: () -> Unit,
) {
    val activity = LocalContext.current.requireActivity<MainActivity>()
    val viewModel = ViewModelProvider(activity)[StarredSyncViewModel::class.java]
    StarredSyncDialogContent(
        title = stringResource(R.string.starred_sync_dialog_title),
        summary = stringResource(R.string.starred_sync_dialog_summary),
        onDownloadNow = {
            viewModel.getStarredTracks().observeOnce(activity) { songs ->
                if (songs != null && Preferences.getDownloadDirectoryUri() == null) {
                    DownloadUtil.getDownloadTracker(activity).download(
                        MappingUtil.mapDownloads(songs),
                        songs.map { Download(it) },
                    )
                }
                onSettingsChanged()
                onDismiss()
            }
        },
        onEnableOnly = {
            Preferences.setStarredSyncEnabled(true)
            onSettingsChanged()
            onDismiss()
        },
        onDisable = {
            Preferences.setStarredSyncEnabled(false)
            onSettingsChanged()
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}

@Composable
fun StarredAlbumsSyncRouteDialog(
    onDismiss: () -> Unit,
    onSettingsChanged: () -> Unit,
) {
    val activity = LocalContext.current.requireActivity<MainActivity>()
    val viewModel = ViewModelProvider(activity)[StarredAlbumsSyncViewModel::class.java]
    StarredSyncDialogContent(
        title = stringResource(R.string.starred_album_sync_dialog_title),
        summary = stringResource(R.string.starred_sync_dialog_summary),
        onDownloadNow = {
            viewModel.getStarredAlbumSongs(activity).observeOnce(activity) { songs ->
                if (!songs.isNullOrEmpty()) {
                    DownloadUtil.getDownloadTracker(activity).download(
                        MappingUtil.mapDownloads(songs),
                        songs.map { Download(it) },
                    )
                }
                onSettingsChanged()
                onDismiss()
            }
        },
        onEnableOnly = {
            Preferences.setStarredAlbumsSyncEnabled(true)
            onSettingsChanged()
            onDismiss()
        },
        onDisable = {
            Preferences.setStarredAlbumsSyncEnabled(false)
            onSettingsChanged()
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}

@Composable
fun StarredArtistsSyncRouteDialog(
    onDismiss: () -> Unit,
    onSettingsChanged: () -> Unit,
) {
    val activity = LocalContext.current.requireActivity<MainActivity>()
    val viewModel = ViewModelProvider(activity)[StarredArtistsSyncViewModel::class.java]
    StarredSyncDialogContent(
        title = stringResource(R.string.starred_artist_sync_dialog_title),
        summary = stringResource(R.string.starred_sync_dialog_summary),
        onDownloadNow = {
            viewModel.getStarredArtistSongs(activity).observeOnce(activity) { songs ->
                if (!songs.isNullOrEmpty()) {
                    DownloadUtil.getDownloadTracker(activity).download(
                        MappingUtil.mapDownloads(songs),
                        songs.map { Download(it) },
                    )
                }
                onSettingsChanged()
                onDismiss()
            }
        },
        onEnableOnly = {
            Preferences.setStarredArtistsSyncEnabled(true)
            onSettingsChanged()
            onDismiss()
        },
        onDisable = {
            Preferences.setStarredArtistsSyncEnabled(false)
            onSettingsChanged()
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}

@Composable
private fun StarredSyncDialogContent(
    title: String,
    summary: String,
    onDownloadNow: () -> Unit,
    onEnableOnly: () -> Unit,
    onDisable: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(summary) },
        confirmButton = {
            TextButton(onClick = onDownloadNow) {
                Text(stringResource(R.string.starred_sync_dialog_positive_button))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEnableOnly) {
                    Text(stringResource(R.string.starred_sync_dialog_neutral_button))
                }
                TextButton(onClick = onDisable) {
                    Text(stringResource(R.string.starred_sync_dialog_negative_button))
                }
            }
        },
    )
}

private suspend fun deleteDownloadStorage(context: Context) {
    withContext(Dispatchers.IO) {
        if (Preferences.getDownloadDirectoryUri() == null) {
            DownloadUtil.getDownloadTracker(context).removeAll()
        }
        val uriString = Preferences.getDownloadDirectoryUri()
        if (uriString != null) {
            val directory = DocumentFile.fromTreeUri(context, Uri.parse(uriString))
            if (directory != null && directory.canWrite()) {
                val trackedDownloads = DownloadRepository().getAllDownloads()
                val trackedFilesMap = trackedDownloads
                    .filter { it.downloadUri != null }
                    .associateBy { it.downloadUri!! }
                for (file in directory.listFiles()) {
                    val trackedDownload = trackedFilesMap[file.uri.toString()]
                    if (trackedDownload != null) {
                        file.delete()
                        DownloadRepository().delete(trackedDownload.id)
                    }
                }
            }
            ExternalAudioReader.refreshCache()
            ExternalDownloadMetadataStore.clear()
        }
    }
}

private fun <T> LiveData<T>.observeOnce(owner: LifecycleOwner, onValue: (T) -> Unit) {
    val liveData = this
    val observer = object : Observer<T> {
        override fun onChanged(value: T) {
            liveData.removeObserver(this)
            onValue(value)
        }
    }
    observe(owner, observer)
}
