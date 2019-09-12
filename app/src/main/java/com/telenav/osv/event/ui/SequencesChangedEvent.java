package com.telenav.osv.event.ui;

import com.telenav.osv.event.OSVStickyEvent;

/**
 * Created by Kalman on 21/02/2017.
 */
public class SequencesChangedEvent extends OSVStickyEvent {

    public final static String TAG = "SequencesChangedEvent";

    public final boolean online;

    public boolean diskChange;

    public long diskSize;

    public String deletedSequenceId;

    public SequencesChangedEvent(boolean online) {
        this.online = online;
    }

    public SequencesChangedEvent(boolean online, boolean diskChange, long diskSize) {
        this.online = online;
        this.diskChange = diskChange;
        this.diskSize = diskSize;
    }

    public SequencesChangedEvent(boolean online, String id) {
        this.online = online;
        this.deletedSequenceId = id;
    }

    @Override
    public Class getStickyClass() {
        return SequencesChangedEvent.class;
    }
}
