package com.telenav.streetview.scalablevideoview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;

public class ScalableVideoView extends TextureView implements View.OnLayoutChangeListener {

    protected ScalableType mScalableType = ScalableType.NONE;

    private int mContentWidth;

    private int mContentHeight;

    public enum ScalableType {
        NONE,

        FIT_XY,
        FIT_START,
        FIT_CENTER,
        FIT_END,

        LEFT_TOP,
        LEFT_CENTER,
        LEFT_BOTTOM,
        CENTER_TOP,
        CENTER,
        CENTER_BOTTOM,
        RIGHT_TOP,
        RIGHT_CENTER,
        RIGHT_BOTTOM,

        LEFT_TOP_CROP,
        LEFT_CENTER_CROP,
        LEFT_BOTTOM_CROP,
        CENTER_TOP_CROP,
        CENTER_CROP,
        CENTER_BOTTOM_CROP,
        RIGHT_TOP_CROP,
        RIGHT_CENTER_CROP,
        RIGHT_BOTTOM_CROP,

        START_INSIDE,
        CENTER_INSIDE,
        END_INSIDE
    }

    public enum PivotPoint {
        LEFT_TOP,
        LEFT_CENTER,
        LEFT_BOTTOM,
        CENTER_TOP,
        CENTER,
        CENTER_BOTTOM,
        RIGHT_TOP,
        RIGHT_CENTER,
        RIGHT_BOTTOM
    }

    public ScalableVideoView(Context context) {
        this(context, null);
    }

    public ScalableVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScalableVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (attrs == null) {
            return;
        }

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.scaleStyle, 0, 0);
        if (a == null) {
            return;
        }

        int scaleType = a.getInt(R.styleable.scaleStyle_scalableType, ScalableType.NONE.ordinal());
        a.recycle();
        mScalableType = ScalableType.values()[scaleType];
        addOnLayoutChangeListener(this);
    }

    public void onVideoSizeChanged(int width, int height) {
        mContentWidth = width;
        mContentHeight = height;
        scaleVideoSize(width, height);
    }

    public void onSizeChanged() {
        if (mContentHeight != 0 && mContentWidth != 0) {
            scaleVideoSize(mContentWidth, mContentHeight);
        }
    }

    public void setScalableType(ScalableType scalableType) {
        mScalableType = scalableType;
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        scaleVideoSize(mContentWidth, mContentHeight);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (mContentHeight != 0 && mContentWidth != 0) {
            scaleVideoSize(mContentWidth, mContentHeight);
        }
    }

    private void scaleVideoSize(int videoWidth, int videoHeight) {
        if (videoWidth == 0 || videoHeight == 0) {
            return;
        }

        Size viewSize = new Size(getWidth(), getHeight());
        Size videoSize = new Size(videoWidth, videoHeight);
        ScaleManager scaleManager = new ScaleManager(viewSize, videoSize);
        Matrix matrix = scaleManager.getScaleMatrix(mScalableType);
        if (matrix != null) {
            setTransform(matrix);
        }
    }

    public class ScaleManager {

        private Size mViewSize;

        private Size mVideoSize;

        ScaleManager(Size viewSize, Size videoSize) {
            mViewSize = viewSize;
            mVideoSize = videoSize;
        }

        private Matrix getMatrix(float sx, float sy, float px, float py) {
            Matrix matrix = new Matrix();
            matrix.setScale(sx, sy, px, py);
            return matrix;
        }

        private Matrix getMatrix(float sx, float sy, PivotPoint pivotPoint) {
            switch (pivotPoint) {
                case LEFT_TOP:
                    return getMatrix(sx, sy, 0, 0);
                case LEFT_CENTER:
                    return getMatrix(sx, sy, 0, mViewSize.getHeight() / 2f);
                case LEFT_BOTTOM:
                    return getMatrix(sx, sy, 0, mViewSize.getHeight());
                case CENTER_TOP:
                    return getMatrix(sx, sy, mViewSize.getWidth() / 2f, 0);
                case CENTER:
                    return getMatrix(sx, sy, mViewSize.getWidth() / 2f, mViewSize.getHeight() / 2f);
                case CENTER_BOTTOM:
                    return getMatrix(sx, sy, mViewSize.getWidth() / 2f, mViewSize.getHeight());
                case RIGHT_TOP:
                    return getMatrix(sx, sy, mViewSize.getWidth(), 0);
                case RIGHT_CENTER:
                    return getMatrix(sx, sy, mViewSize.getWidth(), mViewSize.getHeight() / 2f);
                case RIGHT_BOTTOM:
                    return getMatrix(sx, sy, mViewSize.getWidth(), mViewSize.getHeight());
                default:
                    throw new IllegalArgumentException("Illegal PivotPoint");
            }
        }

        private Matrix getNoScale() {
            float sx = mVideoSize.getWidth() / (float) mViewSize.getWidth();
            float sy = mVideoSize.getHeight() / (float) mViewSize.getHeight();
            return getMatrix(sx, sy, PivotPoint.LEFT_TOP);
        }

        private Matrix getFitScale(PivotPoint pivotPoint) {
            float sx = (float) mViewSize.getWidth() / mVideoSize.getWidth();
            float sy = (float) mViewSize.getHeight() / mVideoSize.getHeight();
            float minScale = Math.min(sx, sy);
            sx = minScale / sx;
            sy = minScale / sy;
            return getMatrix(sx, sy, pivotPoint);
        }

        private Matrix fitXY() {
            return getMatrix(1, 1, PivotPoint.LEFT_TOP);
        }

        private Matrix fitStart() {
            return getFitScale(PivotPoint.LEFT_TOP);
        }

        private Matrix fitCenter() {
            return getFitScale(PivotPoint.CENTER);
        }

        private Matrix fitEnd() {
            return getFitScale(PivotPoint.RIGHT_BOTTOM);
        }

        private Matrix getOriginalScale(PivotPoint pivotPoint) {
            float sx = mVideoSize.getWidth() / (float) mViewSize.getWidth();
            float sy = mVideoSize.getHeight() / (float) mViewSize.getHeight();
            return getMatrix(sx, sy, pivotPoint);
        }

        private Matrix getCropScale(PivotPoint pivotPoint) {
            float sx = (float) mViewSize.getWidth() / mVideoSize.getWidth();
            float sy = (float) mViewSize.getHeight() / mVideoSize.getHeight();
            float maxScale = Math.max(sx, sy);
            sx = maxScale / sx;
            sy = maxScale / sy;
            return getMatrix(sx, sy, pivotPoint);
        }

        private Matrix startInside() {
            if (mVideoSize.getHeight() <= mViewSize.getWidth() && mVideoSize.getHeight() <= mViewSize.getHeight()) {
                // video is smaller than view size
                return getOriginalScale(PivotPoint.LEFT_TOP);
            } else {
                // either of width or height of the video is larger than view size
                return fitStart();
            }
        }

        private Matrix centerInside() {
            if (mVideoSize.getHeight() <= mViewSize.getWidth() && mVideoSize.getHeight() <= mViewSize.getHeight()) {
                // video is smaller than view size
                return getOriginalScale(PivotPoint.CENTER);
            } else {
                // either of width or height of the video is larger than view size
                return fitCenter();
            }
        }

        private Matrix endInside() {
            if (mVideoSize.getHeight() <= mViewSize.getWidth() && mVideoSize.getHeight() <= mViewSize.getHeight()) {
                // video is smaller than view size
                return getOriginalScale(PivotPoint.RIGHT_BOTTOM);
            } else {
                // either of width or height of the video is larger than view size
                return fitEnd();
            }
        }

        Matrix getScaleMatrix(ScalableType scalableType) {
            switch (scalableType) {
                case NONE:
                    return getNoScale();

                case FIT_XY:
                    return fitXY();
                case FIT_CENTER:
                    return fitCenter();
                case FIT_START:
                    return fitStart();
                case FIT_END:
                    return fitEnd();

                case LEFT_TOP:
                    return getOriginalScale(PivotPoint.LEFT_TOP);
                case LEFT_CENTER:
                    return getOriginalScale(PivotPoint.LEFT_CENTER);
                case LEFT_BOTTOM:
                    return getOriginalScale(PivotPoint.LEFT_BOTTOM);
                case CENTER_TOP:
                    return getOriginalScale(PivotPoint.CENTER_TOP);
                case CENTER:
                    return getOriginalScale(PivotPoint.CENTER);
                case CENTER_BOTTOM:
                    return getOriginalScale(PivotPoint.CENTER_BOTTOM);
                case RIGHT_TOP:
                    return getOriginalScale(PivotPoint.RIGHT_TOP);
                case RIGHT_CENTER:
                    return getOriginalScale(PivotPoint.RIGHT_CENTER);
                case RIGHT_BOTTOM:
                    return getOriginalScale(PivotPoint.RIGHT_BOTTOM);

                case LEFT_TOP_CROP:
                    return getCropScale(PivotPoint.LEFT_TOP);
                case LEFT_CENTER_CROP:
                    return getCropScale(PivotPoint.LEFT_CENTER);
                case LEFT_BOTTOM_CROP:
                    return getCropScale(PivotPoint.LEFT_BOTTOM);
                case CENTER_TOP_CROP:
                    return getCropScale(PivotPoint.CENTER_TOP);
                case CENTER_CROP:
                    return getCropScale(PivotPoint.CENTER);
                case CENTER_BOTTOM_CROP:
                    return getCropScale(PivotPoint.CENTER_BOTTOM);
                case RIGHT_TOP_CROP:
                    return getCropScale(PivotPoint.RIGHT_TOP);
                case RIGHT_CENTER_CROP:
                    return getCropScale(PivotPoint.RIGHT_CENTER);
                case RIGHT_BOTTOM_CROP:
                    return getCropScale(PivotPoint.RIGHT_BOTTOM);

                case START_INSIDE:
                    return startInside();
                case CENTER_INSIDE:
                    return centerInside();
                case END_INSIDE:
                    return endInside();

                default:
                    return null;
            }
        }
    }

    private class Size {

        private int mWidth;

        private int mHeight;

        Size(int width, int height) {
            mWidth = width;
            mHeight = height;
        }

        public int getWidth() {
            return mWidth;
        }

        public int getHeight() {
            return mHeight;
        }
    }
}
