package com.telenav.osv.event.network.upload;

import com.telenav.osv.event.OSVStickyEvent;

/**
 * Created by Kalman on 07/11/2016.
 */

public abstract class UploadEvent extends OSVStickyEvent {
    @Override
    public boolean equals(Object obj) {
        return obj instanceof UploadEvent;
    }

    @Override
    public Class getStickyClass() {
        return UploadEvent.class;
    }
}
