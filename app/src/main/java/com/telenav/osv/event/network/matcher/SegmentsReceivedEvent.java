package com.telenav.osv.event.network.matcher;

import com.telenav.osv.event.OSVStickyEvent;
import com.telenav.osv.item.Polyline;

import java.util.List;

/**
 * event which marks segments received from server
 * Created by Kalman on 01/02/2017.
 */
public class SegmentsReceivedEvent extends OSVStickyEvent {

    public final static String TAG = "SegmentsReceivedEvent";

    public final List<Polyline> polylines;

    public final boolean matcher;

    public final Object syncObject;

    public final float zoom;

    public KVBoundingBox boundingBox;

    public SegmentsReceivedEvent(List<Polyline> polylines, Object syncObject, boolean matcher, KVBoundingBox boundingBox, float zoom) {
        this.zoom = zoom;
        this.polylines = polylines;
        this.matcher = matcher;
        this.syncObject = syncObject;
        this.boundingBox = boundingBox;
    }

    @Override
    public Class getStickyClass() {
        return SegmentsReceivedEvent.class;
    }
}
