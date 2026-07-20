package com.cappielloantonio.tempo.ui.dialog

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaMetadata
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.util.AssetLinkUtil
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.util.SleepTimerManager
import com.cappielloantonio.tempo.util.requireActivity
import com.cappielloantonio.tempo.viewmodel.RatingViewModel
import kotlin.math.abs

@Composable
fun PlaybackSpeedRouteDialog(
    onDismiss: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
) {
    val currentSpeed = Preferences.getPlaybackSpeed()
    var selectedSpeed by remember(currentSpeed) {
        mutableFloatStateOf(SPEED_VALUES.firstOrNull { abs(it - currentSpeed) < 0.01f } ?: 1.0f)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playback_speed_dialog_title)) },
        text = {
            Column {
                SPEED_VALUES.forEachIndexed { index, speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedSpeed == speed,
                                onClick = {
                                    selectedSpeed = speed
                                    Preferences.setPlaybackSpeed(speed)
                                    onSpeedSelected(speed)
                                    onDismiss()
                                },
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selectedSpeed == speed, onClick = null)
                        Text(SPEED_LABELS[index], modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.playback_speed_dialog_negative_button))
            }
        },
    )
}

@Composable
fun SleepTimerRouteDialog(onDismiss: () -> Unit) {
    var showCustomInput by remember { mutableStateOf(false) }
    var customMinutes by remember { mutableStateOf("") }
    val manager = SleepTimerManager.getInstance()

    if (showCustomInput) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.sleep_timer_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = customMinutes,
                    onValueChange = { customMinutes = it.filter(Char::isDigit) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text(stringResource(R.string.sleep_timer_custom_hint)) },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = customMinutes.toIntOrNull()?.let { it > 0 } == true,
                    onClick = {
                        manager.startTimer(customMinutes.toInt())
                        onDismiss()
                    },
                ) {
                    Text(stringResource(R.string.sleep_timer_custom_set))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.sleep_timer_dialog_close))
                }
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sleep_timer_dialog_title)) },
        text = {
            Column {
                if (manager.isActive()) {
                    val status = if (manager.isEndOfTrack()) {
                        stringResource(R.string.sleep_timer_dialog_end_of_track_active)
                    } else {
                        stringResource(R.string.sleep_timer_dialog_active_message, manager.getRemainingFormatted())
                    }
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                stringArrayResource(R.array.sleep_timer_duration_labels).forEachIndexed { index, label ->
                    TextButton(
                        onClick = {
                            when (val minutes = SLEEP_TIMER_VALUES[index]) {
                                SLEEP_TIMER_CUSTOM -> showCustomInput = true
                                SLEEP_TIMER_END_OF_TRACK -> {
                                    manager.startEndOfTrack()
                                    onDismiss()
                                }
                                else -> {
                                    manager.startTimer(minutes)
                                    onDismiss()
                                }
                            }
                        },
                    ) {
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Row {
                if (manager.isActive()) {
                    TextButton(
                        onClick = {
                            manager.cancelTimer()
                            onDismiss()
                        },
                    ) {
                        Text(stringResource(R.string.sleep_timer_dialog_cancel_timer))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.sleep_timer_dialog_close))
                }
            }
        },
    )
}

@Composable
fun RatingRouteDialog(
    song: Child? = null,
    album: AlbumID3? = null,
    artist: ArtistID3? = null,
    onDismiss: () -> Unit,
) {
    val activity = LocalContext.current.requireActivity<MainActivity>()
    val viewModel = ViewModelProvider(activity)[RatingViewModel::class.java]
    val liveSong by remember(song) {
        song?.let {
            viewModel.setSong(it)
            viewModel.getLiveSong()
        }
    }?.observeAsState() ?: remember { mutableStateOf(null) }
    val liveAlbum by remember(album) {
        album?.let {
            viewModel.setAlbum(it)
            viewModel.getLiveAlbum()
        }
    }?.observeAsState() ?: remember { mutableStateOf(null) }

    if (artist != null) {
        viewModel.setArtist(artist)
    }

    var rating by remember(song, album, artist, liveSong, liveAlbum) {
        mutableFloatStateOf(
            liveSong?.userRating?.toFloat()
                ?: liveAlbum?.userRating?.toFloat()
                ?: song?.userRating?.toFloat()
                ?: album?.userRating?.toFloat()
                ?: 0f,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rating_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Slider(
                    value = rating,
                    onValueChange = { rating = it },
                    valueRange = 0f..5f,
                    steps = 4,
                )
                Text("${rating.toInt()} / 5")
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.rate(rating.toInt())
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.rating_dialog_positive_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.rating_dialog_negative_button))
            }
        },
    )
}

@Composable
fun TrackInfoRouteDialog(
    mediaMetadata: MediaMetadata,
    onDismiss: () -> Unit,
) {
    val activity = LocalContext.current.requireActivity<MainActivity>()
    val context = activity
    val extras = mediaMetadata.extras?.let(::Bundle)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(mediaMetadata.title?.toString().orEmpty()) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = mediaMetadata.artist?.toString().orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                )
                extras?.trackInfoRows(context).orEmpty().forEach { (label, value, link) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = link != null) {
                                link ?: return@clickable
                                onDismiss()
                                activity.openAssetLink(link, AssetLinkUtil.TYPE_SONG != link.type)
                            },
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium)
                        Text(value, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text(trackTranscodingInfo(extras))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.track_info_dialog_positive_button))
            }
        },
    )
}

@Composable
fun GithubTempoUpdateRouteDialog(
    releaseUrl: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    fun openLink(link: String) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
        )
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.github_update_dialog_title)) },
        confirmButton = {
            TextButton(onClick = { openLink(releaseUrl) }) {
                Text(stringResource(R.string.github_update_dialog_positive_button))
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        Preferences.setTempusUpdateReminder()
                        onDismiss()
                    },
                ) {
                    Text(stringResource(R.string.github_update_dialog_negative_button))
                }
                TextButton(onClick = { openLink(context.getString(R.string.support_url)) }) {
                    Text(stringResource(R.string.github_update_dialog_neutral_button))
                }
            }
        },
    )
}

fun openBatteryOptimizationSettings(context: android.content.Context) {
    context.startActivity(
        Intent().apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
        },
    )
}

private data class TrackInfoRow(
    val label: String,
    val value: String,
    val link: AssetLinkUtil.AssetLink? = null,
)

private fun Bundle.trackInfoRows(context: android.content.Context): List<TrackInfoRow> {
    val placeholder = context.getString(R.string.label_placeholder)
    val songLink = AssetLinkUtil.buildAssetLink(AssetLinkUtil.TYPE_SONG, getString("id"))
    val albumLink = AssetLinkUtil.buildAssetLink(AssetLinkUtil.TYPE_ALBUM, getString("albumId"))
    val artistLink = AssetLinkUtil.buildAssetLink(AssetLinkUtil.TYPE_ARTIST, getString("artistId"))
    val genreValue = getString("genre", placeholder)
    val yearValue = getInt("year", 0)
    val genreLink = AssetLinkUtil.parseLinkString(getString("assetLinkGenre"))
        ?: genreValue.takeIf { it.isNotBlank() && it != placeholder }?.let {
            AssetLinkUtil.buildAssetLink(AssetLinkUtil.TYPE_GENRE, it)
        }
    val yearLink = AssetLinkUtil.parseLinkString(getString("assetLinkYear"))
        ?: yearValue.takeIf { it != 0 }?.let {
            AssetLinkUtil.buildAssetLink(AssetLinkUtil.TYPE_YEAR, it.toString())
        }

    fun intValue(key: String): String = getInt(key, 0).let { if (it != 0) it.toString() else placeholder }

    return listOf(
        TrackInfoRow(context.getString(R.string.track_info_title), getString("title", placeholder), songLink),
        TrackInfoRow(context.getString(R.string.track_info_album), getString("album", placeholder), albumLink),
        TrackInfoRow(context.getString(R.string.track_info_artist), getString("artist", placeholder), artistLink),
        TrackInfoRow(context.getString(R.string.track_info_track_number), intValue("track")),
        TrackInfoRow(context.getString(R.string.track_info_year), if (yearValue != 0) yearValue.toString() else placeholder, yearLink),
        TrackInfoRow(context.getString(R.string.track_info_genre), genreValue, genreLink),
        TrackInfoRow(context.getString(R.string.track_info_size), getLong("size", 0).let { if (it != 0L) MusicUtil.getReadableByteCount(it) else placeholder }),
        TrackInfoRow(context.getString(R.string.track_info_content_type), getString("contentType", placeholder)),
        TrackInfoRow(context.getString(R.string.track_info_suffix), getString("suffix", placeholder)),
        TrackInfoRow(context.getString(R.string.track_info_transcoded_content_type), getString("transcodedContentType", placeholder)),
        TrackInfoRow(context.getString(R.string.track_info_transcoded_suffix), getString("transcodedSuffix", placeholder)),
        TrackInfoRow(context.getString(R.string.track_info_duration), getInt("duration", 0).let { if (it != 0) MusicUtil.getReadableDurationString(it.toLong(), false) else placeholder }),
        TrackInfoRow(context.getString(R.string.track_info_bitrate), getInt("bitrate", 0).let { if (it != 0) "$it kbps" else placeholder }),
        TrackInfoRow(context.getString(R.string.track_info_sampling_rate), getInt("samplingRate", 0).let { if (it != 0) "$it Hz" else placeholder }),
        TrackInfoRow(context.getString(R.string.track_info_bit_depth), getInt("bitDepth", 0).let { if (it != 0) "$it bits" else placeholder }),
        TrackInfoRow(context.getString(R.string.track_info_path), getString("path", placeholder)),
        TrackInfoRow(context.getString(R.string.track_info_disc_number), intValue("discNumber")),
    )
}

@Composable
private fun trackTranscodingInfo(extras: Bundle?): String {
    val context = LocalContext.current
    val prioritizeServerTranscoding = Preferences.isServerPrioritized()
    val transcodingExtension = MusicUtil.getTranscodingFormatPreference()
    val transcodingBitrateStr = MusicUtil.getBitratePreference()
    val transcodingBitrateValue = transcodingBitrateStr.toIntOrNull() ?: 0
    val transcodingBitrate = if (transcodingBitrateValue != 0) "${transcodingBitrateValue}kbps" else "Original"

    return when {
        extras?.getString("uri", "")?.contains(Constants.DOWNLOAD_URI) == true ->
            context.getString(R.string.track_info_summary_downloaded_file)
        prioritizeServerTranscoding ->
            context.getString(R.string.track_info_summary_server_prioritized)
        transcodingExtension == "raw" && transcodingBitrate == "Original" ->
            context.getString(R.string.track_info_summary_original_file)
        transcodingExtension != "raw" && transcodingBitrate == "Original" ->
            context.getString(R.string.track_info_summary_transcoding_codec, transcodingExtension)
        transcodingExtension == "raw" ->
            context.getString(R.string.track_info_summary_transcoding_bitrate, transcodingBitrate)
        else ->
            context.getString(R.string.track_info_summary_full_transcode, transcodingExtension, transcodingBitrate)
    }
}

private val SPEED_VALUES = floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
private val SPEED_LABELS = arrayOf("0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "1.75x", "2.0x")
private const val SLEEP_TIMER_END_OF_TRACK = -2
private const val SLEEP_TIMER_CUSTOM = -1
private val SLEEP_TIMER_VALUES = intArrayOf(5, 10, 15, 20, 30, 45, 60, SLEEP_TIMER_END_OF_TRACK, SLEEP_TIMER_CUSTOM)
