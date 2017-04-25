package com.telenav.osv.ui.custom;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.RelativeLayout;
import com.telenav.osv.R;
import com.telenav.osv.utils.Log;

/**
 *
 * Created by Dmitri on 22/05/2015.
 */
public class RevealRelativeLayout extends RelativeLayout {
    public static final int STATE_REVEALED = 0, STATE_HIDDEN = 1;

    private static final int DEFAULT_COLOR = Color.WHITE;

    private static final int DEFAULT_DURATION = 300;

    private static final String TAG = "RevealRelativeLayout";

    private Path mPath;

    private int mState;

    private int mColor;

    private Point point;

    {
        point = new Point(-1, -1);
        mPath = new Path();
        setBackgroundColor(Color.TRANSPARENT);
        setClickable(true);
    }

    public RevealRelativeLayout(Context context) {
        super(context);
    }

    public RevealRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        readXmlAttributes(context, attrs);
    }

    public RevealRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        readXmlAttributes(context, attrs);
    }

    private void readXmlAttributes(Context context, AttributeSet attrs) {
        // Size will be used for width and height of the icon, plus the space in between
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RevealRelativeLayout, 0, 0);
        try {
            mColor = a.getColor(R.styleable.RevealRelativeLayout_backgroundColor, DEFAULT_COLOR);
        } finally {
            a.recycle();
        }
    }

    public void setBackgroundColor(int argb) {
        mColor = argb;
        invalidate();
    }

    private void updateCircle(Point point, float fraction) {
        float f = mState == STATE_HIDDEN ? fraction : 1 - fraction;
        float diffX = Math.max(getWidth() - point.x, point.x), diffY = Math.max(getHeight() - point.y, point.y);
        float mRadius = (float) (Math.sqrt(Math.pow(diffX, 2) + Math.pow(diffY, 2)) * f);
        mPath.reset();
        mPath.addCircle(point.x, point.y, mRadius, Path.Direction.CW);
        invalidate();
    }

    public void reveal() {
        reveal(point, DEFAULT_DURATION);
    }

    public void reveal(final Point point, int duration) {
        this.point.x = point.x;
        this.point.y = point.y;
        Log.d(TAG, "reveal: x = " + point.x + ", y = " + point.y);
        mState = mState == STATE_HIDDEN ? STATE_REVEALED : STATE_HIDDEN;
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(duration);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                updateCircle(point, valueAnimator.getAnimatedFraction());
            }
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                setVisibility(mState == STATE_HIDDEN ? VISIBLE : GONE);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        animator.start();
    }

    public void revealThenHide(final Point point, final int duration) {
        this.point.x = point.x;
        this.point.y = point.y;
        mState = mState == STATE_HIDDEN ? STATE_REVEALED : STATE_HIDDEN;
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(duration);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                updateCircle(point, valueAnimator.getAnimatedFraction());
            }
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                setVisibility(mState == STATE_HIDDEN ? VISIBLE : GONE);
                reveal(new Point(0, 0), duration);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        animator.start();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mState == STATE_REVEALED;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (point.x == -1 && point.y == -1) {
            point.x = getWidth() / 2;
            point.y = getHeight() / 2;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed) {
            invalidate();
        }
        super.onLayout(changed, l, t, r, b);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        return super.drawChild(canvas, child, drawingTime);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.clipPath(mPath);
        canvas.drawColor(mColor);
        super.dispatchDraw(canvas);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable parcelable = super.onSaveInstanceState();
        if (parcelable == null) {
            parcelable = new Bundle();
        }

        LayoutState savedState = new LayoutState(parcelable);
        savedState.flagState = mState;
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof LayoutState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        LayoutState ss = (LayoutState) state;
        mState = ss.flagState;
        if (mState != STATE_HIDDEN && mState != STATE_REVEALED) {
            mState = STATE_HIDDEN;
        }

        super.onRestoreInstanceState(ss.getSuperState());
    }

    public boolean isVisible() {
        return mState == STATE_REVEALED;
    }

    /**
     * Internal saved state
     */
    static class LayoutState extends BaseSavedState {
        public static final Creator<LayoutState> CREATOR =
                new Creator<LayoutState>() {
                    public LayoutState createFromParcel(Parcel in) {
                        return new LayoutState(in);
                    }

                    public LayoutState[] newArray(int size) {
                        return new LayoutState[size];
                    }
                };

        private int flagState;

        LayoutState(Parcelable superState) {
            super(superState);
        }

        private LayoutState(Parcel in) {
            super(in);
            this.flagState = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.flagState);
        }
    }
}
