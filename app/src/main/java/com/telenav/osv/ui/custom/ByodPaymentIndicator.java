package com.telenav.osv.ui.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import com.google.common.base.Strings;
import com.telenav.osv.R;
import com.telenav.osv.utils.Utils;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringDef;

/**
 * Visualizing the score for the recording
 */
@SuppressWarnings("SuspiciousNameCombination")
public class ByodPaymentIndicator extends View {

    public static final String TAG = "ScoreView";

    private static final int FONT_SIZE_SMALL = 12;

    private static final int FONT_SIZE_LARGE = 18;

    private static final int STATE_HIDDEN = 1;

    private static final int STATE_DOT = 3;

    private static final int STATE_EXTENDED = 5;

    private boolean mDrawTexts = true;

    private boolean mActive;

    private boolean displayingDrawable;

    private Drawable displayedDrawable = null;

    private Paint mPaintTextLarge;

    private Paint mPaintTextSmall;

    private Paint mPaintLarge;

    private Paint mPaintLargeOutline;

    private Paint mPaintDot;

    private Paint mPaintDotOutline;

    private int mColorBlue;

    private int mColorLightBlue;

    private int mColorGray;

    private int mColorLightGray;

    private int mColorYellow;

    private int mColorLightYellow;

    private float mOutlineWidth;

    private String mValueText = "0";

    private int mMultiplier = 0;

    private int mCurrentState = STATE_HIDDEN;

    private float mWidth;

    private float mHeight;

    private RectF leftArc;

    private RectF rectangle;

    private RectF rightArc;

    private float oneDip = 0;

    private String mMultiplierPrefix = "x";

    private String mMultiplierText = "0";

    @ScoreSuffix
    private String mValueSuffix = ScoreSuffix.POINTS; //points as default

    private int mPrefixX;

    private int mPrefixY;

    private int mMultiplierY;

    private int mMultiplierX;

    private int mScoreY;

    private int mScoreX;

    private int mScoreSuffixY;

    private int mScoreSuffixX;

    private boolean displayBubble;

    private String mDefaultValue;

    private int currentSize;

    public ByodPaymentIndicator(Context context) {
        super(context);
        initialize();
    }

    public ByodPaymentIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public ByodPaymentIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    @SuppressWarnings("unused")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ByodPaymentIndicator(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mPaintLargeOutline.setStrokeWidth(mOutlineWidth);
        mPaintDotOutline.setStrokeWidth(mOutlineWidth);

        float left = 0;
        float top = 0;
        float radius = mHeight / 2;

        float centerY = mHeight / 2;
        leftArc.set(left, centerY - radius, left + 2 * radius, centerY + radius);
        rectangle.set(left + radius, top, mWidth - radius, mHeight);
        rightArc.set(mWidth - 2 * radius, centerY - radius, mWidth, centerY + radius);

        canvas.drawRect(rectangle, mPaintLarge);
        canvas.drawArc(rightArc, 270, 180, false, mPaintLarge);
        if (displayBubble || displayingDrawable) {
            canvas.drawArc(leftArc, 90, 360, false, mPaintDot);
        } else {
            canvas.drawArc(leftArc, 90, 180, false, mPaintDot);
        }

        rightArc.inset(mOutlineWidth / 2, mOutlineWidth / 2);
        rectangle.inset(0, mOutlineWidth / 2);
        leftArc.inset(mOutlineWidth / 2, mOutlineWidth / 2);

        canvas.drawLine(rectangle.left, rectangle.top, rectangle.right, rectangle.top, mPaintLargeOutline);
        canvas.drawLine(rectangle.left, rectangle.bottom, rectangle.right, rectangle.bottom, mPaintLargeOutline);
        canvas.drawArc(rightArc, 270, 180, false, mPaintLargeOutline);
        if (displayBubble || displayingDrawable) {
            canvas.drawArc(leftArc, 90, 360, false, mPaintDotOutline);
        } else {
            canvas.drawArc(leftArc, 90, 180, false, mPaintDotOutline);
        }
        if (displayingDrawable && displayedDrawable != null) {
            displayedDrawable.setBounds(0, 0, (int) mHeight, (int) mHeight);
            displayedDrawable.draw(canvas);
        }
        if (!displayingDrawable && displayBubble) {
            canvas.drawText(mMultiplierPrefix, mPrefixX, mPrefixY, mPaintTextSmall);
            canvas.drawText(mMultiplierText, mMultiplierX, mMultiplierY, mPaintTextLarge);
        }
        if (mDrawTexts) {
            canvas.drawText(mValueText, mScoreX, mScoreY, mPaintTextLarge);
            if (!displayingDrawable) {
                canvas.drawText(mValueSuffix, mScoreSuffixX, mScoreSuffixY, mPaintTextSmall);
            }
        }
        super.onDraw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = (int) getHeightForState(mCurrentState);
        int desiredWidth = (int) getWidthForState(mCurrentState);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        //Measure Width
        if (widthMode == MeasureSpec.EXACTLY) {
            //Must be this size
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            width = Math.min(desiredWidth, widthSize);
        } else {
            //Be whatever you want
            width = desiredWidth;
        }

        //Measure Height
        if (heightMode == MeasureSpec.EXACTLY) {
            //Must be this size
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            height = Math.min(desiredHeight, heightSize);
        } else {
            //Be whatever you want
            height = desiredHeight;
        }

        mHeight = height;
        mWidth = width;
        mOutlineWidth = mHeight / 14.666f;
        //MUST CALL THIS
        setMeasuredDimension(width, height);
        invalidate();
    }

    public void setDefaultValue(@Nullable String defaultValue) {
        mDefaultValue = defaultValue;
    }

    public boolean isActive() {
        return mActive;
    }

    //    ===========================================================================================================================
    public void showText() {
        if (!mActive) {
            setDrawableDisplayed(false);
            currentSize = 0;
            mActive = true;
            if (mDefaultValue != null) {
                mValueText = mDefaultValue;
            }
            transitionToState(mCurrentState, STATE_EXTENDED);
            requestLayout();
            invalidate();
        }
    }

    //    ===========================================================================================================================
    public void showTextWithInitialValue(@Nullable String payRate) {
        if (!mActive) {
            setDrawableDisplayed(false);
            currentSize = 0;
            mActive = true;
            if (mDefaultValue != null) {
                mValueText = payRate;
            }
            transitionToState(mCurrentState, STATE_EXTENDED);
            requestLayout();
            invalidate();
        }
    }

    public void showDrawable(@DrawableRes int drawable) {
        showDrawable(getResources().getDrawable(drawable, null), null);
    }

    public void showDrawable(@DrawableRes int drawable, @Nullable String description) {
        showDrawable(getResources().getDrawable(drawable, null), description);
    }

    public void showDrawable(@NonNull Drawable drawable, @Nullable String description) {
        currentSize = 0;
        setDrawableDisplayed(true);
        displayedDrawable = drawable;
        mValueText = Strings.nullToEmpty(description);
        mActive = true;
        if (Strings.isNullOrEmpty(mValueText)) {
            transitionToState(mCurrentState, STATE_DOT);
        } else {
            transitionToState(mCurrentState, STATE_EXTENDED);
        }
        requestLayout();
        invalidate();
    }

    public void hide() {
        if (mActive) {
            mActive = false;
            transitionToState(mCurrentState, STATE_HIDDEN);
            requestLayout();
            invalidate();
        }
    }

    public void updateScore(@Nullable String score) {
        //displaying text, not a drawable now
        setDrawableDisplayed(false);
        mValueText = Strings.nullToEmpty(score);
        if (mCurrentState == STATE_EXTENDED) {
            int newSize = measureElements();
            if (newSize != currentSize) {
                requestLayout();
            }
            currentSize = newSize;
            invalidate();
        } else {
            transitionToState(mCurrentState, STATE_EXTENDED);
        }
    }

    public void setMultiplier(int multiplier) {
        setDrawableDisplayed(false);
        if (mMultiplier == multiplier || multiplier < 1) {
            return;
        }
        mMultiplier = multiplier;
        requestLayout();
        invalidate();
    }

    public void setScoreSuffix(@ScoreSuffix String suffix) {
        setDrawableDisplayed(false);
        this.mValueSuffix = suffix;
    }

    public void setDisplayBubble(boolean displayBubble) {
        setDrawableDisplayed(false);
        this.displayBubble = displayBubble;
    }

    private void setDrawableDisplayed(boolean displayed) {
        if (displayingDrawable != displayed) {
            displayingDrawable = displayed;
            updateOutlineColors();
        }
    }

    private void initialize() {
        mHeight = (int) Utils.dpToPx(getContext(), 48);
        mOutlineWidth = mHeight / 14.666f;
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
                //                Log.d(TAG, "getWidthForState: DOT");
                return (int) Utils.dpToPx(getContext(), 48);
            case STATE_EXTENDED:
                //                Log.d(TAG, "getWidthForState: EXTENDED");
                return measureElements();
            default:
            case STATE_HIDDEN:
                //                Log.d(TAG, "getWidthForState: HIDDEN");
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

    private void transitionToState(final int current, final int next) {
        final Runnable changeState = () -> mCurrentState = next;
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
            case STATE_DOT << STATE_HIDDEN:
                hideAnimation(changeState);
                break;
            case STATE_EXTENDED << STATE_HIDDEN:
                final Runnable collapsedToHiddenRunnable = () -> {
                    mValueText = "";
                    displayedDrawable = null;
                    changeState.run();
                };
                //first switch from extended to collapsed, then switch from collapsed --> hidden
                retractAnimation(collapsedToHiddenRunnable);
                break;
            case STATE_DOT << STATE_EXTENDED:
                extendAnimation(changeState);
                break;
            case STATE_DOT << STATE_DOT:
                //NO OP
                changeState.run();
                break;
            case STATE_EXTENDED << STATE_EXTENDED:
                retractAnimation(() -> {
                            //the next updateOutlineColors (after the animation, see the animlistener class default action) will commit to dot type
                            mCurrentState = STATE_DOT;
                            extendAnimation(changeState);
                        }
                );
                break;
            default:
                Log.d(TAG, "Unknown transition requested. current:" + current + " next:" + next);
        }
    }

    private void updateOutlineColors() {
        if (displayingDrawable) {
            mPaintDot.setColor(mColorYellow);
            mPaintDotOutline.setColor((mColorLightYellow));
            mPaintLarge.setColor(mColorGray);
            mPaintLargeOutline.setColor(mColorLightGray);
        } else {
            mPaintDot.setColor(mColorBlue);
            mPaintDotOutline.setColor((mColorLightBlue));
            mPaintLarge.setColor(mColorBlue);
            mPaintLargeOutline.setColor(mColorLightBlue);
        }
    }

    //    ===========================================================================================================================

    private int measureElements() {
        //put down the cursor then move it to the right
        float centerY = (int) (mHeight / 2f);

        int cursor;
        //text inside the bubble in the left of the score view
        if (displayBubble) {
            //if we don't display the multiplier bubble, there's no need to compute the score multiplier & prefix related stuff

            mMultiplierText = Integer.toString(mMultiplier);

            Rect xBounds = new Rect();
            mPaintTextSmall.getTextBounds(mMultiplierPrefix, 0, mMultiplierPrefix.length(), xBounds);
            Rect mulBounds = new Rect();
            mPaintTextLarge.getTextBounds(mMultiplierText, 0, mMultiplierText.length(), mulBounds);

            cursor = (int) (mHeight / 2f - (xBounds.width() + mulBounds.width()) / 2f - oneDip * 3f / 2f);
            mPrefixY = (int) (centerY + xBounds.height() / 2f);
            mPrefixX = (int) (cursor + xBounds.width() / 2f);
            cursor = cursor + xBounds.width() + (int) (oneDip * 3f);
            mMultiplierY = (int) (centerY + mulBounds.height() / 2f);
            mMultiplierX = (int) (cursor + mulBounds.width() / 2f);
        }

        //text outside of the bubble
        Rect valueTextBounds = new Rect();
        mPaintTextLarge.getTextBounds(mValueText, 0, mValueText.length(), valueTextBounds);
        Rect valueSuffixTextBounds = new Rect();
        mPaintTextSmall.getTextBounds(mValueSuffix, 0, mValueSuffix.length(), valueSuffixTextBounds);
        boolean displayOnlyDrawable = valueTextBounds.width() == 0 && displayingDrawable;

        if (displayBubble || displayingDrawable) {
            // the height acts the role of the bubble diameter here...
            cursor = (int) (mHeight + (displayOnlyDrawable ? 0 : oneDip * 5f));
        } else {
            cursor = (int) (mOutlineWidth + oneDip * 10f);
        }

        mScoreY = (int) (centerY + valueTextBounds.height() / 2f);
        mScoreX = (int) (cursor + valueTextBounds.width() / 2f);

        cursor = (int) (cursor + valueTextBounds.width() + (displayOnlyDrawable ? 0 : oneDip * 5f));

        if (!displayingDrawable) {
            mScoreSuffixY = (int) (centerY + valueSuffixTextBounds.height() / 2f);
            mScoreSuffixX = (int) (cursor + valueSuffixTextBounds.width() / 2f);
            cursor = (int) (cursor + valueSuffixTextBounds.width() + oneDip * 10);
        } else {
            cursor = (int) (cursor + (displayOnlyDrawable ? 0 : oneDip * 10));
        }

        return Math.max(cursor, (int) mHeight);
    }

    private void extendAnimation(final Runnable runnable) {
        //        Log.d(TAG, "extendAnimation: ");
        ResizeAnimation extend =
                new ResizeAnimation(this, getWidthForState(STATE_DOT), getHeightForState(STATE_DOT), getWidthForState(STATE_EXTENDED),
                        getHeightForState(STATE_EXTENDED));
        //        extend.setFillAfter(true);
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
        //        Log.d(TAG, "retractAnimation: ");

        ResizeAnimation retract =
                new ResizeAnimation(this, getWidthForState(STATE_EXTENDED), getHeightForState(STATE_EXTENDED), getWidthForState(STATE_DOT),
                        getHeightForState(STATE_DOT));
        //        retract.setFillAfter(true);
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
        //        Log.d(TAG, "appearAnimation: to " + mHeight + "x" + mHeight);
        ResizeAnimation animation = new ResizeAnimation(this, 0, 0, getWidthForState(STATE_DOT), getHeightForState(STATE_DOT));
        animation.setDuration(300);
        //        animation.setFillAfter(true);
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

    private void hideAnimation(final Runnable endAction) {
        //        Log.d(TAG, "appearAnimation: to " + mHeight + "x" + mHeight);
        float widthForDot = getWidthForState(STATE_DOT);
        ResizeAnimation animation = new ResizeAnimation(this, widthForDot, widthForDot, 0, 0);
        animation.setDuration(300);
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

    @StringDef
    public @interface ScoreSuffix {
        String POINTS = "pts",
                PER_KM = "/km";
    }

    private abstract class AnimationListener implements Animation.AnimationListener {

        public static final String TAG = "AnimationListener";

        @Override
        public void onAnimationStart(Animation animation) {
            mDrawTexts = false;
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            mDrawTexts = true;
            ViewGroup.LayoutParams lp = getLayoutParams();
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            setLayoutParams(lp);
            measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            requestLayout();
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }
}