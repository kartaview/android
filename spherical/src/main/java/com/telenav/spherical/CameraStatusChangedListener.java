package com.telenav.spherical;

/**
 * Created by Kalman on 2/6/16.
 */
public interface CameraStatusChangedListener {
    int STATUS_IDLE = 0;

    int STATUS_DISCONNECTED = -1;

    int STATUS_WORKING = 1;

    void onCameraStatusChanged(final int status);
}
