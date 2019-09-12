package com.telenav.osv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

/**
 * Created by kalmanb on 8/16/17.
 */
public class AppToolbar extends Toolbar {

    public AppToolbar(Context context) {
        super(context);
    }

    public AppToolbar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AppToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();
        boolean mEatingTouch = true;
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            mEatingTouch = false;
        }
        return isClickable() || mEatingTouch;
    }
}
