package com.txtreader.app.ui.reader

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ScrollView

/**
 * 支持手势检测的 ScrollView
 */
class GestureScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {

    interface GestureListener {
        fun onSingleTap(x: Float, y: Float)
        fun onScrollChanged(scrollY: Int)
    }

    var gestureListener: GestureListener? = null

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            gestureListener?.onSingleTap(e.x, e.y)
            return true
        }
    })

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        return super.onTouchEvent(ev)
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        gestureListener?.onScrollChanged(t)
    }
}
