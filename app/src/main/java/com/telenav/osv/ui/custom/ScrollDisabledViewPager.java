package com.telenav.osv.ui.custom;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by Kalman on 11/11/15.
 */
public class ScrollDisabledViewPager extends ViewPager {

    private boolean mEnabledScroll = false;

    public ScrollDisabledViewPager(Context context) {
        super(context);
    }

    public ScrollDisabledViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setCurrentItem(int item) {
        if (mEnabledScroll) {
            super.setCurrentItem(item);
        } else {
            super.setCurrentItem(item, false);
        }
    }

    @Override
    public void setCurrentItem(int item, boolean smoothScroll) {

        if (mEnabledScroll) {
            super.setCurrentItem(item, smoothScroll);
        } else {
            super.setCurrentItem(item, false);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mEnabledScroll) {
            try {
                return super.onInterceptTouchEvent(ev);
            } catch (Exception e) {
                //                Log.d(TAG, Log.getStackTraceString(e));
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Never allow swiping to switch between pages
        if (mEnabledScroll) {
            return super.onTouchEvent(event);
        }
        return mEnabledScroll;
    }

    public void setScrollEnabled(boolean enabled) {
        mEnabledScroll = enabled;
    }
}
