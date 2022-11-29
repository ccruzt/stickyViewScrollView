package com.ui.sticky.recycler.demo.sticky


import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.core.widget.NestedScrollView
import kotlin.math.min

class StickyScrollView@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = android.R.attr.scrollViewStyle
) : NestedScrollView(context, attrs, defStyle)  {

    private var stickyViews = ArrayList<View>()
    private var currentlyStickingView: View? = null
    private var stickyViewTopOffset = 0f
    private var stickyViewLeftOffset = 0
    private var redirectTouchesToStickyView = false
    private var clippingToPadding = false
    private var clipToPaddingHasBeenSet = false

    private var mShadowHeight = 0
    private var mShadowDrawable: Drawable? = null

    private val invalidateRunnable: Runnable = object : Runnable {
        override fun run() {
            currentlyStickingView?.let { currentStickyView ->
                val l = getLeftForViewRelativeOnlyChild(currentStickyView)
                val t = getBottomForViewRelativeOnlyChild(currentStickyView)
                val r = getRightForViewRelativeOnlyChild(currentStickyView)
                val b = (scrollY + (currentStickyView.height + stickyViewTopOffset)).toInt()
                postInvalidate(l, t, r, b)
            }
            postDelayed(this, 16)
        }
    }

    companion object {
        /**
         * Tag for views that should stick and have constant drawing. e.g. TextViews, ImageViews etc
         */
        const val STICKY_TAG = "sticky"
    }

    private fun getLeftForViewRelativeOnlyChild(v: View): Int {
        var view = v
        var left = view.left
        while (view.parent !== getChildAt(0)) {
            view = view.parent as View
            left += view.left
        }
        return left
    }

    private fun getTopForViewRelativeOnlyChild(v: View): Int {
        var view = v
        var top = view.top
        while (view.parent !== getChildAt(0)) {
            view = view.parent as View
            top += view.top
        }
        return top
    }

    private fun getRightForViewRelativeOnlyChild(v: View): Int {
        var view = v
        var right = view.right
        while (view.parent !== getChildAt(0)) {
            view = view.parent as View
            right += view.right
        }
        return right
    }

    private fun getBottomForViewRelativeOnlyChild(v: View): Int {
        var view = v
        var bottom = view.bottom
        while (view.parent !== getChildAt(0)) {
            view = view.parent as View
            bottom += view.bottom
        }
        return bottom
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (!clipToPaddingHasBeenSet) {
            clippingToPadding = true
        }
        notifyHierarchyChanged()
    }

    override fun setClipToPadding(clipToPadding: Boolean) {
        super.setClipToPadding(clipToPadding)
        clippingToPadding = clipToPadding
        clipToPaddingHasBeenSet = true
    }

    override fun addView(child: View) {
        super.addView(child)
        findStickyViews(child)
    }

    override fun addView(child: View, index: Int) {
        super.addView(child, index)
        findStickyViews(child)
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams?) {
        super.addView(child, index, params)
        findStickyViews(child)
    }

    override fun addView(child: View, width: Int, height: Int) {
        super.addView(child, width, height)
        findStickyViews(child)
    }

    override fun addView(child: View, params: ViewGroup.LayoutParams?) {
        super.addView(child, params)
        findStickyViews(child)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        currentlyStickingView?.let { currentlyStickingView ->
            canvas.save()
            canvas.translate(
                (paddingLeft + stickyViewLeftOffset).toFloat(),
                scrollY + stickyViewTopOffset + if (clippingToPadding) paddingTop else 0
            )
            canvas.clipRect(
                0f,
                getClipRectTop(),
                (width - stickyViewLeftOffset).toFloat(),
                (currentlyStickingView.height + mShadowHeight + 1).toFloat()
            )
            mShadowDrawable?.let {
                val left = 0
                val right = currentlyStickingView.width
                val top = currentlyStickingView.height
                val bottom = currentlyStickingView.height + mShadowHeight
                it.setBounds(left, top, right, bottom)
                it.draw(canvas)
            }

            canvas.clipRect(
                0f,
                getClipRectTop(),
                width.toFloat(),
                currentlyStickingView.height.toFloat()
            )

            currentlyStickingView.draw(canvas)
            canvas.restore()
        }
    }

    private fun getClipRectTop() = if (clippingToPadding) - stickyViewTopOffset else 0f

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            redirectTouchesToStickyView = true
        }
        if (redirectTouchesToStickyView) {
            redirectTouchesToStickyView = currentlyStickingView != null
            if (redirectTouchesToStickyView) {
                redirectTouchesToStickyView = shouldRedirectTouchesToStickyView(ev, currentlyStickingView!!)
            }
        } else if (currentlyStickingView == null) {
            redirectTouchesToStickyView = false
        }

        if (redirectTouchesToStickyView) {
            ev.offsetLocation(
                0f,
                -1 * (scrollY + stickyViewTopOffset - getTopForViewRelativeOnlyChild(
                    currentlyStickingView!!
                ))
            )
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun shouldRedirectTouchesToStickyView(ev: MotionEvent, currentStickyView: View) =
        ev.y <= currentStickyView.height + stickyViewTopOffset
                && ev.x >= getLeftForViewRelativeOnlyChild(currentStickyView)
                && ev.x <= getRightForViewRelativeOnlyChild(currentStickyView)

    private var hasNotDoneActionDown = true

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (redirectTouchesToStickyView) {
            ev.offsetLocation(
                0f,
                scrollY + stickyViewTopOffset - getTopForViewRelativeOnlyChild(currentlyStickingView!!)
            )
        }
        if (ev.action == MotionEvent.ACTION_DOWN) {
            hasNotDoneActionDown = false
        }
        if (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_CANCEL) {
            hasNotDoneActionDown = true
        }
        if (hasNotDoneActionDown) {
            val down = MotionEvent.obtain(ev)
            down.action = MotionEvent.ACTION_DOWN
            super.onTouchEvent(down)
            hasNotDoneActionDown = false
        }
        return super.onTouchEvent(ev)
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        doTheStickyThing()
    }

    private fun scrollYPlusPaddingTop(): Int {
        val padding = if (clippingToPadding) 0 else paddingTop
        return scrollY + padding
    }

    private fun doTheStickyThing() {
        var viewThatShouldStick: View? = null
        var approachingView: View? = null
        for (stickyView in stickyViews) {
            val viewTop = getTopForViewRelativeOnlyChild(stickyView) - scrollYPlusPaddingTop()
            if (viewTop <= 0 && assignViewThatShouldSticky(viewThatShouldStick, viewTop)) {
                viewThatShouldStick = stickyView
            } else {
                if (assignApproachingView(approachingView, viewTop)) {
                    approachingView = stickyView
                }
            }
        }

        if (viewThatShouldStick != null) {
            stickyViewTopOffset = getStickyViewTopOffsetStickyLogic(approachingView, viewThatShouldStick)
            if (viewThatShouldStick !== currentlyStickingView) {
                if (currentlyStickingView != null) {
                    stopStickingCurrentlyStickingView()
                }
                // only compute the left offset when we start sticking.
                stickyViewLeftOffset = getLeftForViewRelativeOnlyChild(viewThatShouldStick)
                startStickingView(viewThatShouldStick)
            }
        } else if (currentlyStickingView != null) {
            stopStickingCurrentlyStickingView()
        }
    }

    private fun assignApproachingView(approachingView: View?, viewTop: Int) =
        approachingView == null || viewTop < getTopForViewRelativeOnlyChild(approachingView) - scrollYPlusPaddingTop()

    private fun assignViewThatShouldSticky(viewThatShouldStick: View?, viewTop: Int) =
        viewThatShouldStick == null || viewTop > getTopForViewRelativeOnlyChild(viewThatShouldStick) - scrollYPlusPaddingTop()

    private fun getStickyViewTopOffsetStickyLogic(approachingView: View?, viewThatShouldStick: View): Float {
        return if (approachingView == null) {
            0f
        } else {
            min(0, getTopForViewRelativeOnlyChild(approachingView) - scrollYPlusPaddingTop() - viewThatShouldStick.height).toFloat()
        }
    }

    private fun startStickingView(viewThatShouldStick: View) {
        currentlyStickingView = viewThatShouldStick
    }

    private fun stopStickingCurrentlyStickingView() {
        currentlyStickingView = null
        removeCallbacks(invalidateRunnable)
    }

    private fun notifyHierarchyChanged() {
        if (currentlyStickingView != null) {
            stopStickingCurrentlyStickingView()
        }
        stickyViews.clear()
        findStickyViews(getChildAt(0))
        doTheStickyThing()
        invalidate()
    }

    private fun findStickyViews(v: View) {
        if (v is ViewGroup) {
            v.forEach { childView ->
                val tag = getStringTagForView(childView)
                if (tag.contains(STICKY_TAG)) {
                    stickyViews.add(childView)
                } else if (childView is ViewGroup) {
                    findStickyViews(childView)
                }
            }
        } else {
            val tag = v.tag.toString()
            if (tag.contains(STICKY_TAG)) {
                stickyViews.add(v)
            }
        }
    }

    private fun getStringTagForView(v: View): String {
        return v.tag?.toString() ?: ""
    }

}