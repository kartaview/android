package com.telenav.osv.event.network.matcher;

import com.telenav.osv.event.OSVEvent;
import com.telenav.osv.item.Polyline;

/**
 * Created by Kalman on 07/11/2016.
 */

abstract class MatcherEvent extends OSVEvent {
    private final Polyline polyline;

    MatcherEvent(Polyline polyline) {
        this.polyline = polyline;
    }
}
