package com.telenav.osv.ui.fragment;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.telenav.osv.R;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.command.PhotoCommand;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.camera.CameraInfoEvent;
import com.telenav.osv.event.hardware.camera.CameraInitEvent;
import com.telenav.osv.event.ui.PreviewSwitchEvent;
import com.telenav.osv.manager.Recorder;
import com.telenav.osv.utils.DimenUtils;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Size;

//import com.skobbler.sensorlib.listener.SignDetectedListener;
//import com.skobbler.sensorlib.sign.SignType;

/**
 * Recording ui
 * Created by Kalman on 10/7/2015.
 */
@SuppressLint("ClickableViewAccessibility")
public class CameraPreviewFragment extends FunctionalFragment implements TextureView.SurfaceTextureListener {
    public final static String TAG = "CameraPreviewFragment";

    private GestureDetector mGestureDetector;

    private boolean mPaused = false;

    private View.OnTouchListener mPreviewTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent ev) {
            if (mGestureDetector != null) {
                mGestureDetector.onTouchEvent(ev);
            }
            return true;
        }
    };

    private OSVActivity activity;

    private Recorder mRecorder;

    private View mCameraPreview;

    private boolean mIsSmall = false;

    private ImageView mFocusIndicator;

    private FrameLayout mFocusLockedIndicator;

    private Size mPreviewSize;

    private Size mSurfaceSize = new Size(0, 0);

    private Handler mUIHandler = new Handler(Looper.getMainLooper());

    private TextureView mCameraSurfaceView;

    private FrameLayout mCameraSurfaceHolder;

    private ApplicationPreferences appPrefs;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, null);
        activity = (OSVActivity) getActivity();
        // Setup HUDs
        appPrefs = activity.getApp().getAppPrefs();
        mCameraPreview = view.findViewById(R.id.camera_preview_layout);

        // Setup shutter button
        mCameraSurfaceHolder = mCameraPreview.findViewById(R.id.camera_view);

        mFocusIndicator = mCameraPreview.findViewById(R.id.focus_indicator);
        mFocusLockedIndicator = mCameraPreview.findViewById(R.id.focus_locked_indicator);

        mGestureDetector = new GestureDetector(activity, new GestureListener());
        Animation fadeIn = new AlphaAnimation(0, 0.60f);
        fadeIn.setInterpolator(new DecelerateInterpolator()); //add this
        fadeIn.setDuration(100);

        Animation fadeOut = new AlphaAnimation(0.60f, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator()); //and this
        fadeOut.setStartOffset(100);
        fadeOut.setDuration(300);
        mPaused = false;
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//todo this won't be ok
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            DisplayManager.DisplayListener listener = new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {}

                @Override
                public void onDisplayRemoved(int displayId) {}

                @Override
                public void onDisplayChanged(int displayId) {
                    configureTransform(mCameraSurfaceView, mSurfaceSize.width, mSurfaceSize.height);
                }
            };
            DisplayManager displayManager = (DisplayManager) activity.getSystemService(Context.DISPLAY_SERVICE);
            displayManager.registerDisplayListener(listener, mUIHandler);
        }
        return view;
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mPaused = false;
        EventBus.register(this);
    }

    @Override
    public void onStop() {
        removeCameraPreviewSurface();
        super.onStop();
    }

    @Override
    public void onPause() {
        EventBus.unregister(this);
        mPaused = true;
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        // Pause the camera preview
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mPaused = true;
        super.onDestroyView();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onCameraReady(final CameraInitEvent event) {
        Log.d(TAG, "onCameraReady: " + event.type);
        if (event.type == CameraInitEvent.TYPE_READY) {
            addPreviewSurface();
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onCameraInfoReceived(final CameraInfoEvent event) {
        mPreviewSize = new Size(event.previewWidth, event.previewHeight);
        if (activity != null) {
            int orientation = activity.getResources().getConfiguration().orientation;
            boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
            resizePreview(portrait, event.previewWidth, event.previewHeight);
        }
    }


    private void addPreviewSurface() {
        if (mCameraSurfaceView == null) {
            mCameraSurfaceView = new TextureView(activity);
        }
        mCameraSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mCameraSurfaceView.setOnTouchListener(mPreviewTouchListener);
        mCameraSurfaceView.setSurfaceTextureListener(this);
        mCameraSurfaceHolder.removeAllViews();
        mCameraSurfaceHolder.addView(mCameraSurfaceView);
    }

    private void removeCameraPreviewSurface() {
        if (mCameraSurfaceHolder != null) {
            mCameraSurfaceHolder.removeAllViews();
        }
    }

    /**
     * @param portrait if portrait
     * @param previewWidth scaled to screen size
     * @param previewHeight scaled to screen size
     */
    private void resizePreview(final boolean portrait, final int previewWidth, final int previewHeight) {
        Point point = new Point();
        DimenUtils.getContentSize(activity, portrait, point);
        Log.d(TAG, "resizePreview: contentHeight = " + point.y + " contentWidth = " + point.x);
        float ratio;
        if (!portrait) {
            ratio = (float) point.x / (float) previewHeight;
        } else {
            ratio = (float) point.x / (float) previewHeight;
        }

        RelativeLayout.LayoutParams previewLayoutParams;
        if (portrait) {
            int value = (int) ((float) previewWidth * ratio);
            Log.d(TAG, "resizePreview: surface height = " + value);
            previewLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, value);
            previewLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        } else {
            int value = (int) ((float) previewWidth * ratio);
            Log.d(TAG, "resizePreview: surface width = " + value);
            previewLayoutParams = new RelativeLayout.LayoutParams(value, RelativeLayout.LayoutParams.MATCH_PARENT);
            previewLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        }
        mCameraPreview.setLayoutParams(previewLayoutParams);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void setFocusViewOnScreen(View v, MotionEvent e) {
        if (mCameraSurfaceHolder == null) {
            return;
        }
        float x = e.getX();
        float y = e.getY();
        float r = v.getWidth() / 2;
        float left, top;

        if (x - r < 0) {
            left = 0;
        } else if (x + r > mCameraSurfaceHolder.getWidth()) {
            left = mCameraSurfaceHolder.getWidth() - 2f * r;
        } else {
            left = x - r;
        }
        if (y - r < 0) {
            top = 0;
        } else if (y + r > mCameraSurfaceHolder.getHeight()) {
            top = mCameraSurfaceHolder.getHeight() - 2f * r;
        } else {
            top = y - r;
        }
        v.setVisibility(View.VISIBLE);
        v.setX(left);
        v.setY(top);
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     * @param viewWidth The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void configureTransform(TextureView textureView, int viewWidth, int viewHeight) {
//        Activity activity = getActivity();
        if (null == textureView || null == mPreviewSize) {
            return;
        }
        int rotation = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.height, mPreviewSize.width);
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.height,
                    (float) viewWidth / mPreviewSize.width);
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        textureView.setTransform(matrix);
    }

    @Override
    public void setRecorder(Recorder recorder) {
        mRecorder = recorder;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureAvailable: width " + width + ", height = " + height);
        if (mRecorder != null) {
            mRecorder.setCameraPreviewSurface(surface);
        }
        int orientation = activity.getResources().getConfiguration().orientation;
        boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
        Point point = new Point();
        DimenUtils.getContentSize(activity, portrait, point);

        int w = point.x / 2;
        int h = point.y / 2;
        if (width < w || height < h) {
            if (!mIsSmall) {
                mIsSmall = true;
//                activity.bringPreviewToFront();
            }
        } else {
            mIsSmall = false;
        }
        mSurfaceSize.width = width;
        mSurfaceSize.height = height;
        configureTransform(mCameraSurfaceView, mSurfaceSize.width, mSurfaceSize.height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureSizeChanged: width " + width + ", height = " + height);
        int orientation = activity.getResources().getConfiguration().orientation;
        boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
        Point point = new Point();
        DimenUtils.getContentSize(activity, portrait, point);
        int w = point.x / 2;
        int h = point.y / 2;
        if (width < w || height < h) {
            if (!mIsSmall) {
                mIsSmall = true;
//                activity.bringPreviewToFront();
            }
        } else {
            mIsSmall = false;
        }
        mSurfaceSize.width = width;
        mSurfaceSize.height = height;
        configureTransform(mCameraSurfaceView, width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d(TAG, "onSurfaceTextureDestroyed: ");
        if (mRecorder != null) {
            mRecorder.setCameraPreviewSurface(null);
        }
        mIsSmall = false;
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                if (mPaused || mRecorder == null) return false;
                if (mIsSmall) {
                    EventBus.post(new PreviewSwitchEvent(true));
                } else if (!appPrefs.getBooleanPreference(PreferenceTypes.K_FOCUS_MODE_STATIC)) {
                    // A single tap equals to touch-to-focus
                    setFocusViewOnScreen(mFocusIndicator, e);
                    ObjectAnimator circleFade = ObjectAnimator.ofFloat(mFocusIndicator, View.ALPHA, 1f, 0f);
                    circleFade.setDuration(1000);
                    circleFade.start();
                    int x = (int) (1000f * e.getX() / mCameraSurfaceHolder.getWidth());
                    int y = (int) (1000f * e.getY() / mCameraSurfaceHolder.getHeight());
                    mRecorder.focusOnArea(x, y);
                }
            }
            return super.onSingleTapConfirmed(e);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            EventBus.post(new PhotoCommand());
            return super.onDoubleTap(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (mRecorder != null && !appPrefs.getBooleanPreference(PreferenceTypes.K_FOCUS_MODE_STATIC)) {
                setFocusViewOnScreen(mFocusLockedIndicator, e);
                ObjectAnimator circleFade = ObjectAnimator.ofFloat(mFocusLockedIndicator, View.ALPHA, 1f, 0f);
                circleFade.setDuration(1000);
                circleFade.start();
                int x = (int) (1000f * e.getX() / mCameraSurfaceHolder.getWidth());
                int y = (int) (1000f * e.getY() / mCameraSurfaceHolder.getHeight());
                mRecorder.focusOnArea(x, y);
            }

        }
    }
}
