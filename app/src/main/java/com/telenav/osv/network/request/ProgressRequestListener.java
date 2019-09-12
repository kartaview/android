package com.telenav.osv.network.request;

/**
 * Generic networking progress listener for {@link ProgressRequestBody}.
 * <p> This will provide a callback for update when any networking processing which has been done.
 */
public interface ProgressRequestListener {
    /**
     * Update callback which will inform any subscribed
     * @param bytesWritten the number of bytes written.
     * @param done {@code} true if the this is the last progress emited, {@code} false otherwise.
     */
    void update(long bytesWritten, long contentLength);
}
