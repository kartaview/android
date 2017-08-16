package com.telenav.osv.event.network.matcher;

import com.telenav.osv.event.OSVEvent;

/**
 * Created by Kalman on 16/11/2016.
 */
public class ScoreChangedEvent extends OSVEvent {
    public final float score;

    public final int multiplier;

    private final boolean obd;

    public ScoreChangedEvent(float score, boolean obdConnected, int multiplier) {
        this.score = score;
        this.obd = obdConnected;
        this.multiplier = multiplier;
    }
}
