package com.telenav.osv.event;

/**
 * Event fired when app process finished init sequence
 * Created by Kalman on 20/02/2017.
 */

public class AppReadyEvent extends OSVStickyEvent {

    public static final String TAG = "AppReadyEvent";

    @Override
    public Class getStickyClass() {
        return AppReadyEvent.class;
    }
}
