package com.telenav.osv.item;

import java.util.ArrayList;
import com.skobbler.ngx.map.SKPolyline;

/**
 * Polyline class extension used to denote that if the polyline is used
 */
//ToDo: refactor using converter when rewrite map fragment
public class Polyline extends SKPolyline {

    public boolean isLocal = false;

    public int coverage = 0;

    public Polyline(int identifier) {
        super();
        setIdentifier(identifier);
        setNodes(new ArrayList<>());
    }
}