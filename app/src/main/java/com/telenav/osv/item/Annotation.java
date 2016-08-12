package com.telenav.osv.item;

import com.skobbler.ngx.map.SKAnnotation;

/**
 * Created by Kalman on 11/23/15.
 */
public class Annotation extends SKAnnotation {
    public final boolean isLocal;

    public Annotation(int i, boolean isLocal) {
        super(i);
        this.isLocal = isLocal;
    }
}
