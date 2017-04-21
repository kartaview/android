package com.telenav.osv.event.hardware.gps;

import com.telenav.osv.event.OSVStickyEvent;

/**
 * Created by Kalman on 07/11/2016.
 */

public class AccuracyEvent extends OSVStickyEvent {
    public final float accuracy;

    public AccuracyEvent(float accuracy){
        this.accuracy = accuracy;
    }

    @Override
    public Class getStickyClass() {
        return AccuracyEvent.class;
    }
}
