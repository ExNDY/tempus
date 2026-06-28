package com.cappielloantonio.tempo.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.text.Html
import android.util.Log
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.repository.DownloadRepository
import com.cappielloantonio.tempo.subsonic.models.Child
import java.text.CharacterIterator
import java.text.DecimalFormat
import java.text.StringCharacterIterator
import java.util.Locale
import java.util.regex.Pattern
import java.util.stream.Collectors

object MusicUtil {
    private const val TAG = "MusicUtil"
    private val BITRATE_PATTERN = Pattern.compile("&maxBitRate=\\d+")
    private val FORMAT_PATTERN = Pattern.compile("&format=\\w+")

    @JvmStatic
    fun getStreamUri(id: String, timeOffset: Int = 0): Uri {
        val params = App.getSubsonicClientInstance(false).params
        val uri = StringBuilder()
        uri.append(App.getSubsonicClientInstance(false).url)
        uri.append("stream")

        params["u"]?.let { uri.append("?u=").append(Util.encode(it)) }
        if (params.containsKey("p") && params["p"] != null)
            uri.append("&p=").append(params["p"])
        if (params.containsKey("s") && params["s"] != null)
            uri.append("&s=").append(params["s"])
        if (params.containsKey("t") && params["t"] != null)
            uri.append("&t=").append(params["t"])
        if (params.containsKey("v") && params["v"] != null)
            uri.append("&v=").append(params["v"])
        if (params.containsKey("c") && params["c"] != null)
            uri.append("&c=").append(params["c"])

        val selectedBitrate = getBitratePreference()
        val selectedFormat = getTranscodingFormatPreference()
        Log.i(TAG, "DEBUG: Requesting Format: $selectedFormat at Bitrate: $selectedBitrate")

        if (!Preferences.isServerPrioritized())
            uri.append("&maxBitRate=").append(getBitratePreference())
        if (!Preferences.isServerPrioritized())
            uri.append("&format=").append(getTranscodingFormatPreference())
        if (timeOffset > 0)
            uri.append("&timeOffset=").append(timeOffset)

        uri.append("&id=").append(id)
        Log.d(TAG, "getStreamUri: $uri")
        return Uri.parse(uri.toString())
    }

    @JvmStatic
    fun getStreamUri(id: String): Uri {
        return getStreamUri(id, 0)
    }

    @JvmStatic
    fun updateStreamUri(uri: Uri?): Uri? {
        if (uri == null) return null
        val scheme = uri.scheme
        // If it is local (content:// or file://), return it IMMEDIATELY.
        if (scheme != null && (scheme == "content" || scheme == "file")) {
            return uri
        }

        var s = uri.toString()
        val m1 = BITRATE_PATTERN.matcher(s)
        s = m1.replaceAll("")
        val m2 = FORMAT_PATTERN.matcher(s)
        s = m2.replaceAll("")

        if (!Preferences.isServerPrioritized())
            s += "&maxBitRate=" + getBitratePreference()
        if (!Preferences.isServerPrioritized())
            s += "&format=" + getTranscodingFormatPreference()

        return Uri.parse(s)
    }

    @JvmStatic
    fun getDownloadUri(id: String): Uri {
        val uri = StringBuilder()
        val download = DownloadRepository().getDownload(id)
        if (download == null || download.downloadUri.isNullOrEmpty()) {
            val params = App.getSubsonicClientInstance(false).params
            uri.append(App.getSubsonicClientInstance(false).url)
            uri.append("download")

            params["u"]?.let { uri.append("?u=").append(Util.encode(it)) }
            if (params.containsKey("p") && params["p"] != null)
                uri.append("&p=").append(params["p"])
            if (params.containsKey("s") && params["s"] != null)
                uri.append("&s=").append(params["s"])
            if (params.containsKey("t") && params["t"] != null)
                uri.append("&t=").append(params["t"])
            if (params.containsKey("v") && params["v"] != null)
                uri.append("&v=").append(params["v"])
            if (params.containsKey("c") && params["c"] != null)
                uri.append("&c=").append(params["c"])

            uri.append("&id=").append(id)
        } else {
            uri.append(download.downloadUri)
        }
        Log.d(TAG, "getDownloadUri: $uri")
        return Uri.parse(uri.toString())
    }

    @JvmStatic
    fun getTranscodedDownloadUri(id: String): Uri {
        val params = App.getSubsonicClientInstance(false).params
        val uri = StringBuilder()
        uri.append(App.getSubsonicClientInstance(false).url)
        uri.append("stream")

        params["u"]?.let { uri.append("?u=").append(Util.encode(it)) }
        if (params.containsKey("p") && params["p"] != null)
            uri.append("&p=").append(params["p"])
        if (params.containsKey("s") && params["s"] != null)
            uri.append("&s=").append(params["s"])
        if (params.containsKey("t") && params["t"] != null)
            uri.append("&t=").append(params["t"])
        if (params.containsKey("v") && params["v"] != null)
            uri.append("&v=").append(params["v"])
        if (params.containsKey("c") && params["c"] != null)
            uri.append("&c=").append(params["c"])

        if (!Preferences.isServerPrioritizedInTranscodedDownload())
            uri.append("&maxBitRate=").append(getBitratePreferenceForDownload())
        if (!Preferences.isServerPrioritizedInTranscodedDownload())
            uri.append("&format=").append(getTranscodingFormatPreferenceForDownload())

        uri.append("&id=").append(id)
        Log.d(TAG, "getTranscodedDownloadUri: $uri")
        return Uri.parse(uri.toString())
    }

    @JvmStatic
    fun getReadableDurationString(duration: Long?, millis: Boolean): String {
        val length = duration ?: 0L
        val minutes: Long
        val seconds: Long

        if (millis) {
            minutes = (length / 1000) / 60
            seconds = (length / 1000) % 60
        } else {
            minutes = length / 60
            seconds = length % 60
        }

        return if (minutes < 60) {
            String.format(Locale.getDefault(), "%01d:%02d", minutes, seconds)
        } else {
            val hours = minutes / 60
            val mins = minutes % 60
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, mins, seconds)
        }
    }

    @JvmStatic
    fun getReadableDurationString(duration: Int?, millis: Boolean): String {
        return getReadableDurationString(duration?.toLong(), millis)
    }

    @JvmStatic
    fun getReadableAudioQualityString(child: Child): String {
        if (!Preferences.showAudioQuality() || child.bitrate == null) return ""
        val bitDepthPart = if (child.bitDepth != null && child.bitDepth != 0) {
            "${child.bitDepth}/${if (child.samplingRate != null) child.samplingRate!! / 1000 else ""}"
        } else if (child.samplingRate != null) {
            "${DecimalFormat("0.#").format(child.samplingRate!! / 1000.0)}kHz"
        } else {
            ""
        }
        return "• ${child.bitrate}kbps • $bitDepthPart ${child.suffix}"
    }

    @JvmStatic
    fun getReadablePodcastDurationString(duration: Long): String {
        val minutes = duration / 60
        return if (minutes < 60) {
            String.format(Locale.getDefault(), "%01d min", minutes)
        } else {
            val hours = minutes / 60
            val mins = minutes % 60
            String.format(Locale.getDefault(), "%d h %02d min", hours, mins)
        }
    }

    @JvmStatic
    fun getReadableTrackNumber(context: Context, trackNumber: Int?): String {
        return trackNumber?.toString() ?: context.getString(R.string.label_placeholder)
    }

    @JvmStatic
    fun getReadableString(string: String?): String {
        return if (string != null) {
            Html.fromHtml(string, Html.FROM_HTML_MODE_COMPACT).toString()
        } else {
            ""
        }
    }

    @JvmStatic
    fun forceReadableString(string: String?): String {
        return if (string != null) {
            getReadableString(string)
                .replace("&#34;".toRegex(), "\"")
                .replace("&#39;".toRegex(), "'")
                .replace("&amp;".toRegex(), "'")
                .replace("<a\\s+([^>]+)>((?:.(?!</a>))*.)</a>".toRegex(), "")
        } else {
            ""
        }
    }

    @JvmStatic
    fun getReadableLyrics(string: String?): String {
        return if (string != null) {
            string
                .replace("&#34;".toRegex(), "\"")
                .replace("&#39;".toRegex(), "'")
                .replace("&amp;".toRegex(), "'")
                .replace("&#xA;".toRegex(), "\n")
        } else {
            ""
        }
    }

    @JvmStatic
    fun getReadableByteCount(bytes: Long): String {
        val absB = if (bytes == Long.MIN_VALUE) Long.MAX_VALUE else Math.abs(bytes)
        if (absB < 1024) {
            return "$bytes B"
        }
        var value = absB
        val ci: CharacterIterator = StringCharacterIterator("KMGTPE")
        var i = 40
        while (i >= 0 && absB > 0xfffccccccccccccL shr i) {
            value = value shr 10
            ci.next()
            i -= 10
        }
        value *= java.lang.Long.signum(bytes).toLong()
        return String.format("%.1f %ciB", value / 1024.0, ci.current())
    }

    @JvmStatic
    fun passwordHexEncoding(plainPassword: String): String {
        return "enc:" + plainPassword.chars()
            .mapToObj { Integer.toHexString(it) }
            .collect(Collectors.joining())
    }

    @JvmStatic
    fun getBitratePreference(): String {
        val connectivityManager = connectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        val audioTranscodeFormat = getTranscodingFormatPreference()

        if (audioTranscodeFormat == "raw" || network == null || networkCapabilities == null)
            return "0"

        return if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            Preferences.getMaxBitrateWifi()
        } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            Preferences.getMaxBitrateMobile()
        } else {
            Preferences.getMaxBitrateWifi()
        }
    }

    @JvmStatic
    fun getTranscodingFormatPreference(): String {
        val connectivityManager = connectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)

        if (network == null || networkCapabilities == null) return "raw"

        return if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            val format = Preferences.getAudioTranscodeFormatWifi()
            Log.d(TAG, "DEBUG: Using WIFI Format: $format")
            format
        } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            val format = Preferences.getAudioTranscodeFormatMobile()
            Log.d(TAG, "DEBUG: Using MOBILE Format: $format")
            format
        } else {
            Preferences.getAudioTranscodeFormatWifi()
        }
    }

    @JvmStatic
    fun getBitratePreferenceForDownload(): String {
        val audioTranscodeFormat = getTranscodingFormatPreferenceForDownload()
        if (audioTranscodeFormat == "raw") return "0"
        return Preferences.getBitrateTranscodedDownload()
    }

    @JvmStatic
    fun getTranscodingFormatPreferenceForDownload(): String {
        return Preferences.getAudioTranscodeFormatTranscodedDownload()
    }

    @JvmStatic
    fun limitPlayableMedia(toLimit: List<Child>, position: Int): List<Child> {
        if (toLimit.isNotEmpty() && toLimit.size > Constants.PLAYABLE_MEDIA_LIMIT) {
            val from = if (position < Constants.PRE_PLAYABLE_MEDIA) 0 else position - Constants.PRE_PLAYABLE_MEDIA
            val to = Math.min(from + Constants.PLAYABLE_MEDIA_LIMIT, toLimit.size)
            return toLimit.subList(from, to)
        }
        return toLimit
    }

    @JvmStatic
    fun getPlayableMediaPosition(toLimit: List<Child>, position: Int): Int {
        if (toLimit.isNotEmpty() && toLimit.size > Constants.PLAYABLE_MEDIA_LIMIT) {
            return Math.min(position, Constants.PRE_PLAYABLE_MEDIA)
        }
        return position
    }

    private val connectivityManager: ConnectivityManager
        get() = App.getContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @JvmStatic
    fun ratingFilter(toFilter: MutableList<Child>) {
        if (toFilter.isEmpty()) return
        val filtered = toFilter.stream()
            .filter { child: Child ->
                (child.userRating != null && child.userRating!! >= Preferences.getMinStarRatingAccepted()) || (child.userRating == null)
            }
            .collect(Collectors.toList())
        toFilter.clear()
        toFilter.addAll(filtered)
    }

    @JvmStatic
    fun isImageUrl(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        val path = url.lowercase(Locale.getDefault()).trim { it <= ' ' }.split("\\?".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        return path.endsWith(".jpg") || path.endsWith(".jpeg") ||
                path.endsWith(".png") || path.endsWith(".webp") ||
                path.endsWith(".gif") || path.endsWith(".bmp") ||
                path.endsWith(".svg")
    }
}
