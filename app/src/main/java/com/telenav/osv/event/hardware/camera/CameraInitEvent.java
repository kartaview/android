package com.telenav.osv.event.hardware.camera;

import com.telenav.osv.event.OSVStickyEvent;

/**
 * Created by Kalman on 11/11/2016.
 */

public class CameraInitEvent extends OSVStickyEvent {

    //focus camera ready failed closed
    public static final int TYPE_FAILED = 0;

    public static final int TYPE_READY = 1;

    public static final int TYPE_PERMISSION = 2;

    public final int type;

    public int previewWidth = 0;

    public int previewHeight = 0;

    public CameraInitEvent(int type) {
        this.type = type;
    }

    public CameraInitEvent(int type, int previewWidth, int previewHeight) {
        this.type = type;
        this.previewWidth = previewWidth;
        this.previewHeight = previewHeight;
    }

    @Override
    public Class getStickyClass() {
        return CameraInitEvent.class;
    }
}
