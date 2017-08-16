package com.telenav.osv.event.network.matcher;

import java.util.List;
import com.skobbler.ngx.map.SKBoundingBox;
import com.telenav.osv.event.OSVStickyEvent;
import com.telenav.osv.item.Polyline;

/**
 * event which marks segments received from server
 * Created by Kalman on 01/02/2017.
 */
public class SegmentsReceivedEvent extends OSVStickyEvent {
    public final static String TAG = "SegmentsReceivedEvent";

    public final List<Polyline> all;

    public final boolean matcher;

    public final Object syncObject;

    public final float zoom;

    public SKBoundingBox boundingBox;

    public SegmentsReceivedEvent(List<Polyline> all, Object syncObject, boolean matcher, SKBoundingBox boundingBox, float zoom) {
        this.zoom = zoom;
        this.all = all;
        this.matcher = matcher;
        this.syncObject = syncObject;
        this.boundingBox = boundingBox;
    }

    @Override
    public Class getStickyClass() {
        return SegmentsReceivedEvent.class;
    }
}
