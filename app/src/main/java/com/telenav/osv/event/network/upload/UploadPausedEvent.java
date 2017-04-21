package com.telenav.osv.event.network.upload;

/**
 * Created by Bencze Kalman on 2/12/2017.
 */
public class UploadPausedEvent extends UploadEvent {

    public boolean paused;

    public UploadPausedEvent(boolean paused) {
        this.paused = paused;
    }
}
