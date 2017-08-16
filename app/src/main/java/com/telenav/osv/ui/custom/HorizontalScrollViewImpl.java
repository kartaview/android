package com.telenav.osv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.HorizontalScrollView;

/**
 * Created by kalmanb on 7/12/17.
 */
public class HorizontalScrollViewImpl extends HorizontalScrollView {
    private ScrollViewListener scrollViewListener;

    public HorizontalScrollViewImpl(Context context) {
        super(context);
    }

    public HorizontalScrollViewImpl(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HorizontalScrollViewImpl(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setScrollViewListener(ScrollViewListener scrollViewListener) {
        this.scrollViewListener = scrollViewListener;
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
        if (scrollViewListener != null) {
            scrollViewListener.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
        }
    }

    public interface ScrollViewListener {
        void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY);
    }
}
