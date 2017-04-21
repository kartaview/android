package com.telenav.osv.event.network.upload;

/**
 * Created by Bencze Kalman on 2/12/2017.
 */
public class UploadStartedEvent extends UploadEvent{
    public int remainingSequences;

    public long totalSize;

    public UploadStartedEvent(long totalSize, int remainingSequences) {
        this.totalSize = totalSize;
        this.remainingSequences = remainingSequences;
    }
}
