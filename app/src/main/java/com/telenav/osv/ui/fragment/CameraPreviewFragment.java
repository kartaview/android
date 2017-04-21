package com.telenav.osv.ui.fragment;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.animation.ObjectAnimator;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.camera.CameraInitEvent;
import com.telenav.osv.event.ui.PreviewSwitchEvent;
import com.telenav.osv.manager.Recorder;
import com.telenav.osv.utils.DimenUtils;
import com.telenav.osv.utils.Log;

//import com.skobbler.sensorlib.listener.SignDetectedListener;
//import com.skobbler.sensorlib.sign.SignType;

/**
 * Recording ui
 * Created by Kalman on 10/7/2015.
 */
public class CameraPreviewFragment extends Fragment implements TextureView.SurfaceTextureListener {
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

    private View view;

    private OSVActivity activity;

    private Recorder mRecorder;

    private Handler mHandler = new Handler();

    private TextureView mCameraSurfaceView;

    private View mCameraPreview;

    private boolean mIsSmall = false;

    private ImageView mFocusIndicator;

    private FrameLayout mFocusLockedIndicator;

    private SurfaceTexture mSurface;

    private Runnable mPreviewRunnable = new Runnable() {
        @Override
        public void run() {
            boolean set = false;
            while (!set && !mPaused) {
                if (mRecorder != null) {
                    set = mRecorder.setCameraPreviewSurface(mSurface);
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_camera, null);
        activity = (OSVActivity) getActivity();
        // Setup HUDs
        mCameraPreview = view.findViewById(R.id.camera_preview_layout);
        HandlerThread thread = new HandlerThread("PreviewSetter", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mHandler = new Handler(thread.getLooper());

        // Setup shutter button
        mCameraSurfaceView = (TextureView) mCameraPreview.findViewById(R.id.camera_view);
        mCameraSurfaceView.setOnTouchListener(mPreviewTouchListener);
        mCameraSurfaceView.setSurfaceTextureListener(this);

        mFocusIndicator = (ImageView) mCameraPreview.findViewById(R.id.focus_indicator);
        mFocusLockedIndicator = (FrameLayout) mCameraPreview.findViewById(R.id.focus_locked_indicator);

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
    public void onPause() {
        Log.d(TAG, "onPause: removing surface runnable");
        mHandler.removeCallbacksAndMessages(null);
        if (mRecorder != null) {
            mRecorder.setCameraPreviewSurface(null);
        }
        EventBus.unregister(this);
        mPaused = true;
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        // Pause the camera preview
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mPaused = true;
        try {
          mHandler.getLooper().getThread().interrupt();
        } catch (Exception ignored){}
        super.onDestroyView();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onCameraReady(final CameraInitEvent event) {
        Log.d(TAG, "onCameraReady: " + event.type);
        if (event.type == CameraInitEvent.TYPE_READY) {
            if (activity != null) {
                int orientation = activity.getResources().getConfiguration().orientation;
                boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
//                    resizePreview(portrait, CameraManager.instance.previewWidth, CameraManager.instance.previewHeight);
                mHandler.post(mPreviewRunnable);
                resizePreview(portrait, event.previewWidth, event.previewHeight);
            }
        }
    }

    /**
     * @param portrait
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

    private void setFocusViewOnScreen(View v, MotionEvent e, float indicatorSize) {
        if (mCameraSurfaceView == null) {
            return;
        }
        float x = e.getRawX();
        float y = e.getRawY();
        float r = indicatorSize / 2f;
        float left, top;

        if (x - r < 0) {
            left = 0;
        } else if (x + r > mCameraSurfaceView.getWidth()) {
            left = mCameraSurfaceView.getWidth() - 2f * r;
        } else {
            left = x - r;
        }
        if (y - r < 0) {
            top = 0;
        } else if (y + r > mCameraSurfaceView.getHeight()) {
            top = mCameraSurfaceView.getHeight() - 2f * r;
        } else {
            top = y - r;
        }
        v.setVisibility(View.VISIBLE);
        v.setX(left);
        v.setY(top);
    }

    public void setRecorder(Recorder recorder) {
        mRecorder = recorder;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureAvailable: width " + width + ", height = " + height);
        mSurface = surface;
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
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d(TAG, "onSurfaceTextureDestroyed: ");
        if (mRecorder != null) {
            mRecorder.setCameraPreviewSurface(null);
        }
        mIsSmall = false;
        Log.d(TAG, "onSurfaceTextureAvailable: mIsSmall " + mIsSmall);
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
                // A single tap equals to touch-to-focus
                float indicatorSize = getResources().getDimension(R.dimen.tap_to_focus_view);
                setFocusViewOnScreen(mFocusIndicator, e, indicatorSize);
                ObjectAnimator circleFade = ObjectAnimator.ofFloat(mFocusIndicator, View.ALPHA, 1f, 0f);
                circleFade.setDuration(1000);
                circleFade.start();
                if (mIsSmall) {
                    EventBus.post(new PreviewSwitchEvent(true));
                } else {
                    mRecorder.focusOnTouch(e, mCameraSurfaceView.getWidth(), mCameraSurfaceView.getHeight(), indicatorSize / 2, false);
                }

            }
            return super.onSingleTapConfirmed(e);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (mRecorder != null && mRecorder.isRecording()) {
                mRecorder.takePhoto();
//                    double dist = ComputingDistance.distanceBetween(lm.getActualLocation().getLongitude(), lm.getActualLocation().getLatitude(), lm.getPreviousLocation()
//                            .getLongitude(), lm.getPreviousLocation().getLatitude());
//                   mCameraHandlerService.mShutterManager.takeSnapshot(lm.getActualLocation(), lm.getAccuracy(), dist);
            }
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
            if (mRecorder != null) {
                float indicatorSize = getResources().getDimension(R.dimen.tap_to_focus_view);
                setFocusViewOnScreen(mFocusLockedIndicator, e, indicatorSize);
                ObjectAnimator circleFade = ObjectAnimator.ofFloat(mFocusLockedIndicator, View.ALPHA, 1f, 0f);
                circleFade.setDuration(1000);
                circleFade.start();
                mRecorder.focusOnTouch(e, mCameraSurfaceView.getWidth(), mCameraSurfaceView.getHeight(), indicatorSize / 2f, true);
            }

        }
    }
}
