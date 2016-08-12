package com.telenav.osv.external.glview;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Scroller;
import com.telenav.osv.external.model.Constants;
import com.telenav.osv.external.model.Photo;
import com.telenav.osv.external.model.RotateInertia;

/**
 * View class for photo display
 */
public class GLPhotoView extends GLSurfaceView {

    private static final int ANIMATION_INTERVAL = 10;

    private GLRenderer mRenderer = null;

    private GestureDetector mGestureDetector;

    private ScaleGestureDetector mScaleGestureDetector;

    private Scroller mScroller = null;

    private float mPrevX, mPrevY;

    private RotateInertia mRotateInertia = RotateInertia.INERTIA_0;

    /**
     * Constructor
     * @param context Context
     */
    public GLPhotoView(Context context) {
        this(context, null);

    }

    /**
     * Constructor
     * @param context Context
     * @param attrs Argument for resource
     */
    public GLPhotoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }


    /**
     * onTouchEvent Event listener
     * @param event Event object
     * @return Process continuation judgment value
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        boolean ret = false;
        mScaleGestureDetector.onTouchEvent(event);
        if (!mScaleGestureDetector.isInProgress()) {
            ret = mGestureDetector.onTouchEvent(event);
            if (!ret) {
                super.onTouchEvent(event);
            }
        }
        return ret;
    }


    private void initialize(Context context) {

        setEGLContextClientVersion(2);

        getHolder().setFormat(PixelFormat.TRANSLUCENT);

        mRenderer = new GLRenderer();
        setRenderer(mRenderer);

        setLongClickable(true);

        mGestureDetector = new GestureDetector(context, new GestureDetector.OnGestureListener() {
            final Handler handler = new Handler();

            private final int SWIPE_MAX_OF_PATH_X = 100;

            private final int SWIPE_MAX_OF_PATH_Y = 100;

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }

            @Override
            public void onShowPress(MotionEvent e) {
                return;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                return;
            }

            @Override
            public boolean onDown(MotionEvent e) {

                if (null != mScroller && !mScroller.isFinished()) {
                    mScroller.abortAnimation();
                    handler.removeCallbacksAndMessages(null);
                }

                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

                boolean ret = false;

                if (null != mScroller && !mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }

                if ((Math.abs(distanceX) > SWIPE_MAX_OF_PATH_X) || (Math.abs(distanceY) > SWIPE_MAX_OF_PATH_Y)) {
                    ret = false;
                } else {
                    float diffX = distanceX / Constants.ON_SCROLL_DIVIDER_X;
                    float diffY = distanceY / Constants.ON_SCROLL_DIVIDER_Y;

                    if (Math.abs(diffX) < Constants.THRESHOLD_SCROLL_X) {
                        diffX = 0.0f;
                    }
                    if (Math.abs(diffY) < Constants.THRESHOLD_SCROLL_Y) {
                        diffY = 0.0f;
                    }

                    if (null != mRenderer) {
                        mRenderer.rotate(diffX, -diffY);
                    }
                    ret = true;
                }

                return ret;
            }


            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

                mScroller.fling((int) e2.getX(), (int) e2.getY(), (int) velocityX, (int) velocityY, 0, getWidth(), 0, getHeight());
                mPrevX = e2.getX();
                mPrevY = e2.getY();

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mRotateInertia == RotateInertia.INERTIA_0) {
                            // do nothing
                        } else {
                            mScroller.computeScrollOffset();
                            float diffX = mScroller.getCurrX() - mPrevX;
                            float diffY = mScroller.getCurrY() - mPrevY;
                            mPrevX = mScroller.getCurrX();
                            mPrevY = mScroller.getCurrY();

                            if (mRotateInertia == RotateInertia.INERTIA_50) {
                                diffX = diffX / Constants.ON_FLING_DIVIDER_X_FOR_INERTIA_50;
                                diffY = diffY / Constants.ON_FLING_DIVIDER_Y_FOR_INERTIA_50;
                            } else {
                                diffX = diffX / Constants.ON_FLING_DIVIDER_X_FOR_INERTIA_100;
                                diffY = diffY / Constants.ON_FLING_DIVIDER_Y_FOR_INERTIA_100;
                            }
                            mRenderer.rotate(-diffX, diffY);

                            if (!mScroller.isFinished()) {
                                handler.postDelayed(this, ANIMATION_INTERVAL);
                            }
                        }
                    }
                });

                return true;
            }
        });

        mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {

                float scale = detector.getScaleFactor();

                if (null != mRenderer) {
                    mRenderer.scale(scale);
                }

                return true;
            }
        });

        mScroller = new Scroller(context);

        return;
    }


    /**
     * Texture setting method
     * @param thumbnail Photo object for texture
     */
    public void setTexture(Photo thumbnail) {
        mRenderer.setTexture(thumbnail);
        return;
    }


    /**
     * Inertia setting method
     * @param mRotateInertia Setting inertia value
     */
    public void setmRotateInertia(RotateInertia mRotateInertia) {
        this.mRotateInertia = mRotateInertia;
        return;
    }

}