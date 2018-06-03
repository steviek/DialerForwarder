package com.sixbynine.dialerforwarder.view

import android.content.Context
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent

class DisablableViewPager(context: Context, attrs: AttributeSet) : ViewPager(context, attrs) {

    var disableSwiping = false

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (disableSwiping) return false
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        if (disableSwiping) return false
        return super.onTouchEvent(ev)
    }
}
