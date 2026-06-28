package com.cappielloantonio.tempo.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.cappielloantonio.tempo.R
import com.google.android.material.color.MaterialColors
import java.util.Objects

object AssetLinkUtil {
    const val SCHEME = "tempo"
    const val HOST_ASSET = "asset"

    const val TYPE_SONG = "song"
    const val TYPE_ALBUM = "album"
    const val TYPE_ARTIST = "artist"
    const val TYPE_PLAYLIST = "playlist"
    const val TYPE_GENRE = "genre"
    const val TYPE_YEAR = "year"

    @JvmStatic
    fun parse(intent: Intent?): AssetLink? {
        if (intent == null) return null
        return parse(intent.data)
    }

    @JvmStatic
    fun parse(uri: Uri?): AssetLink? {
        if (uri == null) {
            return null
        }

        if (!SCHEME.equals(uri.scheme, ignoreCase = true)) {
            return null
        }

        val host = uri.host
        if (!HOST_ASSET.equals(host, ignoreCase = true)) {
            return null
        }

        if (uri.pathSegments.size < 2) {
            return null
        }

        val type = uri.pathSegments[0]
        val id = uri.pathSegments[1]
        if (TextUtils.isEmpty(type) || TextUtils.isEmpty(id)) {
            return null
        }

        if (!isSupportedType(type)) {
            return null
        }

        return AssetLink(type, id, uri)
    }

    @JvmStatic
    fun isSupportedType(type: String?): Boolean {
        if (type == null) return false
        return when (type) {
            TYPE_SONG, TYPE_ALBUM, TYPE_ARTIST, TYPE_PLAYLIST, TYPE_GENRE, TYPE_YEAR -> true
            else -> false
        }
    }

    @JvmStatic
    fun buildUri(type: String, id: String): Uri {
        return Uri.Builder()
            .scheme(SCHEME)
            .authority(HOST_ASSET)
            .appendPath(type)
            .appendPath(id)
            .build()
    }

    @JvmStatic
    fun buildLink(type: String?, id: String?): String? {
        if (TextUtils.isEmpty(type) || TextUtils.isEmpty(id) || !isSupportedType(type)) {
            return null
        }
        return buildUri(type!!, id!!).toString()
    }

    @JvmStatic
    fun buildAssetLink(type: String?, id: String?): AssetLink? {
        val link = buildLink(type, id)
        return parseLinkString(link)
    }

    @JvmStatic
    fun parseLinkString(link: String?): AssetLink? {
        if (TextUtils.isEmpty(link)) {
            return null
        }
        return parse(Uri.parse(link))
    }

    @JvmStatic
    fun copyToClipboard(context: Context, assetLink: AssetLink) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return
        val clipData = ClipData.newPlainText(
            context.getString(R.string.asset_link_clipboard_label),
            assetLink.uri.toString()
        )
        clipboardManager.setPrimaryClip(clipData)
    }

    @JvmStatic
    @StringRes
    fun getLabelRes(type: String): Int {
        return when (type) {
            TYPE_SONG -> R.string.asset_link_label_song
            TYPE_ALBUM -> R.string.asset_link_label_album
            TYPE_ARTIST -> R.string.asset_link_label_artist
            TYPE_PLAYLIST -> R.string.asset_link_label_playlist
            TYPE_GENRE -> R.string.asset_link_label_genre
            TYPE_YEAR -> R.string.asset_link_label_year
            else -> R.string.asset_link_label_unknown
        }
    }

    @JvmStatic
    fun applyLinkAppearance(view: View) {
        if (view is TextView) {
            if (view.getTag(R.id.tag_link_original_color) == null) {
                view.setTag(R.id.tag_link_original_color, view.currentTextColor)
            }
            val accent = MaterialColors.getColor(
                view, R.attr.colorPrimary,
                ContextCompat.getColor(view.context, android.R.color.holo_blue_light)
            )
            view.setTextColor(accent)
        }
    }

    @JvmStatic
    fun clearLinkAppearance(view: View) {
        if (view is TextView) {
            val original = view.getTag(R.id.tag_link_original_color)
            if (original is Int) {
                view.setTextColor(original)
            } else {
                val defaultColor = MaterialColors.getColor(
                    view, com.google.android.material.R.attr.colorOnSurface,
                    ContextCompat.getColor(view.context, android.R.color.primary_text_light)
                )
                view.setTextColor(defaultColor)
            }
        }
    }

    data class AssetLink(
        @JvmField val type: String,
        @JvmField val id: String,
        @JvmField val uri: Uri
    )
}
