package com.telenav.osv.command;


import com.telenav.osv.event.OSVEvent;

/**
 * Created by Kalman on 11/11/2016.
 */
public class SignDetectInitCommand extends OSVEvent {
    public boolean initialization;

    public SignDetectInitCommand(boolean initialization) {
        this.initialization = initialization;
    }
}