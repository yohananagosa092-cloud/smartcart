package com.smartcart.app.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.google.android.material.card.MaterialCardView
import kotlin.math.abs

/** Detect horizontal swipes even when they begin over a button or note inside the card. */
class SwipeRevealCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : MaterialCardView(context, attrs) {
    private val threshold = 48 * resources.displayMetrics.density
    private var startX = 0f
    private var startY = 0f
    private var swiping = false
    var onSwipe: (() -> Unit)? = null

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                swiping = false
            }
            MotionEvent.ACTION_MOVE -> {
                val x = abs(event.x - startX)
                if (!swiping && x >= threshold && x > abs(event.y - startY)) {
                    swiping = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    val cancel = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
                    super.dispatchTouchEvent(cancel)
                    cancel.recycle()
                }
                if (swiping) return true
            }
            MotionEvent.ACTION_UP -> if (swiping) {
                swiping = false
                onSwipe?.invoke()
                return true
            }
            MotionEvent.ACTION_CANCEL -> swiping = false
        }
        return super.dispatchTouchEvent(event)
    }
}
