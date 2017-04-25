package com.telenav.osv.event.network.upload;

/**
 * Created by Bencze Kalman on 2/12/2017.
 */
public class UploadCancelledEvent extends UploadEvent {
    public long total;
    public long remaining;

    public UploadCancelledEvent(long total, long remaining) {
        this.total = total;
        this.remaining = remaining;
    }
}
