package com.telenav.osv.manager;

import java.util.ArrayList;
import java.util.List;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.AutoFocusMoveCallback;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import com.telenav.osv.listener.FocusListener;
import com.telenav.osv.utils.Log;

/**
 * This class is responsible for focusing the camera.
 * Created by Kalman on 10/7/2015.
 */
public class FocusManager implements AutoFocusCallback, AutoFocusMoveCallback {
    public final static String TAG = "FocusManager";

    // Miliseconds during which we assume the focus is good
    private static final int FOCUS_KEEP_TIME = 30000;

    public static boolean mIsFocusing;

    private boolean mContinuousSupported;

    private long mLastFocusTimestamp = 0;

    private Handler mHandler;

    private FocusListener mListener;

    private boolean notFocused = true;

    private Camera.Parameters params;

    private int count;

    private boolean mIsLocked = false;

    private boolean setContinuousAfterFocusOk = false;

    public FocusManager() {
        mHandler = new Handler(Looper.getMainLooper());
        mIsFocusing = false;

        CameraManager.instance.setAutoFocusMoveCallback(this);
        params = CameraManager.instance.getParameters();

        if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            mContinuousSupported = true;
        } else {
            if (params.getSupportedFocusModes().contains("auto")) {
                params.setFocusMode("auto");
            }
        }

        // Do a first focus after 1 second
        mHandler.postDelayed(new Runnable() {
            public void run() {
                checkFocusManual();
            }
        }, 1000);
    }

    public void setListener(FocusListener listener) {
        mListener = listener;
    }

    public void checkFocusManual() {
        if (mContinuousSupported || mIsLocked) {
            return;
        }
        long time = System.currentTimeMillis();
        if ((notFocused || time - mLastFocusTimestamp > FOCUS_KEEP_TIME) && !mIsFocusing) {
            if (CameraManager.instance.doAutofocus(this)) {
                mIsFocusing = true;

                if (mListener != null) {
                    mListener.onFocusStart(false);
                }
            }
        }
    }

    public void focusOnTouch(MotionEvent event, int surfaceWidth, int surfaceHeight, float radius, boolean isLongTap) {
        if (surfaceHeight == 0 || surfaceWidth == 0) {
            return;
        }
        cancelLockExposure();
        lockFocus(isLongTap);
        float top, left, right, bottom;
        boolean isLeftMinus = false, isTopMinus = false, isRightOver = false, isBottomOver = false;
        float x = event.getX();
        float y = event.getY();

        if (x - radius < 0) {
            left = 0;
            isLeftMinus = true;
        } else {
            left = x - radius;
        }
        if (y - radius < 0) {
            top = 0;
            isTopMinus = true;
        } else {
            top = y - radius;
        }
        if (x + radius > surfaceWidth) {
            right = surfaceWidth;
            isRightOver = true;
        } else {
            right = x + radius;
        }
        if (y + radius > surfaceHeight) {
            bottom = surfaceHeight;
            isBottomOver = true;
        } else {
            bottom = y + radius;
        }
        if (isLeftMinus) {
            right = 2f * radius;
        }
        if (isTopMinus) {
            bottom = 2f * radius;
        }
        if (isRightOver) {
            left = surfaceWidth - 2f * radius;
        }
        if (isBottomOver) {
            top = surfaceHeight - 2f * radius;
        }

        Rect cameraFocusRect = new Rect(
                (int) left * 2000 / surfaceWidth - 1000,
                (int) top * 2000 / surfaceHeight - 1000,
                (int) right * 2000 / surfaceWidth - 1000,
                (int) bottom * 2000 / surfaceHeight - 1000);
        focus(cameraFocusRect);
    }

    private void lockFocus(boolean lock) {
        Log.d(TAG, "lockFocus: " + lock);
        count = 0;
//        if (mIsLocked != lock) {
        mIsLocked = lock;
        if (lock) {
            if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            CameraManager.instance.setParameters(params);
        } else {
            if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            CameraManager.instance.setParameters(params);
            if (mContinuousSupported) {
                setContinuousAfterFocusOk = true;
            }
            }
//        }
    }

    private void focus(Rect focusRect) {
        CameraManager.instance.cancelAutoFocus();
        List<Camera.Area> focusList = new ArrayList<Camera.Area>();
        Camera.Area focusArea = new Camera.Area(focusRect, 1000);
        focusList.add(focusArea);

        if (params.getMaxNumFocusAreas() > 0) {
            params.setFocusAreas(focusList);
        }
        if (params.getMaxNumMeteringAreas() > 0) {
            params.setMeteringAreas(focusList);
        }
        try {
            CameraManager.instance.setParameters(params);
            CameraManager.instance.doAutofocus(this);
        } catch (Exception e) {
            Log.w(TAG, "Unable to focus: " + Log.getStackTraceString(e));
        }
    }

    private void cancelLockExposure() {
        Log.d(TAG, "cancelLockExposure " + params.getAutoExposureLock());
        if (params.getAutoExposureLock()) {
            params.setAutoExposureLock(false);
        }
        CameraManager.instance.setParameters(params);
    }

    @Override
    public void onAutoFocus(boolean success, Camera cam) {
        Log.d(TAG, "onAutoFocus: " + success);
        mLastFocusTimestamp = System.currentTimeMillis();
        mIsFocusing = false;
        notFocused = !success;
        if (mIsLocked) {
            if (!success) {
                if (count < 3) {
                    CameraManager.instance.doAutofocus(this);
                    count = count + 1;
                    Log.d(TAG, "onAutoFocus: retry autofocus " + count);
                } else {
                    // if the focus is not true after 3 tries cancel the focus and switch to continuous focus
                    lockFocus(false);
                    CameraManager.instance.cancelAutoFocus();
                    CameraManager.instance.doAutofocus(this);
                    Log.d(TAG, "onAutoFocus: locked focusing failed, unlocking");
                }
            } else {
                params.setAutoExposureLock(true);
                CameraManager.instance.setParameters(params);
                Log.d(TAG, "onAutoFocus: AutoExposureLock set");
            }
        } else {
            if (success) {
                if (setContinuousAfterFocusOk) {
                    setContinuousAfterFocusOk = false;
                    Log.d(TAG, "onAutoFocus: focused, setting continuous");
                    if (mContinuousSupported) {
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    }
                    CameraManager.instance.setParameters(params);
                }
            } else {
                if (count < 3) {
                    CameraManager.instance.doAutofocus(this);
                    count = count + 1;
                }
            }
        }
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
