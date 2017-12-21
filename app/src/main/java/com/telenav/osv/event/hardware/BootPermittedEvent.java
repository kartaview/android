package com.telenav.osv.event.hardware;

import com.telenav.osv.event.OSVStickyEvent;

/**
 * boot permission event
 * Created by Kalman on 23/05/2017.
 */
public class BootPermittedEvent extends OSVStickyEvent {

    public final static String TAG = "BootPermissionEvent";

    public boolean enabled;

    public BootPermittedEvent(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Class getStickyClass() {
        return BootPermittedEvent.class;
    }
}
