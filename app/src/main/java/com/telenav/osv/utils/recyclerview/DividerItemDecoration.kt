package com.telenav.osv.utils.recyclerview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * This DividerItemDecoration allows rendering of divider for first and last item as optional
 * It consists of two params in constructor named showFirstDivider and showLastDivider
 * If value of showFirstDivider is passed as true then divider is rendered for first element else divider is not rendered
 * If value of showLastDivider is passed as true then divider is rendered for last element else divider is not rendered
 * By default showFirstDivider and showLastDivider are considered false
 */

class DividerItemDecoration : RecyclerView.ItemDecoration {
    private var mDivider: Drawable? = null
    private var mShowFirstDivider = false
    private var mShowLastDivider = false
    private var mDividerMargin = 0

    constructor(context: Context, attrs: AttributeSet?) {
        val typedArray =
                context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.listDivider))
        mDivider = typedArray.getDrawable(0)
        typedArray.recycle()
    }

    constructor(
            context: Context,
            attrs: AttributeSet?,
            showFirstDivider: Boolean,
            showLastDivider: Boolean
    ) : this(context, attrs) {
        mShowFirstDivider = showFirstDivider
        mShowLastDivider = showLastDivider
    }

    constructor(
            context: Context,
            attrs: AttributeSet?,
            showFirstDivider: Boolean,
            showLastDivider: Boolean,
            dividerMargin: Int
    ) : this(context, attrs) {
        mShowFirstDivider = showFirstDivider
        mShowLastDivider = showLastDivider
        mDividerMargin = dividerMargin
    }

    constructor(divider: Drawable) {
        mDivider = divider
    }

    constructor(divider: Drawable, showFirstDivider: Boolean, showLastDivider: Boolean) : this(
            divider
    ) {
        mShowFirstDivider = showFirstDivider
        mShowLastDivider = showLastDivider
    }

    override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        if (mDivider == null) {
            return
        }
        if (parent.getChildAdapterPosition(view) < 1) {
            return
        }

        if (getOrientation(parent) == LinearLayoutManager.VERTICAL) {
            mDivider?.let { outRect.top = it.intrinsicHeight }
        } else {
            mDivider?.let { outRect.left = it.intrinsicWidth }
        }
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (mDivider == null) {
            super.onDraw(c, parent, state)
            return
        }

        // Initialization needed to avoid compiler warning
        var left = 0
        var right = 0
        var top = 0
        var bottom = 0
        var size = 0
        val orientation = getOrientation(parent)
        val childCount = parent.childCount

        if (orientation == LinearLayoutManager.VERTICAL) {
            mDivider?.let { size = it.intrinsicHeight }
            left = parent.paddingLeft + mDividerMargin
            right = parent.width - parent.paddingRight - mDividerMargin
        } else { //horizontal
            mDivider?.let { size = it.intrinsicWidth }
            top = parent.paddingTop
            bottom = parent.height - parent.paddingBottom
        }

        val startIndex = if (mShowFirstDivider) 0 else 1
        val endIndex = if (mShowLastDivider) childCount else childCount - 1
        for (i in startIndex until endIndex) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams

            if (orientation == LinearLayoutManager.VERTICAL) {
                mDivider?.let { top = child.bottom + params.bottomMargin - it.intrinsicHeight }
                bottom = top + size
            } else { //horizontal
                mDivider?.let { left = child.right + params.rightMargin - it.intrinsicWidth }
                right = left + size
            }
            mDivider?.setBounds(left, top, right, bottom)
            mDivider?.draw(c)
        }
    }

    private fun getOrientation(parent: RecyclerView): Int {
        if (parent.layoutManager is LinearLayoutManager) {
            return (parent.layoutManager as LinearLayoutManager).orientation
        } else {
            throw IllegalStateException("DividerItemDecoration can only be used with a LinearLayoutManager.")
        }
    }
}