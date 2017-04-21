package com.telenav.osv.event.hardware.gps;

import android.location.Location;
import com.telenav.osv.event.OSVStickyEvent;

/**
 * Created by Kalman on 14/11/2016.
 */

public class LocationEvent extends OSVStickyEvent {

    public final Location location;

    public LocationEvent(Location location){
        this.location = location;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LocationEvent;
    }

    @Override
    public Class getStickyClass() {
        return LocationEvent.class;
    }
}
