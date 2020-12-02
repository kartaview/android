package com.telenav.osv.event;

/**
 * Created by Kalman on 27/03/2017.
 * This should not be used for new implementation, over time is intended to be replaced and deleted.
 */

@Deprecated
public abstract class OSVStickyEvent extends OSVEvent {

    public abstract Class getStickyClass();
}
