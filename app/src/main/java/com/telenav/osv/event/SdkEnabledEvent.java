package com.telenav.osv.event;

/**
 * Created by Kalman on 20/01/2017.
 */

public class SdkEnabledEvent extends OSVStickyEvent {

    public final static String TAG = "SdkEnabledEvent";

    public final boolean enabled;

    public SdkEnabledEvent(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Class getStickyClass() {
        return SdkEnabledEvent.class;
    }
}
