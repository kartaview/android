package com.telenav.osv.event.network.matcher;

import com.telenav.osv.event.OSVEvent;
import com.telenav.osv.item.Polyline;

/**
 * Created by Kalman on 07/11/2016.
 */

public abstract class MatcherEvent extends OSVEvent {
    public final Polyline polyline;

    public MatcherEvent(Polyline polyline){
        this.polyline = polyline;
    }
}
