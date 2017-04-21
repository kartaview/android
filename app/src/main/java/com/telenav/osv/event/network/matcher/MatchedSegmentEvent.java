package com.telenav.osv.event.network.matcher;

import java.util.Collection;
import com.telenav.osv.event.OSVStickyEvent;
import com.telenav.osv.item.Polyline;

/**
 * Created by Kalman on 14/11/2016.
 */

public class MatchedSegmentEvent extends OSVStickyEvent {

    public final Collection<Polyline> all;

    public final Polyline polyline;

    public MatchedSegmentEvent(Polyline matched, Collection<Polyline> all) {
        this.polyline = matched;
        this.all = all;
    }

    @Override
    public Class getStickyClass() {
        return MatchedSegmentEvent.class;
    }
}
