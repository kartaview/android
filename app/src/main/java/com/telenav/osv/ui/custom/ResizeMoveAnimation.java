package com.telenav.osv.ui.custom;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * an animation for resizing the view.
 * Created by Kalman on 16/01/2017.
 */
public class ResizeMoveAnimation extends Animation {

    public final static String TAG = "ResizeMoveAnimation";

    private final float mToHeight;

    private final float mFromHeight;

    private final float mToWidth;

    private final float mFromWidth;

    private final float mToPaddingTop;

    private final float mToPaddingLeft;

    private final float mFromPaddingTop;

    private final float mFromPaddingLeft;

    private final float mToPaddingBottom;

    private final float mToPaddingRight;

    private final float mFromPaddingBottom;

    private final float mFromPaddingRight;

    private View mView;

    public ResizeMoveAnimation(View v, float fromWidth, float fromHeight, float toWidth, float toHeight
            , float fromPaddingTop, float fromPaddingLeft, float fromPaddingBottom, float fromPaddingRight
            , float toPaddingTop, float toPaddingLeft, float toPaddingBottom, float toPaddingRight) {
        mToHeight = toHeight;
        mToWidth = toWidth;
        mFromHeight = fromHeight;
        mFromWidth = fromWidth;
        mToPaddingTop = toPaddingTop;
        mToPaddingLeft = toPaddingLeft;
        mFromPaddingTop = fromPaddingTop;
        mFromPaddingLeft = fromPaddingLeft;
        mToPaddingBottom = toPaddingBottom;
        mToPaddingRight = toPaddingRight;
        mFromPaddingBottom = fromPaddingBottom;
        mFromPaddingRight = fromPaddingRight;
        mView = v;
        setDuration(300);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        if (!hasEnded()) {
            float height = (mToHeight - mFromHeight) * interpolatedTime + mFromHeight;
            float width = (mToWidth - mFromWidth) * interpolatedTime + mFromWidth;
            float topPadding = (mToPaddingTop - mFromPaddingTop) * interpolatedTime + mFromPaddingTop;
            float leftPadding = (mToPaddingLeft - mFromPaddingLeft) * interpolatedTime + mFromPaddingLeft;
            float bottomPadding = (mToPaddingBottom - mFromPaddingBottom) * interpolatedTime + mFromPaddingBottom;
            float rightPadding = (mToPaddingRight - mFromPaddingRight) * interpolatedTime + mFromPaddingRight;
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) mView.getLayoutParams();
            p.height = (int) height;
            p.width = (int) width;
            p.topMargin = (int) topPadding;
            p.leftMargin = (int) leftPadding;
            p.bottomMargin = (int) bottomPadding;
            p.rightMargin = (int) rightPadding;
//            Log.d(TAG, "applyTransformation:" + mView.toString() + " setting interpolated size " + width + " x " + height);
            mView.requestLayout();
        }
    }
}