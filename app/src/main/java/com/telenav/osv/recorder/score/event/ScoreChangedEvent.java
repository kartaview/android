package com.telenav.osv.recorder.score.event;

import com.telenav.osv.event.OSVEvent;

/**
 * Model class for score changed event.
 * Created by Kalman on 16/11/2016.
 */
public class ScoreChangedEvent extends OSVEvent {

    /**
     * The value of the score.
     */
    private long score;

    /**
     * The score multiplier which contains the OBD multiplier too.
     */
    private int multiplier;

    public ScoreChangedEvent(long score, int multiplier) {
        this.score = score;
        this.multiplier = multiplier;
    }

    public long getScore() {
        return score;
    }

    public int getMultiplier() {
        return multiplier;
    }
}
