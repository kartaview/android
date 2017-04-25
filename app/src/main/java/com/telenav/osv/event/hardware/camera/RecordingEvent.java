package com.telenav.osv.event.hardware.camera;

import com.telenav.osv.event.OSVStickyEvent;
import com.telenav.osv.item.Sequence;

/**
 * Created by Kalman on 07/11/2016.
 */

public class RecordingEvent extends OSVStickyEvent {

    public final boolean started;

    public Sequence sequence;

    public RecordingEvent(Sequence sequence, boolean started){
        this.started = started;
        this.sequence = sequence;
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof RecordingEvent;
    }

    @Override
    public Class getStickyClass() {
        return RecordingEvent.class;
    }
}
