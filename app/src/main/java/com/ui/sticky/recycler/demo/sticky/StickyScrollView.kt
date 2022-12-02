package com.ui.sticky.recycler.demo.sticky

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.core.view.forEach
import androidx.core.view.marginLeft
import androidx.core.view.marginTop
import androidx.core.widget.NestedScrollView
import kotlin.math.min

class StickyScrollView@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = android.R.attr.scrollViewStyle
) : NestedScrollView(context, attrs, defStyle) {

    private var stickyViews = ArrayList<View>()
    private var currentlyStickingView: View? = null
    @VisibleForTesting
    internal var stickyViewTopOffset = 0f
    private var stickyViewLeftOffset = 0
    private var redirectTouchesToStickyView = false
    @VisibleForTesting
    internal var clippingToPadding = false
    private var clipToPaddingHasBeenSet = false
    private var hasNotDoneActionDown = true

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
        const val STICKY_VIEW_TAG = "sticky"
    }

    private fun getLeftForViewRelativeOnlyChild(v: View): Int {
        var view = v
        var left = view.left
        while (view.parent !== getChildAt(0)) {
            view = view.parent as View
            left += view.left
        }
        return left + (view.parent as View).marginLeft
    }

    private fun getTopForViewRelativeOnlyChild(v: View): Int {
        var view = v
        var top = view.top
        while (view.parent !== getChildAt(0)) {
            view = view.parent as View
            top += view.top
        }
        return top + (view.parent as View).marginTop
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
            canvas.translate((paddingLeft + stickyViewLeftOffset).toFloat(), canvasTranslateYPosition())
            canvas.clipRect(
                0f,
                getClipRectTop(),
                (width - stickyViewLeftOffset).toFloat(),
                (currentlyStickingView.height + 1).toFloat()
            )

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

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            redirectTouchesToStickyView = true
        }
        if (redirectTouchesToStickyView) {
            redirectTouchesToStickyView = currentlyStickingView != null
            if (redirectTouchesToStickyView) {
                redirectTouchesToStickyView = shouldRedirectTouchesToStickyView(ev, currentlyStickingView)
            }
        } else if (currentlyStickingView == null) {
            redirectTouchesToStickyView = false
        }

        if (redirectTouchesToStickyView) {
            currentlyStickingView?.let {
                ev.offsetLocation(0f, -1 * (scrollY + stickyViewTopOffset - getTopForViewRelativeOnlyChild(it)))
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (redirectTouchesToStickyView) {
            currentlyStickingView?.let {
                ev.offsetLocation(0f, scrollY + stickyViewTopOffset - getTopForViewRelativeOnlyChild(it))
            }
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
        handleStickyViews()
    }

    @VisibleForTesting
    internal fun canvasTranslateYPosition() = scrollY + stickyViewTopOffset + if (clippingToPadding) paddingTop else 0

    private fun getClipRectTop() = if (clippingToPadding) - stickyViewTopOffset else 0f

    private fun shouldRedirectTouchesToStickyView(ev: MotionEvent, currentStickyView: View?): Boolean {
        currentStickyView?.let {
            return ev.y <= it.height + stickyViewTopOffset &&
                    ev.x >= getLeftForViewRelativeOnlyChild(it) &&
                    ev.x <= getRightForViewRelativeOnlyChild(it)
        }
        return false
    }

    private fun scrollYPlusPaddingTop(): Int {
        val padding = if (clippingToPadding) 0 else paddingTop
        return scrollY + padding
    }

    private fun handleStickyViews() {
        var viewThatShouldStick: View? = null
        var approachingView: View? = null

        // Assign viewThatShouldStick
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
                stickyViewLeftOffset = getLeftForViewRelativeOnlyChild(viewThatShouldStick)
                currentlyStickingView = viewThatShouldStick
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
        return if (approachingView == null) 0f
        else min(0, getTopForViewRelativeOnlyChild(approachingView) - scrollYPlusPaddingTop() - viewThatShouldStick.height).toFloat()
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
        handleStickyViews()
        invalidate()
    }

    private fun findStickyViews(view: View) {
        if (view is ViewGroup) {
            findStickyViewsInsideViewGroup(view)
        } else {
            val tag = view.tag.toString()
            if (tag.contains(STICKY_VIEW_TAG)) {
                stickyViews.add(view)
            }
        }
    }

    private fun findStickyViewsInsideViewGroup(viewGroup: ViewGroup) {
        viewGroup.forEach { view ->
            val tag = view.tag?.toString() ?: ""
            if (tag.contains(STICKY_VIEW_TAG)) {
                stickyViews.add(view)
            } else if (view is ViewGroup) {
                findStickyViews(view)
            }
        }
    }
}
