package com.telenav.osv.manager.capture;

import java.util.ArrayList;
import java.util.List;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.AutoFocusMoveCallback;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import com.telenav.osv.utils.Log;

/**
 * This class is responsible for focusing the camera.
 * Created by Kalman on 10/7/2015.
 */
abstract class FocusManager implements AutoFocusCallback, AutoFocusMoveCallback {
    public final static String TAG = "FocusManager";

    // Miliseconds during which we assume the focus is good
    private static final int FOCUS_KEEP_TIME = 30000;

    private static boolean mIsFocusing;

    Camera mCamera;

    Camera.Parameters mParameters;

    private boolean mContinuousSupported;

    private long mLastFocusTimestamp = 0;

    private Handler mHandler;

    private boolean notFocused = true;

    private int count;

    private boolean mIsLocked = false;

    private boolean setContinuousAfterFocusOk = false;

    FocusManager() {
        mHandler = new Handler(Looper.getMainLooper());
        mIsFocusing = false;
    }

    void setFocusMode() {
        if (mParameters != null) {
            if (mParameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                mContinuousSupported = true;
            } else {
                if (mParameters.getSupportedFocusModes().contains("auto")) {
                    mParameters.setFocusMode("auto");
                }
            }

            // Do a first focus after 1 second
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    checkFocusManual();
                }
            }, 1000);
        }
    }

    void setAutoFocusMoveCallback(Camera.Parameters params) {

        List<String> focusModes = params.getSupportedFocusModes();
        if (mCamera != null && focusModes != null
                && (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)
                || focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
                || focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))) {
            try {
                mCamera.setAutoFocusMoveCallback(this);
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to set AutoFocusMoveCallback", e);
            }
        }
    }

    /**
     * Trigger the autofocus function of the device
     * @param cb The AF callback
     * @return true if we could start the AF, false otherwise
     */
    private boolean doAutofocus(final AutoFocusCallback cb) {
        if (mCamera != null) {
            try {
                // Trigger af
                mCamera.cancelAutoFocus();

                mHandler.post(new Runnable() {
                    public void run() {
                        try {
                            mCamera.autoFocus(cb);
                        } catch (Exception e) {
                            // Do nothing here
                        }
                    }
                });

            } catch (Exception e) {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }


    void checkFocusManual() {
        if (mContinuousSupported || mIsLocked) {
            return;
        }
        long time = System.currentTimeMillis();
        if ((notFocused || time - mLastFocusTimestamp > FOCUS_KEEP_TIME) && !mIsFocusing) {
            if (doAutofocus(this)) {
                mIsFocusing = true;
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
        try {
            Log.d(TAG, "lockFocus: " + lock);
            if (mParameters == null) {
                if (mCamera != null) {
                    mParameters = mCamera.getParameters();
                }
                if (mParameters == null) {
                    return;
                }
            }
            count = 0;
            mIsLocked = lock;
            if (lock) {
                if (mParameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                setParameters(mParameters);
            } else {
                if (mParameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                setParameters(mParameters);
                if (mContinuousSupported) {
                    setContinuousAfterFocusOk = true;
                }
            }
        } catch (Exception ignored) {}
    }

    protected abstract void setParameters(Camera.Parameters params);

    private void focus(Rect focusRect) {
        cancelAutoFocus();
        List<Camera.Area> focusList = new ArrayList<>();
        Camera.Area focusArea = new Camera.Area(focusRect, 1000);
        focusList.add(focusArea);

        if (mParameters.getMaxNumFocusAreas() > 0) {
            mParameters.setFocusAreas(focusList);
        }
        if (mParameters.getMaxNumMeteringAreas() > 0) {
            mParameters.setMeteringAreas(focusList);
        }
        try {
            setParameters(mParameters);
            doAutofocus(this);
        } catch (Exception e) {
            Log.w(TAG, "Unable to focus: " + Log.getStackTraceString(e));
        }
    }

    private void cancelAutoFocus() {
        if (mCamera != null) {
            try {
                // cancel af
                mCamera.cancelAutoFocus();
            } catch (Exception e) {
                Log.w(TAG, "cancelAutoFocus: " + Log.getStackTraceString(e));
            }
        }
    }

    private void cancelLockExposure() {
        if (mParameters != null) {
            Log.d(TAG, "cancelLockExposure " + mParameters.getAutoExposureLock());
            if (mParameters.getAutoExposureLock()) {
                mParameters.setAutoExposureLock(false);
            }
            setParameters(mParameters);
        }
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
                    doAutofocus(this);
                    count = count + 1;
                    Log.d(TAG, "onAutoFocus: retry autofocus " + count);
                } else {
                    // if the focus is not true after 3 tries cancel the focus and switch to continuous focus
                    lockFocus(false);
                    cancelAutoFocus();
                    doAutofocus(this);
                    Log.w(TAG, "onAutoFocus: locked focusing failed, unlocking");
                }
            } else {
                mParameters.setAutoExposureLock(true);
                setParameters(mParameters);
                Log.d(TAG, "onAutoFocus: AutoExposureLock set");
            }
        } else {
            if (success) {
                if (setContinuousAfterFocusOk) {
                    setContinuousAfterFocusOk = false;
                    Log.d(TAG, "onAutoFocus: focused, setting continuous");
                    if (mContinuousSupported) {
                        mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    }
                    setParameters(mParameters);
                }
            } else {
                if (count < 3) {
                    doAutofocus(this);
                    count = count + 1;
                }
            }
        }
    }

    @Override
    public void onAutoFocusMoving(boolean start, Camera cam) {
        if (mIsFocusing && !start) {
            // We were focusing, but we stopped, notify time of last focus
            mLastFocusTimestamp = System.currentTimeMillis();
        }
        mIsFocusing = start;
    }
}
