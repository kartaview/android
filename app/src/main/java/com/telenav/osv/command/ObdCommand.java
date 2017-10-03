package com.telenav.osv.command;

import com.telenav.osv.event.OSVStickyEvent;

/**
 * Created by Kalman on 15/11/2016.
 */

public class ObdCommand extends OSVStickyEvent {

    public final boolean start;

    public ObdCommand(boolean start) {
        this.start = start;
    }

    @Override
    public Class getStickyClass() {
        return ObdCommand.class;
    }
}
