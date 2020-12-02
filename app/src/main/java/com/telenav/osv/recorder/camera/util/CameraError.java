package com.telenav.osv.recorder.camera.util;

/**
 * Error class used for storing all the encountered camera errors.
 * This {@code Throwable} error will be send with a specific message from the camera component.
 * @author cameliao
 */

public class CameraError extends Throwable {

    public static final String ERROR_CAMERA_INIT = "Camera initialisation failed.";

    public static final String ERROR_CAMERA_INIT_FAILED_TO_OPEN = "Camera failed to open.";

    public static final String ERROR_CAMERA_PREVIEW_FAILED = "Failed to addChild the preview to camera session.";

    public static final String ERROR_CAMERA_IS_NOT_OPENED = "Camera is not opened.";

    public static final String ERROR_CAMERA_PERMISSION_NEEDED = "Camera permission needed.";

    public static final String ERROR_CAPTURE_FAILED = "Couldn't take a picture";

    public static final String ERROR_CAPTURE_SAVE_FAILED = "Couldn't save the image";

    public CameraError(String message) {
        super(message);
    }
}
