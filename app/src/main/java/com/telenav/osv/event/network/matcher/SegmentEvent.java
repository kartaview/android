package com.telenav.osv.event.network.matcher;

import com.telenav.osv.item.Polyline;

/**
 * Created by Kalman on 14/11/2016.
 */

public class SegmentEvent extends MatcherEvent {
    public SegmentEvent(Polyline polyline){
        super(polyline);
    }
}
