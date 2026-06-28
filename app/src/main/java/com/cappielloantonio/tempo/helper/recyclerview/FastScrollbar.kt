package com.cappielloantonio.tempo.helper.recyclerview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max
import kotlin.math.min

class FastScrollbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private var bubble: TextView? = null
    private var handle: View? = null
    private var recyclerView: RecyclerView? = null
    private var viewHeight = 0
    private var isInitialized = false
    private var currentAnimator: ObjectAnimator? = null

    private val onScrollListener: RecyclerView.OnScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            updateBubbleAndHandlePosition()
        }
    }

    interface BubbleTextGetter {
        fun getTextToShowInBubble(pos: Int): String?
    }

    init {
        init()
    }

    private fun init() {
        if (isInitialized) return
        isInitialized = true
        orientation = HORIZONTAL
        clipChildren = false
    }

    fun setViewsToUse(@LayoutRes layoutResId: Int, @IdRes bubbleResId: Int, @IdRes handleResId: Int) {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(layoutResId, this, true)
        bubble = findViewById(bubbleResId)
        bubble?.visibility = INVISIBLE
        handle = findViewById(handleResId)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewHeight = h
        updateBubbleAndHandlePosition()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        val handleView = handle ?: return super.onTouchEvent(event)
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (event.x < handleView.x - ViewCompat.getPaddingStart(handleView)) return false
                currentAnimator?.cancel()
                if (bubble?.visibility == INVISIBLE) showBubble()
                handleView.isSelected = true
            }
            MotionEvent.ACTION_MOVE -> {
                val y = event.y
                setBubbleAndHandlePosition(y)
                setRecyclerViewPosition(y)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handleView.isSelected = false
                hideBubble()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun setRecyclerView(recyclerView: RecyclerView?) {
        if (this.recyclerView !== recyclerView) {
            this.recyclerView?.removeOnScrollListener(onScrollListener)
            this.recyclerView = recyclerView
            recyclerView?.addOnScrollListener(onScrollListener)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        recyclerView?.removeOnScrollListener(onScrollListener)
        recyclerView = null
    }

    private fun setRecyclerViewPosition(y: Float) {
        val rv = recyclerView ?: return
        val handleView = handle ?: return
        val adapter = rv.adapter ?: return
        val itemCount = adapter.itemCount
        val proportion: Float = when {
            handleView.y == 0f -> 0f
            handleView.y + handleView.height >= viewHeight - TRACK_SNAP_RANGE -> 1f
            else -> y / viewHeight.toFloat()
        }
        val targetPos = getValueInRange(0, itemCount - 1, (proportion * itemCount.toFloat()).toInt())
        (rv.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(targetPos, 0)
        
        val bubbleText = (adapter as? BubbleTextGetter)?.getTextToShowInBubble(targetPos)
        bubble?.let {
            it.text = bubbleText
            if (TextUtils.isEmpty(bubbleText)) {
                hideBubble()
            } else if (it.visibility == INVISIBLE) {
                showBubble()
            }
        }
    }

    private fun getValueInRange(minVal: Int, maxVal: Int, value: Int): Int {
        val minimum = max(minVal, value)
        return min(minimum, maxVal)
    }

    private fun updateBubbleAndHandlePosition() {
        val rv = recyclerView ?: return
        val handleView = handle ?: return
        if (bubble == null || handleView.isSelected) return
        val verticalScrollOffset = rv.computeVerticalScrollOffset()
        val verticalScrollRange = rv.computeVerticalScrollRange()
        val proportion = verticalScrollOffset.toFloat() / (verticalScrollRange.toFloat() - viewHeight)
        setBubbleAndHandlePosition(viewHeight * proportion)
    }

    private fun setBubbleAndHandlePosition(y: Float) {
        val handleView = handle ?: return
        val handleHeight = handleView.height
        handleView.y = getValueInRange(0, viewHeight - handleHeight, (y - handleHeight / 2).toInt()).toFloat()
        bubble?.let {
            val bubbleHeight = it.height
            it.y = getValueInRange(0, viewHeight - bubbleHeight - handleHeight / 2, (y - bubbleHeight).toInt()).toFloat()
        }
    }

    private fun showBubble() {
        val bubbleView = bubble ?: return
        bubbleView.visibility = VISIBLE
        currentAnimator?.cancel()
        currentAnimator = ObjectAnimator.ofFloat(bubbleView, "alpha", 0f, 1f).setDuration(BUBBLE_ANIMATION_DURATION.toLong())
        currentAnimator?.start()
    }

    private fun hideBubble() {
        val bubbleView = bubble ?: return
        currentAnimator?.cancel()
        currentAnimator = ObjectAnimator.ofFloat(bubbleView, "alpha", 1f, 0f).setDuration(BUBBLE_ANIMATION_DURATION.toLong())
        currentAnimator?.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                bubbleView.visibility = INVISIBLE
                currentAnimator = null
            }

            override fun onAnimationCancel(animation: Animator) {
                super.onAnimationCancel(animation)
                bubbleView.visibility = INVISIBLE
                currentAnimator = null
            }
        })
        currentAnimator?.start()
    }

    companion object {
        private const val BUBBLE_ANIMATION_DURATION = 100
        private const val TRACK_SNAP_RANGE = 5
    }
}
