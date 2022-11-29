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
                invalidate(l, t, r, b)
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
                0f, if (clippingToPadding) -stickyViewTopOffset else 0f, (
                        width - stickyViewLeftOffset).toFloat(), (
                        currentlyStickingView.height + mShadowHeight + 1).toFloat()
            )
            if (mShadowDrawable != null) {
                val left = 0
                val right = currentlyStickingView.width
                val top = currentlyStickingView.height
                val bottom = currentlyStickingView.height + mShadowHeight
                mShadowDrawable!!.setBounds(left, top, right, bottom)
                mShadowDrawable!!.draw(canvas)
            }
            canvas.clipRect(
                0f,
                if (clippingToPadding) -stickyViewTopOffset else 0f,
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
                currentlyStickingView?.let {
                    redirectTouchesToStickyView =
                        ev.y <= currentlyStickingView!!.height + stickyViewTopOffset && ev.x >= getLeftForViewRelativeOnlyChild(
                            it
                        ) && ev.x <= getRightForViewRelativeOnlyChild(it)
                }
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
        if (hasNotDoneActionDown) {
            val down = MotionEvent.obtain(ev)
            down.action = MotionEvent.ACTION_DOWN
            super.onTouchEvent(down)
            hasNotDoneActionDown = false
        }
        if (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_CANCEL) {
            hasNotDoneActionDown = true
        }
        return super.onTouchEvent(ev)
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        doTheStickyThing()
    }

    private fun doTheStickyThing() {
        var viewThatShouldStick: View? = null
        var approachingView: View? = null
        for (v in stickyViews) {
            val viewTop =
                getTopForViewRelativeOnlyChild(v) - scrollY + if (clippingToPadding) 0 else paddingTop
            if (viewTop <= 0) {
                if (viewThatShouldStick == null || viewTop > getTopForViewRelativeOnlyChild(
                        viewThatShouldStick
                    ) - scrollY + if (clippingToPadding) 0 else paddingTop
                ) {
                    viewThatShouldStick = v
                }
            } else {
                if (approachingView == null || viewTop < getTopForViewRelativeOnlyChild(
                        approachingView
                    ) - scrollY + if (clippingToPadding) 0 else paddingTop
                ) {
                    approachingView = v
                }
            }
        }
        if (viewThatShouldStick != null) {
            stickyViewTopOffset = if (approachingView == null) 0f else Math.min(
                0,
                getTopForViewRelativeOnlyChild(approachingView) - scrollY + (if (clippingToPadding) 0 else paddingTop) - viewThatShouldStick.height
            ).toFloat()
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

    private fun startStickingView(viewThatShouldStick: View) {
        currentlyStickingView = viewThatShouldStick
    }

    private fun stopStickingCurrentlyStickingView() {
        currentlyStickingView = null
        removeCallbacks(invalidateRunnable)
    }

    /**
     * Notify that the sticky attribute has been added or removed from one or more views in the View hierarchy
     */
    fun notifyStickyAttributeChanged() {
        notifyHierarchyChanged()
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