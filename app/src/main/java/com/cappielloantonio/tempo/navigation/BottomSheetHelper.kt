package com.cappielloantonio.tempo.navigation

import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.annotation.OptIn
import androidx.fragment.app.FragmentManager
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.ui.fragment.PlayerBottomSheetFragment
import com.cappielloantonio.tempo.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlin.math.max
import kotlin.math.min

class BottomSheetHelper(
    private val bottomSheetBehavior: BottomSheetBehavior<View>,
    private val bottomSheetView: View,
    private val fragmentManager: FragmentManager
) {
    @OptIn(UnstableApi::class)
    private val playerBottomSheetFragment = PlayerBottomSheetFragment()

    var state: Int
        get() = bottomSheetBehavior.state
        set(value) {
            bottomSheetBehavior.state = value
        }

    fun addCallback(callback: BottomSheetBehavior.BottomSheetCallback) {
        bottomSheetBehavior.addBottomSheetCallback(callback)
    }

    fun setStateInPeek(isVisible: Boolean) {
        if (isVisible) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    fun setVisibility(visibility: Boolean) {
        bottomSheetView.visibility = if (visibility) View.VISIBLE else View.GONE
    }

    @OptIn(UnstableApi::class)
    fun replaceFragment(playerBottomSheet: Int) {
        fragmentManager
            .beginTransaction()
            .replace(playerBottomSheet, playerBottomSheetFragment, "PlayerBottomSheet")
            .commit()
    }

    fun checkAfterStateChanged(mainViewModel: MainViewModel) {
        Handler(Looper.getMainLooper()).postDelayed({
            setStateInPeek(mainViewModel.isQueueLoaded())
        }, 100)
    }

    fun collapseDelayed() {
        Handler(Looper.getMainLooper()).postDelayed({
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }, 100)
    }

    fun setDraggable(isDraggable: Boolean) {
        bottomSheetBehavior.isDraggable = isDraggable
    }

    @OptIn(UnstableApi::class)
    fun animate(slideOffset: Float) {
        val header = playerBottomSheetFragment.getPlayerHeader()
        if (header != null) {
            val condensedSlideOffset = max(0.0f, min(0.2f, slideOffset - 0.2f)) / 0.2f
            header.alpha = 1 - condensedSlideOffset
            header.visibility = if (condensedSlideOffset > 0.99) View.GONE else View.VISIBLE
        }
    }

    fun setPeekHeight(peekHeight: Int, displayDensity: Float) {
        val newPeekPx = (peekHeight * displayDensity).toInt()
        bottomSheetBehavior.peekHeight = newPeekPx
    }
}
