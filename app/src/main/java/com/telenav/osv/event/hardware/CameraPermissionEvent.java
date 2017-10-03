package com.telenav.osv.event.hardware;

import com.telenav.osv.event.OSVStickyEvent;

/**
 * Created by Kalman on 14/11/2016.
 */

public class CameraPermissionEvent extends OSVStickyEvent {

    @Override
    public Class getStickyClass() {
        return CameraPermissionEvent.class;
    }
}
