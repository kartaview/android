package com.telenav.osv.ui.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import com.telenav.osv.R;
import com.telenav.osv.utils.Utils;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

/**
 * Visualizing the score for the recording
 * Created by Kalman on 13/01/2017.
 */
@SuppressWarnings("SuspiciousNameCombination")
public class ScoreIndicator extends View {

    public final static String TAG = "ScoreView";

    private final static int FONT_SIZE_SMALL = 12;

    private final static int FONT_SIZE_LARGE = 18;

    private static final int STATE_HIDDEN = 1;

    private static final int STATE_DOT = 3;

    private static final int STATE_EXTENDED = 5;

    private boolean mDrawTexts = true;

    private boolean mDrawMultiplierTexts = true;

    private boolean mObdConnected;

    private boolean mActive;

    private boolean mCoverageAvailable;

    private boolean mHadCoverage;

    private Paint mPaintTextLarge;

    private Paint mPaintTextSmall;

    private Paint mPaintLarge;

    private Paint mPaintLargeOutline;

    private Paint mPaintDot;

    private Paint mPaintDotOutline;

    private VectorDrawableCompat mNoWifiVector;

    private int mColorBlue;

    private int mColorLightBlue;

    private int mColorGray;

    private int mColorLightGray;

    private int mColorYellow;

    private int mColorLightYellow;

    private float mOutlineWidth;

    private int mPoints = 0;

    private int mMultiplier = 1;

    private int mState = STATE_HIDDEN;

    private float mWidth;

    private float mHeight;

    private RectF leftArc;

    private RectF rectangle;

    private RectF rightArc;

    private boolean mDrawIcon;

    private float oneDip = 0;

    private String mMultiplierPrefix = "x";

    private String mPointsSuffix = "pts";

    private String mMultiplierText = "1";

    private String mPointsText = "0";

    private int mPrefixX;

    private int mPrefixY;

    private int mMultiplierY;

    private int mMultiplierX;

    private int mPointsY;

    private int mPointsX;

    private int mSuffixY;

    private int mSuffixX;

    private int currentSize;

    public ScoreIndicator(Context context) {
        super(context);
        initialize();
    }

    public ScoreIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public ScoreIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    @SuppressWarnings("unused")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ScoreIndicator(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = (int) mWidth;
        int height = (int) mHeight;
        //        Log.d(TAG, "onDraw: width = " + width + ", height = " + height);
        mOutlineWidth = height / 14.666f;
        mPaintLargeOutline.setStrokeWidth(mOutlineWidth);
        mPaintDotOutline.setStrokeWidth(mOutlineWidth);
        float left = 0;
        float top = 0;
        float radius = height / 2;

        float center_y = height / 2;
        leftArc.set(left, center_y - radius, left + 2 * radius, center_y + radius);
        rectangle.set(left + radius, top, (float) width - radius, (float) height);
        rightArc.set((float) width - 2 * radius, center_y - radius, (float) width, center_y + radius);

        canvas.drawRect(rectangle, mPaintLarge);
        canvas.drawArc(rightArc, 270, 180, false, mPaintLarge);
        canvas.drawArc(leftArc, 90, 360, false, mPaintDot);

        leftArc.inset(mOutlineWidth / 2, mOutlineWidth / 2);
        rectangle.inset(0, mOutlineWidth / 2);
        rightArc.inset(mOutlineWidth / 2, mOutlineWidth / 2);

        canvas.drawLine(rectangle.left, rectangle.top, rectangle.right, rectangle.top, mPaintLargeOutline);
        canvas.drawLine(rectangle.left, rectangle.bottom, rectangle.right, rectangle.bottom, mPaintLargeOutline);
        canvas.drawArc(rightArc, 270, 180, false, mPaintLargeOutline);
        canvas.drawArc(leftArc, 90, 360, false, mPaintDotOutline);
        if (mDrawIcon) {
            mNoWifiVector.setBounds(0, 0, height, height);
            mNoWifiVector.draw(canvas);
        }
        if (!mDrawIcon && mDrawMultiplierTexts) {
            canvas.drawText(mMultiplierPrefix, mPrefixX, mPrefixY, mPaintTextSmall);
            canvas.drawText(mMultiplierText, mMultiplierX, mMultiplierY, mPaintTextLarge);
        }
        if (mDrawTexts) {
            canvas.drawText(mPointsText, mPointsX, mPointsY, mPaintTextLarge);
            canvas.drawText(mPointsSuffix, mSuffixX, mSuffixY, mPaintTextSmall);
        }
        super.onDraw(canvas);
    }

    //    ===========================================================================================================================

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = (int) getHeightForState(mState);
        int desiredWidth = (int) getWidthForState(mState);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        //Measure Width
        if (widthMode == MeasureSpec.EXACTLY) {
            //            Log.d(TAG, "onMeasure: width exactly " + widthSize);
            //Must be this size
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            //            Log.d(TAG, "onMeasure: width at most");
            //Can't be bigger than...
            width = Math.min(desiredWidth, widthSize);
        } else {
            //            Log.d(TAG, "onMeasure: width unspecified");
            //Be whatever you want
            width = desiredWidth;
        }

        //Measure Height
        if (heightMode == MeasureSpec.EXACTLY) {
            //            Log.d(TAG, "onMeasure: height exactly " + heightSize);
            //Must be this size
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            //            Log.d(TAG, "onMeasure: height at most");
            //Can't be bigger than...
            height = Math.min(desiredHeight, heightSize);
        } else {
            //            Log.d(TAG, "onMeasure: height unspecified");
            //Be whatever you want
            height = desiredHeight;
        }
        mHeight = height;
        mWidth = width;
        //MUST CALL THIS
        setMeasuredDimension(width, height);
        invalidate();
    }

    public void setScore(int score) {
        if (mPoints == score) {
            return;
        }
        mPoints = score;
        int newSize = measureTexts();
        if (newSize != currentSize || getWidth() != newSize) {
            currentSize = newSize;
            requestLayout();
        }
        invalidate();
    }

    public void setMultiplier(int multiplier) {
        if ((mMultiplier == multiplier && computeMultiplierText(multiplier).equals(mMultiplierText)) || multiplier < 1) {
            return;
        }
        mMultiplier = multiplier;
        int newSize = measureTexts();
        if (newSize != currentSize || getWidth() != newSize) {
            currentSize = newSize;
            requestLayout();
        }
        invalidate();
    }

    public void setCoverageAvailable(boolean available) {
        if (mCoverageAvailable != available) {
            mCoverageAvailable = available;
            mHadCoverage = mHadCoverage || available;
            onEvent();
        }
    }

    public void reset() {
        mHadCoverage = false;
        mCoverageAvailable = false;
        mObdConnected = false;
        mActive = false;
        mDrawTexts = false;
        mDrawMultiplierTexts = false;
        mState = STATE_HIDDEN;
        mMultiplierPrefix = "x";
        mMultiplier = 1;
        mMultiplierText = "1";
        mDrawIcon = false;
        mPoints = 0;
        requestLayout();
        invalidate();
    }

    public void setObdConnected(boolean connected) {
        if (mObdConnected != connected) {
            mObdConnected = connected;
            invalidate();
            onEvent();
        }
    }

    public void setActive(boolean active) {
        if (mActive != active) {
            this.mActive = active;
            onEvent();
            invalidate();
        }
    }

    private void initialize() {
        mHeight = (int) Utils.dpToPx(getContext(), 48);
        rectangle = new RectF();
        rightArc = new RectF();
        leftArc = new RectF();
        mColorBlue = getResources().getColor(R.color.default_blue);
        mColorLightBlue = getResources().getColor(R.color.score_background_light_blue);
        mColorGray = getResources().getColor(R.color.default_gray);
        mColorLightGray = getResources().getColor(R.color.score_background_light_gray);
        mColorYellow = getResources().getColor(R.color.default_yellow);
        mColorLightYellow = getResources().getColor(R.color.default_yellow_lighter);
        mPaintLarge = new Paint();
        mPaintLarge.setColor(mColorBlue);
        mPaintLarge.setStrokeWidth(0);
        mPaintLarge.setStyle(Paint.Style.FILL);
        mPaintLargeOutline = new Paint();
        mPaintLargeOutline.setColor(mColorLightBlue);
        mOutlineWidth = 15;
        mPaintLargeOutline.setStrokeWidth(mOutlineWidth);
        mPaintLargeOutline.setStyle(Paint.Style.STROKE);
        mPaintDot = new Paint();
        mPaintDot.setColor(mColorBlue);
        mPaintDot.setStrokeWidth(0);
        mPaintDot.setStyle(Paint.Style.FILL);
        mPaintDotOutline = new Paint();
        mPaintDotOutline.setColor(mColorLightBlue);
        mPaintDotOutline.setStrokeWidth(mOutlineWidth);
        mPaintDotOutline.setStyle(Paint.Style.STROKE);
        mNoWifiVector = VectorDrawableCompat.create(getResources(), R.drawable.vector_no_wifi, null);

        mPaintTextLarge = new Paint();
        mPaintTextLarge.setColor(Color.WHITE);
        mPaintTextLarge.setTextAlign(Paint.Align.CENTER);
        mPaintTextLarge.setAntiAlias(true);
        mPaintTextLarge.setTypeface(Typeface.SANS_SERIF);
        mPaintTextLarge.setTextSize(Utils.dpToPx(getContext(), FONT_SIZE_LARGE));
        mPaintTextSmall = new Paint();
        mPaintTextSmall.setColor(Color.WHITE);
        mPaintTextSmall.setTextAlign(Paint.Align.CENTER);
        mPaintTextLarge.setAntiAlias(true);
        mPaintTextLarge.setTypeface(Typeface.SANS_SERIF);
        mPaintTextSmall.setTextSize(Utils.dpToPx(getContext(), FONT_SIZE_SMALL));
        oneDip = Utils.dpToPx(getContext(), 1);
    }

    private float getWidthForState(int state) {
        switch (state) {
            case STATE_DOT:
                return (int) Utils.dpToPx(getContext(), 48);
            case STATE_EXTENDED:
                return measureTexts();
            default:
            case STATE_HIDDEN:
                return 0;
        }
    }

    private float getHeightForState(int state) {
        switch (state) {
            case STATE_DOT:
            case STATE_EXTENDED:
                return (int) Utils.dpToPx(getContext(), 48);
            default:
            case STATE_HIDDEN:
                return 0;
        }
    }

    private void onEvent() {
        if (mCoverageAvailable || mHadCoverage) {
            if (mActive) {
                transitionToState(mState, STATE_EXTENDED);
                return;
            } else {
                transitionToState(mState, STATE_DOT);
                return;
            }
        }
        requestLayout();
    }

    private void transitionToState(int current, final int next) {
        final Runnable changeState = () -> mState = next;
        switch (current << next) {
            case STATE_HIDDEN << STATE_DOT:
                appearAnimation(changeState);
                break;
            case STATE_EXTENDED << STATE_DOT:
                retractAnimation(changeState);
                break;
            case STATE_HIDDEN << STATE_EXTENDED:
                appearAnimation(() -> extendAnimation(changeState));
                break;
            case STATE_DOT << STATE_EXTENDED:
                extendAnimation(changeState);
                break;
            case STATE_DOT << STATE_DOT:
                commitChanges();
                changeState.run();
                break;
            case STATE_EXTENDED << STATE_EXTENDED:
                retractAnimation(() -> {
                    mState = STATE_DOT;//the next commitChanges (after the animation, see the animlistener class default action) will commit to dot type
                    extendAnimation(changeState);
                });
                break;
        }
    }

    //    ===========================================================================================================================

    private void commitChanges() {
        if (!mCoverageAvailable) {
            mPaintDot.setColor(mColorYellow);
            mPaintDotOutline.setColor((mColorLightYellow));
            mPaintLarge.setColor(mColorGray);
            mPaintLargeOutline.setColor(mColorLightGray);
            mDrawIcon = true;
        } else {
            mPaintDot.setColor(mColorBlue);
            mPaintDotOutline.setColor((mColorLightBlue));
            mPaintLarge.setColor(mColorBlue);
            mPaintLargeOutline.setColor(mColorLightBlue);
            mDrawIcon = false;
        }
    }

    private int measureTexts() {
        mMultiplierText = computeMultiplierText(mMultiplier);
        mPointsText = mPoints + "";

        Rect xBounds = new Rect();
        mPaintTextSmall.getTextBounds(mMultiplierPrefix, 0, mMultiplierPrefix.length(), xBounds);
        Rect mulBounds = new Rect();
        mPaintTextLarge.getTextBounds(mMultiplierText, 0, mMultiplierText.length(), mulBounds);

        int length = mPointsText.length();
        StringBuilder pointsPlaceHolder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            pointsPlaceHolder.append("0");
        }
        Rect pointsBounds = new Rect();
        mPaintTextLarge.getTextBounds(pointsPlaceHolder.toString(), 0, length, pointsBounds);
        Rect ptsBounds = new Rect();
        mPaintTextSmall.getTextBounds(mPointsSuffix, 0, mPointsSuffix.length(), ptsBounds);

        //put down the cursor then move it to the right
        float centerY = (int) (mHeight / 2f);
        int cursor = (int) (mHeight / 2f - (xBounds.width() + mulBounds.width()) / 2f - oneDip * 3f / 2f);
        mPrefixY = (int) (centerY + xBounds.height() / 2f);
        mPrefixX = (int) (cursor + xBounds.width() / 2f);
        cursor = cursor + xBounds.width() + (int) (oneDip * 3f);
        mMultiplierY = (int) (centerY + mulBounds.height() / 2f);
        mMultiplierX = (int) (cursor + mulBounds.width() / 2f);
        cursor = (int) (mHeight + oneDip * 5f);
        mPointsY = (int) (centerY + pointsBounds.height() / 2f);
        mPointsX = (int) (cursor + pointsBounds.width() / 2f);
        cursor = (int) (cursor + pointsBounds.width() + oneDip * 5f);
        mSuffixY = (int) (centerY + ptsBounds.height() / 2f);
        mSuffixX = (int) (cursor + ptsBounds.width() / 2f);

        cursor = (int) (cursor + ptsBounds.width() + oneDip * 10);
        return cursor;
    }

    @NonNull
    private String computeMultiplierText(int multiplier) {
        return "" + ((mObdConnected ? 2 : 1) * multiplier);
    }

    private void extendAnimation(final Runnable runnable) {
        ResizeAnimation extend =
                new ResizeAnimation(this, getWidthForState(STATE_DOT), getHeightForState(STATE_DOT), getWidthForState(STATE_EXTENDED),
                        getHeightForState(STATE_EXTENDED));
        extend.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationEnd(Animation animation) {
                if (runnable != null) {
                    runnable.run();
                }
                super.onAnimationEnd(animation);
            }
        });
        extend.setDuration(300);
        startAnimation(extend);
    }

    private void retractAnimation(final Runnable endAction) {
        ResizeAnimation retract =
                new ResizeAnimation(this, getWidthForState(STATE_EXTENDED), getHeightForState(STATE_EXTENDED), getWidthForState(STATE_DOT),
                        getHeightForState(STATE_DOT));
        retract.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationEnd(Animation animation) {
                if (endAction != null) {
                    endAction.run();
                }
                super.onAnimationEnd(animation);
            }
        });
        retract.setDuration(300);
        startAnimation(retract);
    }

    private void appearAnimation(final Runnable endAction) {
        ResizeAnimation animation = new ResizeAnimation(this, 0, 0, getWidthForState(STATE_DOT), getHeightForState(STATE_DOT));
        animation.setDuration(300);
        mDrawMultiplierTexts = false;
        animation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationEnd(Animation animation) {
                if (endAction != null) {
                    endAction.run();
                }
                super.onAnimationEnd(animation);
            }
        });
        startAnimation(animation);
    }

    private abstract class AnimationListener implements Animation.AnimationListener {

        public static final String TAG = "AnimationListener";

        @Override
        public void onAnimationStart(Animation animation) {
            mDrawTexts = false;
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (!mDrawMultiplierTexts) {
                mDrawMultiplierTexts = true;
            }
            mDrawTexts = true;
            commitChanges();
            ViewGroup.LayoutParams lp = getLayoutParams();
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            setLayoutParams(lp);
            measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            requestLayout();
            measureTexts();
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }
}