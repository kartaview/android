package com.telenav.osv.recorder.camera.focus;

import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import com.telenav.osv.utils.Size;

/**
 * Interface that holds all the available focus operations.
 * @author cameliao
 */

public interface Focus {

    /**
     * Focuses camera on a tapped screen point.
     * @param screenPoint the tapped point to focus on.
     * @param previewSize the size of the camera preview.
     */
    void focusOnArea(Point screenPoint, Size previewSize);

    /**
     * Locks the camera focus.
     */
    void lockFocus();

    /**
     * Unlocks the camera focus.
     */
    void unlockFocus();

    /**
     * Sets a background handler for executing all the camera operations.
     * @param backgroundHandler represents a background handler for executing all the camera operations.
     */
    void setBackgroundThread(Handler backgroundHandler);

    //Defined for Camera2 API

    /**
     * Sets the capture session used for focus operations.
     * @param captureSession the capture session.
     */
    default void setCameraCaptureSession(CameraCaptureSession captureSession) {}

    /**
     * Sets the capture request builder which contains all the setting for the camera preview.
     * @param previewRequestBuilder the capture request builder for the camera preview.
     */
    default void setCaptureRequestBuilder(CaptureRequest.Builder previewRequestBuilder) {}

    //Defined for Camera1 API

    /**
     * Sets the camera service used for focus operations.
     * @param camera the instance to the camera 1.
     */
    default void setCamera(Camera camera) {}

}
