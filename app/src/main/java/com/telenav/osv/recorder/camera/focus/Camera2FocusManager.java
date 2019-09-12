package com.telenav.osv.recorder.camera.focus;

import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.os.Handler;
import com.telenav.osv.recorder.camera.util.CameraHelper;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Size;
import androidx.annotation.NonNull;

/**
 * Focus controller which implements {@link Focus}.
 * Handles all the logic for the focus operations defined in the interface.
 * @author cameliao
 */

public class Camera2FocusManager implements Focus {

    private static final String TAG = Camera2FocusManager.class.getSimpleName();

    /**
     * The camera focus is locked.
     */
    private static final int CAMERA_STATE_FOCUS_LOCKED = 0;

    /**
     * Focused on the tapped area.
     */
    private static final int CAMERA_STATE_TAPPED_TO_FOCUS = 1;

    /**
     * Default camera focus.
     */
    private static final int CAMERA_STATE_AUTO_FOCUS = 2;


    /**
     * The number of regions for the auto-focus (AF) routine.
     * If the number of regions is equal to this value, then the operation for setting a region to auto-focus is not available.
     */
    private static final int AUTO_FOCUS_REGION_UNAVAILABLE = 0;

    private final Object syncObject = new Object();

    /**
     * The capture session used for setting the camera focus.
     * The focus request is set through the {@link CameraCaptureSession#capture(CaptureRequest, CameraCaptureSession.CaptureCallback, Handler)} method.
     */
    private CameraCaptureSession captureSession;

    /**
     * Instance of {@link CameraCharacteristics} containing the properties describing the {@link CameraDevice}.
     */
    private CameraCharacteristics cameraCharacteristics;

    /**
     * A {@code boolean} value, representing if the feature tap to focus is supported by the current Android device.
     */
    private boolean supportsTapToFocus;

    /**
     * This is the rectangle representing the size of the active region of the sensors.
     */
    private Rect focusRect;

    /**
     * The builder for setting the needed configurations for focusing.
     * The properties that must be set are {@link CaptureRequest#CONTROL_AF_MODE} and {@link CaptureRequest#CONTROL_AE_MODE}.
     */
    private CaptureRequest.Builder previewRequestBuilder;

    /**
     * The current state of the camera.
     */
    private int cameraState;

    /**
     * Represents a background handler for receiving all the camera callback.
     */
    private Handler backgroundHandler;

    /**
     * Callback for listening when a capture request is started.
     */
    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            process();
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            process();
        }

        private void process() {
            switch (cameraState) {
                case CAMERA_STATE_FOCUS_LOCKED: {
                    Log.d(TAG, "Camera state: CAMERA_STATE_FOCUS_LOCKED");
                    synchronized (syncObject) {
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);

                        try {
                            if (captureSession != null) {
                                captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, null);
                            }
                        } catch (CameraAccessException | IllegalStateException | IllegalArgumentException e) {
                            Log.e(TAG, String.format("Camera state: CAMERA_STATE_FOCUS_LOCKED - %s", e.getMessage()));
                        }
                    }
                    break;
                }
                case CAMERA_STATE_TAPPED_TO_FOCUS: {
                    Log.d(TAG, "Camera state: CAMERA_STATE_TAPPED_TO_FOCUS");
                    synchronized (syncObject) {
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);
                        try {
                            cameraState = CAMERA_STATE_AUTO_FOCUS;
                            if (captureSession != null) {
                                captureSession.setRepeatingRequest(previewRequestBuilder.build(), captureCallback, null);
                            }
                        } catch (CameraAccessException | IllegalStateException | IllegalArgumentException e) {
                            Log.e(TAG, String.format("Camera state: CAMERA_STATE_TAPPED_TO_FOCUS - %s", e.getMessage()));
                        }
                    }
                    break;
                }
                case CAMERA_STATE_AUTO_FOCUS: {
                    Log.d(TAG, "Camera state: CAMERA_STATE_AUTO_FOCUS");
                    setAutoFocus();
                    try {
                        synchronized (syncObject) {
                            if (captureSession != null) {
                                captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, null);
                            }
                        }
                    } catch (CameraAccessException | IllegalStateException | IllegalArgumentException e) {
                        Log.e(TAG, String.format("Camera state: CAMERA_STATE_AUTO_FOCUS - %s", e.getMessage()));
                    }
                }
            }

        }
    };

    /**
     * Default constructor for the current class.
     * @param cameraCharacteristics instance of {@link CameraCharacteristics} containing the camera properties.
     * @param builder instance of the {@link CaptureRequest.Builder} containing the settings for the camera.
     * @param cameraCaptureSession instance of the {@link CameraCaptureSession} representing the current camera preview session.
     */
    public Camera2FocusManager(CameraCharacteristics cameraCharacteristics, CaptureRequest.Builder builder, CameraCaptureSession cameraCaptureSession) {
        this.cameraCharacteristics = cameraCharacteristics;
        this.previewRequestBuilder = builder;
        this.captureSession = cameraCaptureSession;
        setFocusActiveRegion();
        supportsTapToFocus = CameraHelper.checkTapToFocusAvailability(cameraCharacteristics);
    }

    @Override
    public void focusOnArea(Point screenPoint, Size previewSize) {
        synchronized (syncObject) {
            if (!supportsTapToFocus || captureSession == null) {
                return;
            }
            try {
                configureFocus(screenPoint, previewSize);
                captureSession.stopRepeating();
                cameraState = CAMERA_STATE_TAPPED_TO_FOCUS;
                captureSession.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler);
            } catch (CameraAccessException | IllegalStateException | IllegalArgumentException e) {
                Log.e(TAG, String.format("focusOnArea: %s", e.getMessage()));
            }
        }
    }

    @Override
    public void lockFocus() {
        synchronized (syncObject) {
            if (captureSession == null) {
                return;
            }
            try {
                captureSession.stopRepeating();
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                        CameraMetadata.CONTROL_AF_TRIGGER_START);
                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                        CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
                cameraState = CAMERA_STATE_FOCUS_LOCKED;
                captureSession.capture(previewRequestBuilder.build(), captureCallback,
                        backgroundHandler);
            } catch (CameraAccessException | IllegalStateException | IllegalArgumentException e) {
                Log.e(TAG, String.format("lockFocus: %s", e.getMessage()));
            }
        }
    }

    @Override
    public void unlockFocus() {
        synchronized (syncObject) {
            if (cameraState != CAMERA_STATE_FOCUS_LOCKED || captureSession == null) {
                return;
            }
            try {
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                        CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                captureSession.capture(previewRequestBuilder.build(), null,
                        backgroundHandler);
                cameraState = CAMERA_STATE_AUTO_FOCUS;
                captureSession.setRepeatingRequest(previewRequestBuilder.build(), captureCallback,
                        backgroundHandler);
            } catch (CameraAccessException | IllegalStateException | IllegalArgumentException e) {
                Log.e(TAG, String.format("unlockFocus: %s", e.getMessage()));
            }
        }
    }

    @Override
    public void setCameraCaptureSession(CameraCaptureSession captureSession) {
        synchronized (syncObject) {
            this.captureSession = captureSession;
        }
    }

    @Override
    public void setCaptureRequestBuilder(CaptureRequest.Builder previewRequestBuilder) {
        synchronized (syncObject) {
            this.previewRequestBuilder = previewRequestBuilder;
        }
    }

    @Override
    public void setBackgroundThread(Handler backgroundHandler) {
        this.backgroundHandler = backgroundHandler;
    }

    /**
     * Sets the auto focus ON for the capture session.
     */
    private void setAutoFocus() {
        int[] modes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        if (modes == null || modes.length == 0 || (modes.length == 1 && modes[0] == CameraCharacteristics.CONTROL_AF_MODE_OFF)) {
            //The auto focus mode is not supported
            synchronized (syncObject) {
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
                Log.d(TAG, "Auto focus mode not supported");
            }
        } else {
            synchronized (syncObject) {
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            }
        }
    }

    /**
     * Sets the focus region from the active the region of the sensors.
     */
    private void setFocusActiveRegion() {
        focusRect = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        if (focusRect != null) {
            focusRect = new Rect(0, 0, focusRect.width(), focusRect.height());
        }
    }

    /**
     * Configures the focus for a tapped area.
     * @param screenPoint the tapped location on the screen.
     */
    private void configureFocus(Point screenPoint, Size previewSize) {
        //Calculate and set the focus area to the auto focus and auto exposure regions.
        Rect rect = CameraHelper.calculateCameraFocusRect(screenPoint.x, screenPoint.y, previewSize.getHeight(), previewSize.getWidth(), focusRect);
        if (rect.left < 0) {
            int normalizeFocusValue = -rect.left;
            rect.left = rect.left + normalizeFocusValue;
            rect.right = rect.right + normalizeFocusValue;
        }
        Log.d(TAG, String.format("Focus rectangle: left = %s; top = %s; right = %s; bottom = %s", rect.left, rect.top, rect.right, rect.bottom));
        MeteringRectangle[] focusArea = new MeteringRectangle[]{new MeteringRectangle(rect, MeteringRectangle.METERING_WEIGHT_MAX - 1)};
        Integer autoFocusMaxRegion = cameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
        synchronized (syncObject) {
            if (autoFocusMaxRegion != null && autoFocusMaxRegion > AUTO_FOCUS_REGION_UNAVAILABLE) {
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, focusArea);
                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, focusArea);
            }
            previewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            //Start trigger for the auto exposure and auto focus in order to start the actual transformation of the preview.
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        }
    }
}
