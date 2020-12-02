package com.telenav.osv.network.response;

public class ProgressEvent {

    final String progressIdentifier;

    final long bytesRead;

    boolean done;

    public ProgressEvent(String progressIdentifier, long bytesRead, boolean done) {
        this.progressIdentifier = progressIdentifier;
        this.bytesRead = bytesRead;
        this.done = done;
    }

    public boolean isDone() {
        return done;
    }

    public String getProgressIdentifier() {
        return progressIdentifier;
    }

    public long getBytesRead() {
        return bytesRead;
    }
}
