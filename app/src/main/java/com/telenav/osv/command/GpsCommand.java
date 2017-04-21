package com.telenav.osv.command;

import com.telenav.osv.event.OSVStickyEvent;

/**
 * Created by Kalman on 07/11/2016.
 */

public class GpsCommand extends OSVStickyEvent {

    public final boolean start;

    public GpsCommand(boolean start){
        this.start = start;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof GpsCommand;
    }

    @Override
    public Class getStickyClass() {
        return GpsCommand.class;
    }
}
