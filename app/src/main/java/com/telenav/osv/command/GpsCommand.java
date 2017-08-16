package com.telenav.osv.command;

import com.telenav.osv.event.OSVStickyEvent;

/**
 * Created by Kalman on 07/11/2016.
 */

public class GpsCommand extends OSVStickyEvent {

    public final boolean start;

    public boolean singlePosition;

    public GpsCommand(boolean start) {
        this.start = start;
    }

    public GpsCommand(boolean start, boolean singlePosition) {
        this.start = start;
        this.singlePosition = singlePosition;
    }

    @Override
    public Class getStickyClass() {
        return GpsCommand.class;
    }
}
