package com.telenav.osv.event.network.matcher;

import com.telenav.osv.common.model.KVLatLng;

public class KVBoundingBox {

    private KVLatLng topLeft;

    private KVLatLng bottomRight;

    public KVBoundingBox(KVLatLng topLeft, KVLatLng bottomRight) {
        this.topLeft = topLeft;
        this.bottomRight = bottomRight;
    }

    public KVLatLng getTopLeft() {
        return topLeft;
    }

    public KVLatLng getBottomRight() {
        return bottomRight;
    }
}
