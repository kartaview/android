package com.telenav.osv.event.hardware.gps;

import com.telenav.osv.event.OSVStickyEvent;

/**
 * Event holding accuracyType
 * Created by Kalman on 07/11/2016.
 */
public class AccuracyEvent extends OSVStickyEvent {

    public final int type;

    public AccuracyEvent(int type) {
        this.type = type;
    }

    @Override
    public Class getStickyClass() {
        return AccuracyEvent.class;
    }
}
