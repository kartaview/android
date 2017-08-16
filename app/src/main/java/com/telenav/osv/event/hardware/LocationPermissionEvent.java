package com.telenav.osv.event.hardware;

import com.google.android.gms.common.api.Status;
import com.telenav.osv.event.OSVStickyEvent;

/**
 * Created by Kalman on 14/11/2016.
 */

public class LocationPermissionEvent extends OSVStickyEvent {

    public final Status status;

    public LocationPermissionEvent(Status status) {
        this.status = status;
    }

    @Override
    public Class getStickyClass() {
        return LocationPermissionEvent.class;
    }
}
