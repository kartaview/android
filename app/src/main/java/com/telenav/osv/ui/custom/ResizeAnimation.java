package com.telenav.osv.ui.custom;

import java.lang.String;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import com.telenav.osv.utils.Log;

/**
 * an animation for resizing the view.
 * Created by Kalman on 16/01/2017.
 */
public class ResizeAnimation extends Animation {

    public final static String TAG = "ResizeAnimation";
    private View mView;
    private final float mToHeight;
    private final float mFromHeight;

    private final float mToWidth;
    private final float mFromWidth;

    public ResizeAnimation(View v, float fromWidth, float fromHeight, float toWidth, float toHeight) {
        mToHeight = toHeight;
        mToWidth = toWidth;
        mFromHeight = fromHeight;
        mFromWidth = fromWidth;
        mView = v;
        setDuration(300);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        if (!hasEnded()) {
            float height =
                    (mToHeight - mFromHeight) * interpolatedTime + mFromHeight;
            float width = (mToWidth - mFromWidth) * interpolatedTime + mFromWidth;
            ViewGroup.LayoutParams p = mView.getLayoutParams();
            p.height = (int) height;
            p.width = (int) width;
//            Log.d(TAG, "applyTransformation: setting interpolated size " + width + " x " + height);
            mView.requestLayout();
        }
    }
}