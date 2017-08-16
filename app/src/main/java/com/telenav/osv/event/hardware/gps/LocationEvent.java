package com.telenav.osv.event.hardware.gps;

import android.location.Location;
import com.telenav.osv.event.OSVStickyEvent;

/**
 * Created by Kalman on 14/11/2016.
 */

public class LocationEvent extends OSVStickyEvent {

    public final Location location;

    public boolean shouldCenter;

    public LocationEvent(Location location, boolean shouldCenter) {
        this.location = location;
        this.shouldCenter = shouldCenter;
    }

    @Override
    public Class getStickyClass() {
        return LocationEvent.class;
    }
}
