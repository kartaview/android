package com.telenav.osv.event;/**
 * Created by Kalman on 20/02/2017.
 */

public class AppReadyEvent extends OSVStickyEvent {

    public final static String TAG = "AppReadyEvent";

    @Override
    public Class getStickyClass() {
        return AppReadyEvent.class;
    }
}
