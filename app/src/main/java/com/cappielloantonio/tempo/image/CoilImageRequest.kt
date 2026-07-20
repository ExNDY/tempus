package com.cappielloantonio.tempo.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.Config.RGB_565
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toDrawable
import coil3.imageLoader
import coil3.load
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.request.crossfade
import coil3.request.error
import coil3.request.fallback
import coil3.request.placeholder
import coil3.request.transformations
import coil3.size.Scale
import coil3.toBitmap
import coil3.transform.RoundedCornersTransformation
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.util.Util
import com.google.android.material.elevation.SurfaceColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File

class CoilImageRequest private constructor() {

    enum class ResourceType {
        Unknown,
        Album,
        Artist,
        Folder,
        Directory,
        Playlist,
        Song,
    }

    fun interface BitmapCallback {
        fun onBitmapReady(bitmap: Bitmap?)
    }

    class Builder private constructor(
        private val data: Any?,
        private val cacheKey: String?,
        private val type: ResourceType,
        private val addLastModifiedToFileCacheKey: Boolean = false,
    ) {

        fun build(): Builder = this

        fun into(imageView: ImageView) {
            loadInto(
                imageView = imageView,
                data = data,
                cacheKey = cacheKey,
                type = type,
                addLastModifiedToFileCacheKey = addLastModifiedToFileCacheKey,
            )
        }

        companion object {
            @JvmStatic
            fun from(context: Context, item: String?, type: ResourceType): Builder {
                val resolvedData = if (item != null && !Preferences.isDataSavingMode()) {
                    createUrl(item, Preferences.getImageSize())
                } else {
                    null
                }
                val resolvedCacheKey = if (item != null && !Preferences.isDataSavingMode()) {
                    createCoverArtCacheKey(item, type, Preferences.getImageSize())
                } else {
                    null
                }

                return Builder(
                    data = resolvedData,
                    cacheKey = resolvedCacheKey,
                    type = type,
                )
            }

            @JvmStatic
            fun fromLocalFile(context: Context, file: File, cacheKey: String?, type: ResourceType): Builder {
                return Builder(
                    data = file,
                    cacheKey = cacheKey ?: file.absolutePath,
                    type = type,
                    addLastModifiedToFileCacheKey = true,
                )
            }

            @JvmStatic
            fun fromData(context: Context, data: Any?, cacheKey: String?, type: ResourceType): Builder {
                return Builder(
                    data = data,
                    cacheKey = cacheKey,
                    type = type,
                )
            }
        }
    }

    companion object {
        private const val DEFAULT_CORNER_RADIUS = 1

        @JvmStatic
        fun createUrl(item: String?, size: Int): String {
            val params = App.getSubsonicClientInstance(false).params

            val uri = StringBuilder()
            uri.append(App.getSubsonicClientInstance(false).url)
            uri.append("getCoverArt")

            params["u"]?.let { uri.append("?u=").append(Util.encode(it)) }
            if (params.containsKey("p") && params["p"] != null) uri.append("&p=").append(params["p"])
            if (params.containsKey("s") && params["s"] != null) uri.append("&s=").append(params["s"])
            if (params.containsKey("t") && params["t"] != null) uri.append("&t=").append(params["t"])
            if (params.containsKey("v") && params["v"] != null) uri.append("&v=").append(params["v"])
            if (params.containsKey("c") && params["c"] != null) uri.append("&c=").append(params["c"])
            if (size != -1) uri.append("&size=").append(size)

            uri.append("&id=").append(item)

            return uri.toString()
        }

        @JvmStatic
        fun loadAlbumArtBitmap(
            context: Context,
            coverId: String,
            size: Int,
            callback: BitmapCallback,
        ) {
            val url = createUrl(coverId, size)
            val cacheKey = createCoverArtCacheKey(coverId, ResourceType.Album, size)
            val request = baseRequest(context, url, cacheKey, ResourceType.Album)
                .size(size, size)
                .allowHardware(false)
                .target(
                    onError = {
                        callback.onBitmapReady(null)
                    },
                    onSuccess = { image ->
                        callback.onBitmapReady(image.toBitmap(size, size))
                    },
                )
                .build()

            context.imageLoader.enqueue(request)
        }

        @JvmStatic
        fun buildImageRequest(
            context: Context,
            item: String?,
            type: ResourceType,
        ): ImageRequest {
            val resolvedData = if (item != null && !Preferences.isDataSavingMode()) {
                createUrl(item, Preferences.getImageSize())
            } else {
                null
            }
            val resolvedCacheKey = if (item != null && !Preferences.isDataSavingMode()) {
                createCoverArtCacheKey(item, type, Preferences.getImageSize())
            } else {
                null
            }

            return baseRequest(context, resolvedData, resolvedCacheKey, type).build()
        }

        @JvmStatic
        fun loadInto(
            imageView: ImageView,
            data: Any?,
            cacheKey: String?,
            type: ResourceType,
            addLastModifiedToFileCacheKey: Boolean = false,
        ) {
            val requestIdentity = requestIdentity(
                data = data,
                cacheKey = cacheKey,
                type = type,
                addLastModifiedToFileCacheKey = addLastModifiedToFileCacheKey,
            )
            if (imageView.getTag(R.id.tag_tempus_image_request_key) == requestIdentity) {
                return
            }
            imageView.setTag(R.id.tag_tempus_image_request_key, requestIdentity)

            imageView.load(data, imageView.context.imageLoader) {
                applyBaseOptions(imageView.context, data, cacheKey, type, addLastModifiedToFileCacheKey)
            }
        }

        @JvmStatic
        fun loadBitmapBlocking(
            context: Context,
            data: Any?,
            cacheKey: String?,
            width: Int,
            height: Int,
        ): Bitmap? = runBlocking(Dispatchers.IO) {
            val request = ImageRequest.Builder(context)
                .data(data)
                .memoryCacheKey(cacheKey)
                .size(width, height)
                .bitmapConfig(RGB_565)
                .allowHardware(false)
                .build()

            val result = context.imageLoader.execute(request)
            if (result is SuccessResult) {
                result.image.toBitmap(width, height)
            } else {
                null
            }
        }

        @JvmStatic
        fun getCachedFileBlocking(
            context: Context,
            data: Any?,
            cacheKey: String?,
        ): File? = runBlocking(Dispatchers.IO) {
            val resolvedCacheKey = cacheKey ?: defaultCacheKey(data)
            val request = ImageRequest.Builder(context)
                .data(data)
                .memoryCacheKey(resolvedCacheKey)
                .diskCacheKey(resolvedCacheKey)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()

            val result = context.imageLoader.execute(request)
            if (result !is SuccessResult) {
                return@runBlocking null
            }

            val diskKey = result.diskCacheKey ?: resolvedCacheKey ?: return@runBlocking null
            context.imageLoader.diskCache?.openSnapshot(diskKey)?.use { snapshot ->
                File(snapshot.data.toString())
            }
        }

        @JvmStatic
        fun getPlaceholder(context: Context, type: ResourceType): Drawable {
            return when (type) {
                ResourceType.Album -> AppCompatResources.getDrawable(context, R.drawable.ic_placeholder_album)
                ResourceType.Artist -> AppCompatResources.getDrawable(context, R.drawable.ic_placeholder_artist)
                ResourceType.Folder -> AppCompatResources.getDrawable(context, R.drawable.ic_placeholder_folder)
                ResourceType.Directory -> AppCompatResources.getDrawable(context, R.drawable.ic_placeholder_directory)
                ResourceType.Playlist -> AppCompatResources.getDrawable(context, R.drawable.ic_placeholder_playlist)
                ResourceType.Song -> AppCompatResources.getDrawable(context, R.drawable.ic_placeholder_song)
                ResourceType.Unknown -> SurfaceColors.SURFACE_5.getColor(context).toDrawable()
            } ?: SurfaceColors.SURFACE_5.getColor(context).toDrawable()
        }

        private fun baseRequest(
            context: Context,
            data: Any?,
            cacheKey: String?,
            type: ResourceType,
            addLastModifiedToFileCacheKey: Boolean = false,
        ): ImageRequest.Builder {
            return ImageRequest.Builder(context)
                .data(data)
                .applyBaseOptions(context, data, cacheKey, type, addLastModifiedToFileCacheKey)
        }

        private fun ImageRequest.Builder.applyBaseOptions(
            context: Context,
            data: Any?,
            cacheKey: String?,
            type: ResourceType,
            addLastModifiedToFileCacheKey: Boolean,
        ) = apply {
            val placeholder = getPlaceholder(context, type)
            placeholder(placeholder)
            fallback(placeholder)
            error(placeholder)
            crossfade(true)
            scale(Scale.FILL)
            bitmapConfig(RGB_565)

            val resolvedCacheKey = cacheKey ?: defaultCacheKey(data)
            val radius = if (Preferences.isCornerRoundingEnabled()) {
                Preferences.getRoundedCornerSize().coerceAtLeast(DEFAULT_CORNER_RADIUS)
            } else {
                DEFAULT_CORNER_RADIUS
            }
            memoryCacheKey(memoryCacheKey(resolvedCacheKey, radius))
            diskCacheKey(resolvedCacheKey)

            if (addLastModifiedToFileCacheKey && data is File) {
                memoryCacheKeyExtra("lastModified", data.lastModified().toString())
            }

            transformations(RoundedCornersTransformation(radius.toFloat()))
        }

        private fun createCoverArtCacheKey(
            item: String,
            type: ResourceType,
            size: Int,
        ): String {
            val serverId = Preferences.getServerId().orEmpty()
            return "cover:$serverId:${type.name}:$item:$size"
        }

        private fun memoryCacheKey(
            cacheKey: String?,
            radius: Int,
        ): String? {
            return cacheKey?.let { "$it:radius:$radius" }
        }

        private fun requestIdentity(
            data: Any?,
            cacheKey: String?,
            type: ResourceType,
            addLastModifiedToFileCacheKey: Boolean,
        ): String {
            val resolvedCacheKey = cacheKey ?: defaultCacheKey(data).orEmpty()
            val lastModified = if (addLastModifiedToFileCacheKey && data is File) {
                data.lastModified().toString()
            } else {
                ""
            }
            return "${type.name}|$resolvedCacheKey|$lastModified"
        }

        private fun defaultCacheKey(data: Any?): String? {
            return when (data) {
                null -> null
                is File -> "file:${data.absolutePath}"
                is String -> normalizeUrl(data) ?: data
                else -> data.toString()
            }
        }

        private fun normalizeUrl(data: String): String? {
            val url = data.toHttpUrlOrNull() ?: return null
            return url.newBuilder().build().toString()
        }
    }
}
