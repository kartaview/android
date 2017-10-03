package com.telenav.osv.item;

import java.util.ArrayList;
import com.skobbler.ngx.map.SKPolyline;

/**
 * Created by Kalman on 11/11/2016.
 */

public class Polyline extends SKPolyline {

    public boolean isLocal = false;

    public int coverage = 0;

    public Polyline(int identifier) {
        super();
        setIdentifier(identifier);
        setNodes(new ArrayList<>());
    }
}