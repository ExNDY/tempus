package com.cappielloantonio.tempo.util

import android.content.Context
import androidx.media3.common.util.UnstableApi
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@UnstableApi
class TileSizeManager private constructor() {
    private var tileSizePx = 0
    private var tileSpanCount = 0
    private var tileSpacing = 0
    private var genreSizePx = 0
    private var genreSpanCount = 0
    private var genreSpacing = 0
    private var discoverWidthPx = 0
    private var discoverHeightPx = 0
    private var tileIsInitialized = false
    private var genreIsInitialized = false
    private var discoverIsInitialized = false

    fun getTileSizePx(context: Context): Int {
        if (!tileIsInitialized) calculateTileSize(context)
        return tileSizePx
    }

    fun getTileSpanCount(context: Context): Int {
        if (!tileIsInitialized) calculateTileSize(context)
        return tileSpanCount
    }

    fun getTileSpacing(context: Context): Int {
        if (!tileIsInitialized) calculateTileSize(context)
        return tileSpacing
    }

    fun getGenreSizePx(context: Context): Int {
        if (!genreIsInitialized) calculateGenreSize(context)
        return genreSizePx
    }

    fun getGenreSpanCount(context: Context): Int {
        if (!genreIsInitialized) calculateGenreSize(context)
        return genreSpanCount
    }

    fun getGenreSpacing(context: Context): Int {
        if (!genreIsInitialized) calculateGenreSize(context)
        return genreSpacing
    }

    fun getDiscoverWidthPx(context: Context): Int {
        if (!discoverIsInitialized) calculateTileSize(context)
        return discoverWidthPx
    }

    fun getDiscoverHeightPx(context: Context): Int {
        if (!discoverIsInitialized) calculateTileSize(context)
        return discoverHeightPx
    }

    fun calculateTileSize(context: Context) {
        val metrics = context.resources.displayMetrics
        val screenWidth = metrics.widthPixels.toFloat()
        val screenHeight = metrics.heightPixels.toFloat()

        // retrieve the divisor in the preferences
        val userTileSize = max(2, min(6, Preferences.getTileSize()))
        val divisor = userTileSize.toFloat()

        // little pading = 10
        tileSizePx = (min(screenWidth, screenHeight) / divisor).roundToInt() - 10
        tileSpanCount = max(2, (screenWidth / tileSizePx.toFloat()).roundToInt())

        tileSpacing = when (userTileSize) {
            2 -> 20
            3 -> 15
            4 -> 10
            5 -> 6
            6 -> 2
            else -> 20
        }
        tileIsInitialized = true
    }

    fun calculateGenreSize(context: Context) {
        val metrics = context.resources.displayMetrics
        val screenWidth = metrics.widthPixels.toFloat()
        val screenHeight = metrics.heightPixels.toFloat()

        // retrieve the divisor in the preferences
        val userTileSize = max(2, min(3, Preferences.getTileSize()))
        val divisor = userTileSize.toFloat()

        // little pading = 10
        genreSizePx = (min(screenWidth, screenHeight) / divisor).roundToInt() - 10
        genreSpanCount = max(2, (screenWidth / genreSizePx.toFloat()).roundToInt())

        genreSpacing = when (userTileSize) {
            2 -> 20
            3 -> 15
            4 -> 10
            5 -> 6
            6 -> 2
            else -> 20
        }
        genreIsInitialized = true
    }

    fun calculateDiscoverSize(context: Context) {
        val metrics = context.resources.displayMetrics
        val screenWidth = metrics.widthPixels.toFloat()
        val screenHeight = metrics.heightPixels.toFloat()
        val discoverDivisor: Float

        // retrieve the divisor in the preferences
        val userTileSize = max(2, min(6, Preferences.getTileSize()))

        discoverDivisor = when (userTileSize) {
            2 -> 1.0f
            3 -> 1.25f
            4 -> 1.5f
            5 -> 1.75f
            6 -> 2.0f
            else -> 1.0f
        }

        discoverWidthPx = (min(screenWidth, screenHeight) / discoverDivisor).roundToInt() - 50
        discoverHeightPx = (discoverWidthPx.toFloat() * 0.6f).roundToInt()
        discoverIsInitialized = true
    }

    companion object {
        private var instance: TileSizeManager? = null

        @JvmStatic
        fun getInstance(): TileSizeManager {
            if (instance == null) {
                instance = TileSizeManager()
            }
            return instance!!
        }
    }
}
