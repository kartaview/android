package com.telenav.osv.recorder.camera.focus;

import java.util.ArrayList;
import java.util.List;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Handler;
import com.google.common.base.Optional;
import com.telenav.osv.recorder.camera.util.CameraHelper;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Size;

/**
 * The implementation of the {@code Focus} operations using Camera V1.
 */
//TODO: This class contains the legacy code and should be refactored properly.
@SuppressWarnings("deprecation")
public class Camera1FocusManager implements Focus, Camera.AutoFocusCallback {

    private static final String TAG = Camera1FocusManager.class.getSimpleName();

    /**
     * Milliseconds during which we assume the focus is good
     */
    private static final int FOCUS_KEEP_TIME = 30000;

    private static final int STATE_FOCUSING = 0;

    private static final int STATE_FOCUSED = 1;

    private static final int STATE_NOT_FOCUSED = 2;

    private static final int STATE_FOCUS_LOCKED = 3;

    private static final int STATE_FOCUS_UNLOCKED = 4;

    private static final int STATE_TAP_TO_FOCUS = 5;

    private static final int FOCUS_WEIGHT = 1000;

    /**
     * The maximum number for retrying to focus.
     */
    private static final int MAX_FOCUS_RETRIES = 3;

    private Camera.Parameters cameraParameters;

    private Camera camera;

    private Handler backgroundHandler;

    private int focusState;

    private int focusRetryCounter;

    private long mLastFocusTimestamp = 0;

    public Camera1FocusManager(Camera.Parameters cameraParameters, Optional<Camera> camera) {
        if (camera.isPresent()) {
            this.camera = camera.get();
        }
        this.cameraParameters = cameraParameters;
    }

    @Override
    public void focusOnArea(Point screenPoint, Size previewSize) {
        Log.d(TAG, "focusOnArea");
        if (cameraParameters == null || camera == null) {
            return;
        }
        focusState = STATE_TAP_TO_FOCUS;
        cancelAutoFocus();
        List<Camera.Area> focusList = new ArrayList<>();
        Camera.Area focusArea = new Camera.Area(CameraHelper.calculateCameraFocusRect(screenPoint.x, screenPoint.y), FOCUS_WEIGHT);
        focusList.add(focusArea);
        if (cameraParameters.getMaxNumFocusAreas() > 0) {
            cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            try {
                if (cameraParameters.getFocusAreas() != null) {
                    cameraParameters.getFocusAreas().clear();
                }
            } catch (NumberFormatException e) {
                Log.d(TAG, "focusOnArea. Status: error. Message:  " + e.getLocalizedMessage());
            }
            cameraParameters.setFocusAreas(focusList);
        }
        setParameters();
        enableAutoFocus();
    }

    @Override
    public void lockFocus() {
        Log.d(TAG, "lockFocus");
        focusState = STATE_FOCUS_LOCKED;
        enableAutoFocus();
    }

    @Override
    public void unlockFocus() {
        Log.d(TAG, "unlockFocus");
        if (cameraParameters == null || camera == null) {
            return;
        }
        focusState = STATE_FOCUS_UNLOCKED;
        boolean mContinuousSupported = cameraParameters.getSupportedFocusModes()
                .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        if (mContinuousSupported) {
            cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else {
            if (cameraParameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
        }
        setParameters();
    }

    @Override
    public void setBackgroundThread(Handler backgroundHandler) {
        this.backgroundHandler = backgroundHandler;
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        mLastFocusTimestamp = System.currentTimeMillis();
        if (!success) {
            if (focusRetryCounter < MAX_FOCUS_RETRIES) {
                enableAutoFocus();
                focusRetryCounter++;
                Log.d(TAG, "onAutoFocus. Status: retry. Message: Retry auto focus counter - " + focusRetryCounter);
            } else {
                // if the focus is not true after 3 tries cancel the focus and switch to continuous focus
                focusRetryCounter = 0;
                unlockFocus();
                focusState = STATE_NOT_FOCUSED;
            }
        } else {
            if (focusState == STATE_TAP_TO_FOCUS) {
                unlockFocus();
            } else {
                focusState = STATE_FOCUSED;
            }
        }
    }

    @Override
    public void setCamera(Camera camera) {
        try {
            if (camera != null) {
                cameraParameters = camera.getParameters();
            }
            this.camera = camera;
        } catch (RuntimeException e) {
            Log.d(TAG, "setCamera. Status: error. Message: " + e.getMessage());
            this.camera = null;
            this.cameraParameters = null;
        }
    }

    /**
     * Cancel camera auto focus.
     */
    private void cancelAutoFocus() {
        if (camera != null) {
            try {
                // cancel af
                camera.cancelAutoFocus();
            } catch (Exception e) {
                Log.w(TAG, "cancelAutoFocus: " + Log.getStackTraceString(e));
            }
        }
    }

    /**
     * Enables the auto focus mode.
     * @return {@code true} if the auto focus mode is enabled, {@code false} otherwise.
     */
    private boolean enableAutoFocus() {
        if (camera != null) {
            try {
                camera.cancelAutoFocus();
                backgroundHandler.post(() -> {
                    try {
                        camera.autoFocus(this);
                    } catch (Exception e) {
                        Log.d(TAG, "enableAutoFocus. Status: error. Message: Unable to focus. " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                Log.d(TAG, "enableAutoFocus. Status: error. Message: Unable to cancel auto focus. " + e.getMessage());
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Set the camera parameters to the camera device.
     */
    private void setParameters() {
        try {
            if (camera != null && cameraParameters != null) {
                camera.setParameters(cameraParameters);
            }
        } catch (Exception e) {
            Log.d(TAG, "setParameters. Status: error. Message: Unable to set the parameters. " + e.getMessage());
        }
    }

    /**
     * Check if manual focus is enabled otherwise enable the auto focus mode.
     */
    private void checkFocusManual() {
        long time = System.currentTimeMillis();
        if ((time - mLastFocusTimestamp > FOCUS_KEEP_TIME) || focusState == STATE_NOT_FOCUSED) {
            if (enableAutoFocus()) {
                focusState = STATE_FOCUSING;
            }
        }
    }
}