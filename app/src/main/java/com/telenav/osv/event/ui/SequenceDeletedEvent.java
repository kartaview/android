package com.telenav.osv.event.ui;

import com.telenav.osv.event.OSVStickyEvent;

/**
 * Created by Kalman on 21/02/2017.
 */
public class SequenceDeletedEvent extends OSVStickyEvent {

    public final static String TAG = "SequenceDeletedEvent";

    public final boolean online;

    public int sequenceId = 0;

    public SequenceDeletedEvent(boolean online) {
        this.online = online;
    }

    public SequenceDeletedEvent(boolean online, int id) {
        this.online = online;
        this.sequenceId = id;
    }

    @Override
    public Class getStickyClass() {
        return SequenceDeletedEvent.class;
    }
}
