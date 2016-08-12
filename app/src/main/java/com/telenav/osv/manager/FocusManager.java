package com.telenav.osv.manager;

import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.AutoFocusMoveCallback;
import android.os.Handler;
import android.os.Looper;
import com.telenav.osv.listener.FocusListener;

/**
 * This class is responsible for focusing the camera.
 * Created by Kalman on 10/7/2015.
 */
public class FocusManager implements AutoFocusCallback, AutoFocusMoveCallback {
    public final static String TAG = "FocusManager";

    public static boolean mIsFocusing;

    private boolean mContinuous;

    // Miliseconds during which we assume the focus is good
    private int mFocusKeepTimeMs = 30000;

    private long mLastFocusTimestamp = 0;

    private Handler mHandler;

    private FocusListener mListener;

    private boolean notFocused = true;

    public FocusManager() {
        mHandler = new Handler(Looper.getMainLooper());
        mIsFocusing = false;

        CameraManager.instance.setAutoFocusMoveCallback(this);
        Camera.Parameters params = CameraManager.instance.getParameters();

        if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            mContinuous = true;
        } else {
            if (params.getSupportedFocusModes().contains("auto")) {
                params.setFocusMode("auto");
            }
        }

        // Do a first focus after 1 second
        mHandler.postDelayed(new Runnable() {
            public void run() {
                checkFocus();
            }
        }, 1000);
    }

    public void setListener(FocusListener listener) {
        mListener = listener;
    }

    public void checkFocus() {
        if (mContinuous) {
            return;
        }
        long time = System.currentTimeMillis();
        if ((notFocused || time - mLastFocusTimestamp > mFocusKeepTimeMs) && !mIsFocusing) {
            refocus();
        }
    }

    public void refocus() {
        if (mContinuous) {
            return;
        }
        if (CameraManager.instance.doAutofocus(this)) {
            mIsFocusing = true;

            if (mListener != null) {
                mListener.onFocusStart(false);
            }
        }
    }

    @Override
    public void onAutoFocus(boolean success, Camera cam) {
        mLastFocusTimestamp = System.currentTimeMillis();
        mIsFocusing = false;
        notFocused = !success;
        if (mListener != null) {
            mListener.onFocusReturns(false, success);
        }
    }

    @Override
    public void onAutoFocusMoving(boolean start, Camera cam) {
        if (mIsFocusing && !start) {
            // We were focusing, but we stopped, notify time of last focus
            mLastFocusTimestamp = System.currentTimeMillis();
        }

        if (start) {
            if (mListener != null) {
                mListener.onFocusStart(true);
            }
        } else {
            if (mListener != null) {
                mListener.onFocusReturns(true, false);
            }
        }
        mIsFocusing = start;
    }


}
