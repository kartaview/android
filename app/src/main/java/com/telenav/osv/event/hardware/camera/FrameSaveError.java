package com.telenav.osv.event.hardware.camera;

/*
 * Event fired when the camera reports an error
 * Created by Kalman on 16/02/2017.
 */

import com.telenav.osv.event.OSVEvent;

public class FrameSaveError extends OSVEvent {

    public final static String TAG = "FrameSaveError";

    public final Exception exception;

    public String message;

    public FrameSaveError(Exception e, String message) {
        this.exception = e;
        this.message = message;
    }
}
