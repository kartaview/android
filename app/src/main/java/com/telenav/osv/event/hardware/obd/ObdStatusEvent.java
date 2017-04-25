package com.telenav.osv.event.hardware.obd;

import com.telenav.osv.event.OSVStickyEvent;

/**
 * Created by Kalman on 15/11/2016.
 */

public class ObdStatusEvent extends OSVStickyEvent {

    public static final int TYPE_DISCONNECTED = 0;
    public static final int TYPE_CONNECTED = 1;
    public static final int TYPE_CONNECTING = 2;

    public final int type;

    public ObdStatusEvent(int type){
        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ObdStatusEvent;
    }

    @Override
    public Class getStickyClass() {
        return ObdStatusEvent.class;
    }
}
