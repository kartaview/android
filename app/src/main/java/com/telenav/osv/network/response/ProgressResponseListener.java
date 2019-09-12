package com.telenav.osv.network.response;

/**
 * Generic networking progress listener for {@link ProgressResponseBody}.
 * <p> This will provide a callback for update when any networking processing has been done.
 */
public interface ProgressResponseListener {
    /**
     * Update callback which will inform any subscribed
     * @param progressIdentifier the identifier for the progress.
     * @param bytesRead the number of bytes read.
     * @param done {@code} true if the this is the last progress emited, {@code} false otherwise.
     */
    void update(String progressIdentifier, long bytesRead, boolean done);
}
