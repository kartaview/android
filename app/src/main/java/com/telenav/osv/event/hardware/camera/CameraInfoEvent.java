package com.telenav.osv.event.hardware.camera;

import com.telenav.osv.event.OSVStickyEvent;

/**
 * Created by kalmanb on 7/14/17.
 */
public class CameraInfoEvent extends OSVStickyEvent {

    public int previewWidth = 0;

    public int previewHeight = 0;

    public CameraInfoEvent(int previewWidth, int previewHeight) {
        this.previewWidth = previewWidth;
        this.previewHeight = previewHeight;
    }

    @Override
    public Class getStickyClass() {
        return CameraInfoEvent.class;
    }
}
