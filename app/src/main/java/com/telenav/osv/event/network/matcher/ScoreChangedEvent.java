package com.telenav.osv.event.network.matcher;

import com.telenav.osv.event.OSVEvent;

/**
 * Created by Kalman on 16/11/2016.
 */
public class ScoreChangedEvent extends OSVEvent {
    public final float score;

    public final boolean obd;

    public final int multiplier;

    public ScoreChangedEvent(float score, boolean obdConnected, int multiplier){
        this.score = score;
        this.obd = obdConnected;
        this.multiplier = multiplier;
    }
}
