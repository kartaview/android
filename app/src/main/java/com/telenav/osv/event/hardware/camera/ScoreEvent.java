package com.telenav.osv.event.hardware.camera;

import com.telenav.osv.event.OSVEvent;

public class ScoreEvent extends OSVEvent {

    private long score;

    public ScoreEvent(long score) {
        this.score = score;
    }

    public long getScore() {
        return score;
    }
}
